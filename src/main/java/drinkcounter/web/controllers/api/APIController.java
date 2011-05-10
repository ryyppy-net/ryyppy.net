package drinkcounter.web.controllers.api;

import com.csvreader.CsvWriter;
import com.google.common.base.Charsets;
import drinkcounter.DrinkCounterService;
import drinkcounter.UserService;
import drinkcounter.authentication.AuthenticationChecks;
import drinkcounter.authentication.NotEnoughRightsException;
import drinkcounter.model.Drink;
import drinkcounter.model.User;
import drinkcounter.util.PartyMarshaller;
import drinkcounter.web.controllers.ui.AuthenticationController;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpSession;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author Toni
 */
@Controller
public class APIController {

    private static final Logger log = LoggerFactory.getLogger(APIController.class);

    @Autowired
    private PartyMarshaller partyMarshaller;

    @Autowired
    private DrinkCounterService drinkCounterService;
    
    @Autowired
    private AuthenticationChecks authenticationChecks;

    @Autowired
    private UserService userService;

    @RequestMapping("/parties/{partyId}")
    public @ResponseBody byte[] printXml(HttpSession session, @PathVariable String partyId) throws IOException{
        int id = Integer.parseInt(partyId);
        authenticationChecks.checkRightsForParty(id);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        partyMarshaller.marshall(id, baos);
        byte[] bytesXml = baos.toByteArray();
        return bytesXml;
    }

    @RequestMapping("/users/{userId}/show-drinks")
    public @ResponseBody byte[] showDrinks(HttpSession session, @PathVariable String userId) throws IOException{
        int id = Integer.parseInt(userId);
        authenticationChecks.checkLowLevelRightsToUser(id);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        partyMarshaller.marshallDrinks(id, baos);
        byte[] bytesXml = baos.toByteArray();
        return bytesXml;
    }

    @RequestMapping("/users/{userId}/add-drink")
    public @ResponseBody String addDrink(HttpSession session, @PathVariable String userId){
        int id = Integer.parseInt(userId);
        authenticationChecks.checkHighLevelRightsToUser(id);
        int drinkId = drinkCounterService.addDrink(id);
        User user = userService.getUser(id);
        return Integer.toString(drinkId);
    }

    @RequestMapping("/users/{userId}/remove-drink/{drinkId}")
    public @ResponseBody String removeDrinkFromUser(HttpSession session, @PathVariable String userId, @PathVariable String drinkId){
        int userIdInt = Integer.parseInt(userId);
        authenticationChecks.checkHighLevelRightsToUser( userIdInt);
        int drinkIdInt = Integer.parseInt(drinkId);
        drinkCounterService.removeDrinkFromUser(userIdInt, drinkIdInt);
        log.info(String.format("Removed drink %d from user %d.", drinkIdInt, userIdInt));
        return "";
    }
    
    @RequestMapping("/users/{userId}")
    public @ResponseBody byte[] userXml(HttpSession session, @PathVariable String userId) throws IOException{
        int id = Integer.parseInt(userId);
        authenticationChecks.checkHighLevelRightsToUser( id);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        partyMarshaller.marshallUser(id, baos);
        byte[] bytesXml = baos.toByteArray();
        return bytesXml;
    }
    
    @RequestMapping("/users/{userId}/drinks")
    public ResponseEntity<byte[]> drinkHistory(HttpSession session, @PathVariable String userId) throws IOException{
        int id = Integer.parseInt(userId);
        authenticationChecks.checkLowLevelRightsToUser(id);

        User user = userService.getUser(id);
        List<Drink> drinks = user.getDrinks();

        Map<String, Integer> drinksPerDay = new LinkedHashMap<String, Integer>();
        String format = "YYYY-MM-dd";

        for (Drink d : drinks) {
            DateTime dt = new DateTime(d.getTimeStamp());
            double timezoneOffset = (Double)session.getAttribute(AuthenticationController.TIMEZONEOFFSET);
            DateTimeZone dtz = DateTimeZone.forOffsetMillis((int)(-timezoneOffset * 60 * 1000));
            dt = dt.toDateTime(dtz);
            String s = dt.toString(format);

            Integer i = 0;
            if (drinksPerDay.containsKey(s))
                i = drinksPerDay.get(s);
            i += 1;
            drinksPerDay.put(s, i);
        }

        String today = new DateTime().toString(format);
        if (!drinksPerDay.containsKey(today))
            drinksPerDay.put(today, 0);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "text/plain;charset=utf-8");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CsvWriter csvWriter = new CsvWriter(new OutputStreamWriter(baos, Charsets.UTF_8), ',');
        csvWriter.writeRecord(new String[]{"Time", "Drinks"});

        for (Entry<String, Integer> p : drinksPerDay.entrySet()) {
            DateTime dt = new DateTime(p.getKey());
            csvWriter.writeRecord(new String[]{Long.toString(dt.getMillis()), p.getValue().toString()});
        }

        csvWriter.close();
        byte[] bytes = baos.toByteArray();
        return new ResponseEntity<byte[]>(bytes, headers, HttpStatus.OK);
    }

    @RequestMapping("/users/{userId}/show-history")
    public ResponseEntity<byte[]> showHistory(HttpSession session, @PathVariable String userId) throws IOException{
        int id = Integer.parseInt(userId);
        authenticationChecks.checkHighLevelRightsToUser(id);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "text/plain;charset=utf-8");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CsvWriter csvWriter = new CsvWriter(new OutputStreamWriter(baos, Charsets.UTF_8), ',');
        csvWriter.writeRecord(new String[]{"Time", "Alcohol"});

        User user = userService.getUser(id);
        List<String[]> history = getSlopes(user, false);

        for (String[] s : history) {
            csvWriter.writeRecord(s);
        }
        csvWriter.close();
        byte[] bytes = baos.toByteArray();
        return new ResponseEntity<byte[]>(bytes, headers, HttpStatus.OK);
    }

    @RequestMapping("/parties/{partyId}/get-history")
    public ResponseEntity<byte[]> getPartyHistory(HttpSession session, @PathVariable String partyId) throws IOException {
        int id = Integer.parseInt(partyId);
        authenticationChecks.checkRightsForParty(id);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CsvWriter csvWriter = new CsvWriter(new OutputStreamWriter(baos, Charsets.UTF_8), ',');
        csvWriter.writeRecord(new String[]{"UserID", "Time", "Alcohol"});

        DateTime now = new DateTime();
        DateTime start = now.minusMinutes(300);
        int intervalMs = 2 * 60 * 1000;
        
        List<User> users = drinkCounterService.listUsersByParty(id);
        
        for (User user : users) {
            List<String[]> history = getSlopes(user, true);
            DateTime time = start;
            for (String[] s : history) {
                csvWriter.writeRecord(s);
                time = time.plusMillis(intervalMs);
            }
        }
        csvWriter.close();
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "text/plain;charset=utf-8");
        
        byte[] bytes = baos.toByteArray();
        return new ResponseEntity<byte[]>(bytes, headers, HttpStatus.OK);
    }

    private List<String[]> getSlopes(User user, boolean getId) {
        int intervalMs = 60 * 1000;
        DateTime now = new DateTime();
        DateTime start = now.minusMinutes(300);

        List<Float> history = user.getPromillesAtInterval(start.toDate(), now.toDate(), intervalMs);
        List<String[]> slopes = new LinkedList<String[]>();

        double lastSlope = Double.MAX_VALUE;
        Long lastX = null;
        Float lastY = null;
        long lastInserted = 0;
        
        Long x = start.getMillis();
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
        slopes.add(getCsvValues(new DateTime().getMillis(), user.getPromilles(), user, getId));
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
    
    @ExceptionHandler()
    public HttpEntity handleExceptions(){
        return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    @RequestMapping("/passphrase/{passphrase}")
    public @ResponseBody String passphrase(@PathVariable String passphrase) throws IOException{
        // TODO korjaa käyttämään userXml-metodia, tämä on lähinnä PoC vasta
        User user = userService.getUserByPassphrase(passphrase.toLowerCase());
        if (user == null)
            throw new NotEnoughRightsException();
        
        // I've had it with this motherfucking XML in this motherfucking Java
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CsvWriter csvWriter = new CsvWriter(new OutputStreamWriter(baos, Charsets.UTF_8), ',');
        csvWriter.writeRecord(new String[]{user.getName(), Double.toString(user.getPromilles())});
        csvWriter.close();
        
        return baos.toString();
    }
}
