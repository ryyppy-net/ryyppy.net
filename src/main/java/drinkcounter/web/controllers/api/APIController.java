package drinkcounter.web.controllers.api;

import com.csvreader.CsvWriter;
import drinkcounter.PartyRoster;
import drinkcounter.DrinkLog;
import drinkcounter.PassphraseLogin;
import drinkcounter.Promilles;
import drinkcounter.UserAccounts;
import drinkcounter.alcoholcalculator.AlcoholCalculator;
import drinkcounter.alcoholcalculator.BloodAlcohol;
import drinkcounter.authentication.OwnUser;
import drinkcounter.authentication.OwnUserOrPartyMate;
import drinkcounter.authentication.PartyAccess;
import drinkcounter.authentication.PartyMember;
import drinkcounter.authentication.PartyMemberAndOwnUserOrPartyMate;
import drinkcounter.model.Drink;
import drinkcounter.model.User;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.dataformat.xml.XmlMapper;

/**
 *
 * @author Toni
 */
@RestController
@RequestMapping("API")
public class APIController {

    private static final Logger log = LoggerFactory.getLogger(APIController.class);
    /**
     * Gram's per litre
     */
    public static final float ALCOHOL_DENSITY = 789;

    private static final String XML = MediaType.APPLICATION_XML_VALUE + ";charset=UTF-8";
    private static final XmlMapper XML_MAPPER = new XmlMapper();

    private final PartyRoster partyRoster;
    private final DrinkLog drinkLog;
    private final UserAccounts userAccounts;
    private final PassphraseLogin passphraseLogin;
    private final Promilles promilles;
    private final PartyAccess partyAccess;

    private Clock clock = Clock.systemUTC();

    public APIController(PartyRoster partyRoster,
            DrinkLog drinkLog, UserAccounts userAccounts, PassphraseLogin passphraseLogin, Promilles promilles, PartyAccess partyAccess) {
        this.partyRoster = partyRoster;
        this.drinkLog = drinkLog;
        this.userAccounts = userAccounts;
        this.passphraseLogin = passphraseLogin;
        this.promilles = promilles;
        this.partyAccess = partyAccess;
    }

    @PartyMember
    @GetMapping(value = "/parties/{partyId}", produces = XML)
    public String party(@PathVariable int partyId) {
        List<BloodAlcohol> participants = promilles.of(partyRoster.members(partyId));
        Map<Integer, Instant> lastDrinkTimes = drinkLog.lastDrinkTimes(participants.stream().map(participant -> participant.user().getId()).toList());
        return xml(ClassicPartyDTO.fromParty(partyRoster.get(partyId), participants, lastDrinkTimes, clock));
    }

    @OwnUser
    @GetMapping(value = "/users/{userId}/show-drinks", produces = XML)
    public String showDrinks(@PathVariable int userId) {
        return xml(ClassicUserDrinksDTO.fromUser(userAccounts.get(userId), drinkLog.drinksOf(userId)));
    }

    @OwnUser
    @PostMapping("/users/{userId}/add-drink")
    public String addDrink(
    @PathVariable int userId, 
    @RequestParam(value="volume", required=false) Float volume,
    @RequestParam(value="alcohol", required=false) Float alcoholPercentage ){
        if(volume != null && alcoholPercentage != null){
            float alcoholAmount = AlcoholCalculator.getAlcoholAmount(volume, alcoholPercentage);
            return Integer.toString(drinkLog.record(userId, alcoholAmount));
        }
        
        return Integer.toString(drinkLog.record(userId, new Date()));
    }

    /** Adds a drink to a participant through the party; refused with 409 once the party has ended. */
    @PartyMemberAndOwnUserOrPartyMate
    @PostMapping("/parties/{partyId}/users/{userId}/add-drink")
    public String addDrinkInParty(
    @PathVariable int partyId, @PathVariable int userId,
    @RequestParam("volume") Float volume,
    @RequestParam("alcohol") Float alcoholPercentage){
        if (!partyAccess.isMember(partyId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        float alcoholAmount = AlcoholCalculator.getAlcoholAmount(volume, alcoholPercentage);
        return Integer.toString(drinkLog.recordInParty(partyId, userId, null, alcoholAmount).getId());
    }

    @OwnUserOrPartyMate
    @PostMapping("/users/{userId}/edit-drink/{drinkId}")
    public String editDrinkOfUser(@PathVariable int userId, @PathVariable String drinkId,
    @RequestParam("volume") Float volume,
    @RequestParam("alcohol") Float alcoholPercentage){
        drinkLog.correctAlcohol(userId, Integer.parseInt(drinkId),
                AlcoholCalculator.getAlcoholAmount(volume, alcoholPercentage));
        return "";
    }

    @OwnUserOrPartyMate
    @PostMapping("/users/{userId}/remove-drink/{drinkId}")
    public String removeDrinkFromUser(@PathVariable int userId, @PathVariable String drinkId){
        int drinkIdInt = Integer.parseInt(drinkId);
        drinkLog.undo(userId, drinkIdInt);
        log.info(String.format("Removed drink %d from user %d.", drinkIdInt, userId));
        return "";
    }
    
    @OwnUserOrPartyMate
    @GetMapping(value = "/users/{userId}", produces = XML)
    public String user(@PathVariable int userId) {
        Instant lastDrinkAt = drinkLog.lastDrinkTimes(List.of(userId)).get(userId);
        return xml(ClassicUserDTO.fromUser(promilles.of(userAccounts.get(userId)), lastDrinkAt, clock));
    }

    @OwnUser
    @GetMapping("/users/{userId}/drinks")
    public ResponseEntity<byte[]> drinkHistory(HttpSession session, @PathVariable int userId) throws IOException{
        List<Drink> drinks = drinkLog.drinksOf(userId);

        Map<String, Integer> drinksPerDay = new LinkedHashMap<String, Integer>();
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        double timezoneOffset = AuthenticationController.timezoneOffset(session);
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
    @GetMapping("/users/{userId}/show-history")
    public ResponseEntity<byte[]> showHistory(@PathVariable int userId) throws IOException{
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "text/plain;charset=utf-8");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CsvWriter csvWriter = new CsvWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8), ',');
        csvWriter.writeRecord(new String[]{"Time", "Alcohol"});

        User user = userAccounts.get(userId);
        List<String[]> history = getSlopes(user, false);

        for (String[] s : history) {
            csvWriter.writeRecord(s);
        }
        csvWriter.close();
        byte[] bytes = baos.toByteArray();
        return new ResponseEntity<byte[]>(bytes, headers, HttpStatus.OK);
    }

    @PartyMember
    @PostMapping("/parties/{partyId}/add-anonymous-user")
    public String addAnonymousUser(
            @PathVariable int partyId,
            @RequestParam("name") String name,
            @RequestParam("sex") String sex,
            @RequestParam("weight") float weight){
        User user = new User();
        user.setName(name);
        user.setSex(User.Sex.valueOf(sex));
        user.setWeight(weight);
        user.setGuest(true);
        userAccounts.add(user);
        partyRoster.join(partyId, user.getId());
        return user.getId().toString();
    }
    
    @PartyMember
    @PostMapping("/parties/{partyId}/link-user-to-party/{userId}")
    public String linkUserToParty(@PathVariable int partyId,
            @PathVariable int userId){
        partyRoster.join(partyId, userId);
        return "";
    }

    private static String xml(Object dto) {
        return XML_MAPPER.writeValueAsString(dto);
    }

    private List<String[]> getSlopes(User user, boolean getId) {
        int intervalMs = 60 * 1000;
        BloodAlcohol bloodAlcohol = promilles.of(user);
        Instant start = bloodAlcohol.now().minus(Duration.ofMinutes(300));

        List<Float> history = bloodAlcohol.promillesAt(start, bloodAlcohol.now(), Duration.ofMillis(intervalMs));
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
        slopes.add(getCsvValues(bloodAlcohol.now().toEpochMilli(), bloodAlcohol.promilles(), user, getId));
        return slopes;
    }

    private String[] getCsvValues(long x, float y, User user, boolean getId) {
        if (getId)
            return new String[]{Integer.toString(user.getId()), Long.toString(x), Float.toString(y)};
        return new String[]{Long.toString(x), Float.toString(y)};
    }
    
    @GetMapping("/passphrase/{passphrase}")
    public String getInfoWithPassphrase(@PathVariable String passphrase) throws IOException{
        User user = passphraseLogin.findUser(passphrase.toLowerCase());
        if (user == null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        return getUserCsv(user);
    }

    @PostMapping("/passphrase/{passphrase}/add-drink/{time}")
    public String addDrinkWithPassphrase(@PathVariable String passphrase, @PathVariable String time) throws IOException{
        User user = passphraseLogin.findUser(passphrase.toLowerCase());
        if (user == null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        try {
            if (time == null || time.equals("") || time.equals("0"))
                drinkLog.record(user.getId(), new Date());
            else
                drinkLog.record(user.getId(), new Date(Long.parseLong(time)));
        } catch (Exception e) {
            return "-1";
        }

        return getUserCsv(user);
    }

    @PostMapping("/passphrase/{passphrase}/undo-drink")
    public String undoDrink(@PathVariable String passphrase) throws IOException{
        User user = passphraseLogin.findUser(passphrase.toLowerCase());
        if (user == null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        List<Drink> drinks = drinkLog.drinksOf(user.getId());
        if (!drinks.isEmpty())
            drinkLog.undo(user.getId(), drinks.get(drinks.size() - 1).getId());

        return getUserCsv(user);
    }

    private String getUserCsv(User user) throws IOException {
        // I've had it with this motherfucking XML in this motherfucking Java
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CsvWriter csvWriter = new CsvWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8), ',');
        csvWriter.writeRecord(new String[]{user.getName(), Double.toString(promilles.of(user).promilles())});
        csvWriter.close();

        return baos.toString();
    }
}
