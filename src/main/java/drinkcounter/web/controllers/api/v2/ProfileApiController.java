/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.web.controllers.api.v2;

import com.csvreader.CsvWriter;
import com.google.gson.Gson;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 *
 * @author Toni
 */
@Controller
@RequestMapping("/v2")
public class ProfileApiController {
    
    @Autowired
    private DrinkCounterService drinkCounterService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CurrentUser currentUser;
    
    private Gson gson = new Gson();
    
    @RequestMapping(value="/profile", method=RequestMethod.GET, produces = {"application/json;charset=UTF-8"})
    public @ResponseBody String getUser() {
        User user = currentUser.getUser();
        UserDTO userDTO = UserDTO.fromUser(user);
        userDTO.setHistory(SlopeService.getSlopes(user));
        return gson.toJson(userDTO);
    }
    
    @RequestMapping(value="/profile", method=RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
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
    
    @RequestMapping(value="/profile/drinks", method= RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
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
    
    @RequestMapping(value="/profile/drinks", method=RequestMethod.GET, produces = {"application/json;charset=UTF-8"})
    public @ResponseBody String getDrinks(){
        List<Drink> drinks = currentUser.getUser().getDrinks();
        List<DrinkDTO> drinkDTOs = new ArrayList<DrinkDTO>();
        for (Drink drink : drinks) {
            DrinkDTO drinkDTO = new DrinkDTO();
            drinkDTO.setId(drink.getId());
            drinkDTO.setTimestamp(new DateTime(drink.getTimeStamp()).toString());
            drinkDTO.setAmountOfShots(drink.getAmountOfShots());
            drinkDTOs.add(drinkDTO);
        }
        return gson.toJson(drinkDTOs);
    }
    
    @RequestMapping(value="/profile/drinks/{drinkId}", method=RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void deleteDrink(@PathVariable Integer drinkId){
        drinkCounterService.removeDrinkFromUser(currentUser.getUser().getId(), drinkId);
    }
    
    @RequestMapping("/profile/drink-history")
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
