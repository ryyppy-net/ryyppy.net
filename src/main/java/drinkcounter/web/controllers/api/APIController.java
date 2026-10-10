package drinkcounter.web.controllers.api;

import com.csvreader.CsvWriter;
import drinkcounter.DrinkCounterService;
import drinkcounter.UserService;
import drinkcounter.alcoholcalculator.AlcoholCalculator;
import drinkcounter.authentication.NotEnoughRightsException;
import drinkcounter.authentication.OwnUser;
import drinkcounter.authentication.OwnUserOrPartyMate;
import drinkcounter.authentication.PartyMember;
import drinkcounter.model.Drink;
import drinkcounter.model.User;
import drinkcounter.util.PartyMarshaller;
import drinkcounter.web.controllers.ui.AuthenticationController;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author Toni
 */
@Controller
@RequestMapping("API")
public class APIController {

    private static final Logger log = LoggerFactory.getLogger(APIController.class);
    /**
     * Gram's per litre
     */
    public static final float ALCOHOL_DENSITY = 789;

    @Autowired
    private PartyMarshaller partyMarshaller;

    @Autowired
    private DrinkCounterService drinkCounterService;
    
    @Autowired
    private UserService userService;

    private Clock clock = Clock.systemUTC();

    @PartyMember
    @RequestMapping("/parties/{partyId}")
    public @ResponseBody byte[] printXml(HttpSession session, @PathVariable int partyId) throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        partyMarshaller.marshall(partyId, baos);
        byte[] bytesXml = baos.toByteArray();
        return bytesXml;
    }

    @OwnUser
    @RequestMapping("/users/{userId}/show-drinks")
    public @ResponseBody byte[] showDrinks(HttpSession session, @PathVariable int userId) throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        partyMarshaller.marshallDrinks(userId, baos);
        byte[] bytesXml = baos.toByteArray();
        return bytesXml;
    }

    @OwnUserOrPartyMate
    @RequestMapping("/users/{userId}/add-drink")
    public @ResponseBody String addDrink(HttpSession session, 
    @PathVariable int userId, 
    @RequestParam(value="volume", required=false) Float volume,
    @RequestParam(value="alcohol", required=false) Float alcoholPercentage ){
        if(volume != null && alcoholPercentage != null){
            float alcoholAmount = AlcoholCalculator.getAlcoholAmount(volume, alcoholPercentage);
            return Integer.toString(drinkCounterService.addDrink(userId, alcoholAmount));
        }
        
        return Integer.toString(drinkCounterService.addDrink(userId));
    }

    @OwnUserOrPartyMate
    @RequestMapping("/users/{userId}/edit-drink/{drinkId}")
    public @ResponseBody String editDrinkOfUser(@PathVariable int userId, @PathVariable String drinkId,
    @RequestParam("volume") Float volume,
    @RequestParam("alcohol") Float alcoholPercentage){
        drinkCounterService.changeDrinkAlcohol(userId, Integer.parseInt(drinkId),
                AlcoholCalculator.getAlcoholAmount(volume, alcoholPercentage));
        return "";
    }

    @OwnUserOrPartyMate
    @RequestMapping("/users/{userId}/remove-drink/{drinkId}")
    public @ResponseBody String removeDrinkFromUser(HttpSession session, @PathVariable int userId, @PathVariable String drinkId){
        int drinkIdInt = Integer.parseInt(drinkId);
        drinkCounterService.removeDrinkFromUser(userId, drinkIdInt);
        log.info(String.format("Removed drink %d from user %d.", drinkIdInt, userId));
        return "";
    }
    
    @OwnUserOrPartyMate
    @RequestMapping("/users/{userId}")
    public @ResponseBody byte[] userXml(HttpSession session, @PathVariable int userId) throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        partyMarshaller.marshallUser(userId, baos);
        byte[] bytesXml = baos.toByteArray();
        return bytesXml;
    }
    
    @OwnUser
    @RequestMapping("/users/{userId}/drinks")
    public ResponseEntity<byte[]> drinkHistory(HttpSession session, @PathVariable int userId) throws IOException{
        User user = userService.getUser(userId);
        List<Drink> drinks = user.getDrinks();

        Map<String, Integer> drinksPerDay = new LinkedHashMap<String, Integer>();
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        double timezoneOffset = (Double)session.getAttribute(AuthenticationController.TIMEZONEOFFSET);
        ZoneOffset dtz = ZoneOffset.ofTotalSeconds((int)(-timezoneOffset * 60));

        for (Drink d : drinks) {
            String s = d.getTimeStamp().atZone(dtz).format(format);

            Integer i = 0;
            if (drinksPerDay.containsKey(s))
                i = drinksPerDay.get(s);
            i += 1;
            drinksPerDay.put(s, i);
        }

        String today = LocalDate.now(clock.withZone(dtz)).format(format);
        if (!drinksPerDay.containsKey(today))
            drinksPerDay.put(today, 0);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "text/plain;charset=utf-8");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CsvWriter csvWriter = new CsvWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8), ',');
        csvWriter.writeRecord(new String[]{"Time", "Drinks"});

        for (Entry<String, Integer> p : drinksPerDay.entrySet()) {
            long millis = LocalDate.parse(p.getKey(), format).atStartOfDay(dtz).toInstant().toEpochMilli();
            csvWriter.writeRecord(new String[]{Long.toString(millis), p.getValue().toString()});
        }

        csvWriter.close();
        byte[] bytes = baos.toByteArray();
        return new ResponseEntity<byte[]>(bytes, headers, HttpStatus.OK);
    }

    @OwnUserOrPartyMate
    @RequestMapping("/users/{userId}/show-history")
    public ResponseEntity<byte[]> showHistory(HttpSession session, @PathVariable int userId) throws IOException{
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "text/plain;charset=utf-8");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CsvWriter csvWriter = new CsvWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8), ',');
        csvWriter.writeRecord(new String[]{"Time", "Alcohol"});

        User user = userService.getUser(userId);
        List<String[]> history = getSlopes(user, false);

        for (String[] s : history) {
            csvWriter.writeRecord(s);
        }
        csvWriter.close();
        byte[] bytes = baos.toByteArray();
        return new ResponseEntity<byte[]>(bytes, headers, HttpStatus.OK);
    }

    @PartyMember
    @RequestMapping("/parties/{partyId}/add-anonymous-user")
    public @ResponseBody String addAnonymousUser(HttpSession session,
            @PathVariable int partyId,
            @RequestParam("name") String name,
            @RequestParam("sex") String sex,
            @RequestParam("weight") float weight){
        User user = new User();
        user.setName(name);
        user.setSex(User.Sex.valueOf(sex));
        user.setWeight(weight);
        user.setGuest(true);
        userService.addUser(user);
        drinkCounterService.linkUserToParty(user.getId(), partyId);
        return user.getId().toString();
    }
    
    @PartyMember
    @RequestMapping("/parties/{partyId}/link-user-to-party/{userId}")
    public @ResponseBody String linkUserToParty(HttpSession session, @PathVariable int partyId,
            @PathVariable int userId){
        drinkCounterService.linkUserToParty(userId, partyId);
        return "";
    }

    private List<String[]> getSlopes(User user, boolean getId) {
        int intervalMs = 60 * 1000;
        Instant now = Instant.now();
        Instant start = now.minus(Duration.ofMinutes(300));

        List<Float> history = user.getPromillesAtInterval(Date.from(start), Date.from(now), intervalMs);
        List<String[]> slopes = new LinkedList<String[]>();

        double lastSlope = Double.MAX_VALUE;
        Long lastX = null;
        Float lastY = null;
        long lastInserted = 0;

        Long x = start.toEpochMilli();
        for (Float y : history) {
            double slope = y / (x / 31536000000L);
            if (Math.abs(slope - lastSlope) >= 0.000000001) {
                if (lastX != null && lastY != null && lastInserted != lastX) {
                    slopes.add(getCsvValues(lastX, lastY, user, getId));
                }
                slopes.add(getCsvValues(x, y, user, getId));
                lastInserted = x;
            }
            lastSlope = slope;
            lastX = x;
            lastY = y;
            x += intervalMs;
        }
        slopes.add(getCsvValues(Instant.now().toEpochMilli(), user.getPromilles(), user, getId));
        return slopes;
    }

    private String[] getCsvValues(long x, float y, User user, boolean getId) {
        if (getId)
            return new String[]{Integer.toString(user.getId()), Long.toString(x), Float.toString(y)};
        return new String[]{Long.toString(x), Float.toString(y)};
    }
    
    @ExceptionHandler(NotEnoughRightsException.class)
    public HttpEntity handleForbidden(){
        return new ResponseEntity(HttpStatus.FORBIDDEN);
    }

    @RequestMapping("/passphrase/{passphrase}")
    public @ResponseBody String getInfoWithPassphrase(@PathVariable String passphrase) throws IOException{
        User user = userService.getUserByPassphrase(passphrase.toLowerCase());
        if (user == null)
            throw new NotEnoughRightsException();

        return getUserCsv(user);
    }

    @RequestMapping("/passphrase/{passphrase}/add-drink/{time}")
    public @ResponseBody String addDrinkWithPassphrase(@PathVariable String passphrase, @PathVariable String time) throws IOException{
        User user = userService.getUserByPassphrase(passphrase.toLowerCase());
        if (user == null)
            throw new NotEnoughRightsException();

        try {
            if (time == null || time.equals("") || time.equals("0"))
                drinkCounterService.addDrink(user.getId());
            else
                drinkCounterService.addDrink(user.getId(), new Date(Long.parseLong(time)));
        } catch (Exception e) {
            return "-1";
        }

        return getUserCsv(user);
    }

    @RequestMapping("/passphrase/{passphrase}/undo-drink")
    public @ResponseBody String undoDrink(@PathVariable String passphrase) throws IOException{
        User user = userService.getUserByPassphrase(passphrase.toLowerCase());
        if (user == null)
            throw new NotEnoughRightsException();

        int count = user.getDrinks().size();
        if (count > 0)
            drinkCounterService.removeDrinkFromUser(user.getId(), user.getDrinks().get(count - 1).getId()); // TODO: optimize (if needed, Toni mitenköhä nuo laiskat listat toimii)

        return getUserCsv(user);
    }

    private String getUserCsv(User user) throws IOException {
        // I've had it with this motherfucking XML in this motherfucking Java
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CsvWriter csvWriter = new CsvWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8), ',');
        csvWriter.writeRecord(new String[]{user.getName(), Double.toString(user.getPromilles())});
        csvWriter.close();

        return baos.toString();
    }
}
