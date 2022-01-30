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
import java.util.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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
            time = new Date(new DateTime(timestamp).getMillis());
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
            drinkDTO.setTimestamp(new DateTime(drink.getTimeStamp()).toString());
            drinkDTO.setAmountOfShots(drink.getAmountOfShots());
            drinkDTOs.add(drinkDTO);
        }
        return drinkDTOs;
    }

    @DeleteMapping
    @RequestMapping(value="drinks/{drinkId}", method=RequestMethod.DELETE)
    public void deleteDrink(@PathVariable Integer drinkId){
        drinkCounterService.removeDrinkFromUser(currentUser.getUser().getId(), drinkId);
    }

    @GetMapping("drink-history")
    public ResponseEntity<byte[]> getDrinkHistory() throws IOException{
        User user = currentUser.getUser();
        List<Drink> drinks = user.getDrinks();

        Map<String, Integer> drinksPerDay = new LinkedHashMap<String, Integer>();
        String format = "YYYY-MM-dd";

        for (Drink d : drinks) {
            DateTime dt = new DateTime(d.getTimeStamp());
            double timezoneOffset = 0; // (Double)session.getAttribute(AuthenticationController.TIMEZONEOFFSET);
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
        CsvWriter csvWriter = new CsvWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8), ',');
        csvWriter.writeRecord(new String[]{"Time", "Drinks"});

        for (Map.Entry<String, Integer> p : drinksPerDay.entrySet()) {
            DateTime dt = new DateTime(p.getKey());
            csvWriter.writeRecord(new String[]{Long.toString(dt.getMillis()), p.getValue().toString()});
        }

        csvWriter.close();
        byte[] bytes = baos.toByteArray();
        return new ResponseEntity<byte[]>(bytes, headers, HttpStatus.OK);
    }
}
