/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.web.controllers.api.v2;

import drinkcounter.DrinkCounterService;
import drinkcounter.UserService;
import drinkcounter.alcoholcalculator.AlcoholCalculator;
import drinkcounter.authentication.DrinkcounterUserDetails;
import drinkcounter.authentication.LoggedInUser;
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

    private Clock clock = Clock.systemUTC();

    public ProfileApiController(DrinkCounterService drinkCounterService, UserService userService) {
        this.drinkCounterService = drinkCounterService;
        this.userService = userService;
    }

    @GetMapping
    public UserDTO getUser(@LoggedInUser DrinkcounterUserDetails me) {
        User user = userService.getUser(me.getUserId());
        UserDTO userDTO = UserDTO.fromUser(user);
        userDTO.setHistory(SlopeService.getSlopes(user));
        return userDTO;
    }

    @PostMapping
    public void updateUser(@LoggedInUser DrinkcounterUserDetails me,
                        @RequestParam("name") String name,
                        @RequestParam("email") String email,
                        @RequestParam("sex") User.Sex sex,
                        @RequestParam("weight") Float weight){
        User user = userService.getUser(me.getUserId());
        user.setName(name);
        user.setEmail(email);
        user.setSex(sex);
        user.setWeight(weight); 
        userService.updateUser(user);
    }

    @PostMapping("drinks")
    public DrinkDTO drink(@LoggedInUser DrinkcounterUserDetails me,
            @RequestParam(value="volume", required=false) Float volume,
            @RequestParam(value="alcohol", required=false) Float alcoholPercentage,
            @RequestParam(value="timestamp", required=false) String timestamp){
        double alcoholAmount = AlcoholCalculator.STANDARD_DRINK_ALCOHOL_GRAMS;
        if (volume != null && alcoholPercentage != null) {
            alcoholAmount = AlcoholCalculator.getAlcoholAmount(volume, alcoholPercentage);
        }
        Date time = null;
        if(timestamp != null){
            time = Date.from(Instant.parse(timestamp));
        }
        return DrinkDTO.fromDrink(drinkCounterService.addDrink(me.getUserId(), time, (float)alcoholAmount));
    }

    @GetMapping("drinks")
    public List<DrinkDTO> getDrinks(@LoggedInUser DrinkcounterUserDetails me){
        return userService.getUser(me.getUserId()).getDrinks().stream().map(DrinkDTO::fromDrink).toList();
    }

    @PutMapping("drinks/{drinkId}")
    public void changeDrink(@LoggedInUser DrinkcounterUserDetails me, @PathVariable Integer drinkId,
            @RequestParam("volume") Float volume,
            @RequestParam("alcohol") Float alcoholPercentage){
        drinkCounterService.changeDrinkAlcohol(me.getUserId(), drinkId,
                AlcoholCalculator.getAlcoholAmount(volume, alcoholPercentage));
    }

    @DeleteMapping("drinks/{drinkId}")
    public void deleteDrink(@LoggedInUser DrinkcounterUserDetails me, @PathVariable Integer drinkId){
        drinkCounterService.removeDrinkFromUser(me.getUserId(), drinkId);
    }

    @GetMapping("drink-history")
    public ResponseEntity<byte[]> getDrinkHistory(@LoggedInUser DrinkcounterUserDetails me) throws IOException{
        User user = userService.getUser(me.getUserId());
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
