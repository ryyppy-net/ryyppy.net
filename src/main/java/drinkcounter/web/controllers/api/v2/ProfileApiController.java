/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.web.controllers.api.v2;

import com.csvreader.CsvWriter;
import drinkcounter.DrinkCounterService;
import drinkcounter.UserService;
import drinkcounter.alcoholcalculator.AlcoholCalculator;
import drinkcounter.authentication.CurrentUser;
import drinkcounter.model.Drink;
import drinkcounter.model.User;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 *
 * @author Toni
 */
@RestController
@RequestMapping("API/v2/profile")
public class ProfileApiController {

    private final DrinkCounterService drinkCounterService;
    private final UserService userService;
    private final CurrentUser currentUser;

    public ProfileApiController(DrinkCounterService drinkCounterService, UserService userService, CurrentUser currentUser) {
        this.drinkCounterService = drinkCounterService;
        this.userService = userService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public UserDTO getUser() {
        User user = currentUser.getUser();
        UserDTO userDTO = UserDTO.fromUser(user);
        userDTO.setHistory(SlopeService.getSlopes(user));
        return userDTO;
    }

    @PostMapping
    public void updateUser(
                        @RequestParam("name") String name,
                        @RequestParam("email") String email,
                        @RequestParam("sex") User.Sex sex,
                        @RequestParam("weight") Float weight){
        User user = currentUser.getUser();
        user.setName(name);
        user.setEmail(email);
        user.setSex(sex);
        user.setWeight(weight); 
        userService.updateUser(user);
    }

    @PostMapping("drinks")
    public void drink(
            @RequestParam(value="volume", required=false) Float volume,
            @RequestParam(value="alcohol", required=false) Float alcoholPercentage,
            @RequestParam(value="timestamp", required=false) String timestamp){
        Integer userId = currentUser.getUser().getId();
        double alcoholAmount = AlcoholCalculator.STANDARD_DRINK_ALCOHOL_GRAMS;
        if (volume != null && alcoholPercentage != null) {
            alcoholAmount = AlcoholCalculator.getAlcoholAmount(volume, alcoholPercentage);
        }
        Date time = null;
        if(timestamp != null){
            time = Date.from(Instant.parse(timestamp));
        }
        drinkCounterService.addDrink(userId, time, (float)alcoholAmount);
        
    }

    @GetMapping("drinks")
    public List<DrinkDTO> getDrinks(){
        List<Drink> drinks = currentUser.getUser().getDrinks();
        List<DrinkDTO> drinkDTOs = new ArrayList<DrinkDTO>();
        for (Drink drink : drinks) {
            DrinkDTO drinkDTO = new DrinkDTO();
            drinkDTO.setId(drink.getId());
            drinkDTO.setTimestamp(drink.getTimeStamp().toString());
            drinkDTO.setAmountOfShots(drink.getAmountOfShots());
            drinkDTOs.add(drinkDTO);
        }
        return drinkDTOs;
    }

    @DeleteMapping("drinks/{drinkId}")
    public void deleteDrink(@PathVariable Integer drinkId){
        drinkCounterService.removeDrinkFromUser(currentUser.getUser().getId(), drinkId);
    }

    @GetMapping("drink-history")
    public ResponseEntity<byte[]> getDrinkHistory() throws IOException{
        User user = currentUser.getUser();
        List<Drink> drinks = user.getDrinks();

        Map<String, Integer> drinksPerDay = new LinkedHashMap<String, Integer>();
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Drink d : drinks) {
            double timezoneOffset = 0; // (Double)session.getAttribute(AuthenticationController.TIMEZONEOFFSET);
            ZoneOffset dtz = ZoneOffset.ofTotalSeconds((int)(-timezoneOffset * 60));
            String s = d.getTimeStamp().atZone(dtz).format(format);

            Integer i = 0;
            if (drinksPerDay.containsKey(s))
                i = drinksPerDay.get(s);
            i += 1;
            drinksPerDay.put(s, i);
        }

        String today = LocalDate.now(ZoneId.systemDefault()).format(format);
        if (!drinksPerDay.containsKey(today))
            drinksPerDay.put(today, 0);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "text/plain;charset=utf-8");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CsvWriter csvWriter = new CsvWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8), ',');
        csvWriter.writeRecord(new String[]{"Time", "Drinks"});

        for (Map.Entry<String, Integer> p : drinksPerDay.entrySet()) {
            long millis = LocalDate.parse(p.getKey(), format).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            csvWriter.writeRecord(new String[]{Long.toString(millis), p.getValue().toString()});
        }

        csvWriter.close();
        byte[] bytes = baos.toByteArray();
        return new ResponseEntity<byte[]>(bytes, headers, HttpStatus.OK);
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<String> handleInvalidTimestamp(DateTimeParseException ex) {
        return ResponseEntity.badRequest().body("Invalid timestamp: " + ex.getMessage());
    }
}
