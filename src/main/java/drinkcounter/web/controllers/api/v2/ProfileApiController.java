/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.web.controllers.api.v2;

import drinkcounter.DrinkCounterService;
import drinkcounter.UserService;
import drinkcounter.alcoholcalculator.AlcoholCalculator;
import drinkcounter.authentication.CurrentUser;
import drinkcounter.model.Drink;
import drinkcounter.model.User;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
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

    private Clock clock = Clock.systemUTC();

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
        String csv = DrinkHistoryService.buildCsv(user.getDrinks(), clock);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "text/plain;charset=utf-8");
        return new ResponseEntity<byte[]>(csv.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<String> handleInvalidTimestamp(DateTimeParseException ex) {
        return ResponseEntity.badRequest().body("Invalid timestamp: " + ex.getMessage());
    }
}
