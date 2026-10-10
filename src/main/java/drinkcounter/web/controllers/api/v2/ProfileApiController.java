/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.web.controllers.api.v2;

import drinkcounter.DrinkLog;
import drinkcounter.UserAccounts;
import drinkcounter.alcoholcalculator.AlcoholCalculator;
import drinkcounter.authentication.LoggedInUser;
import drinkcounter.authentication.LoggedInUserId;
import drinkcounter.model.User;
import drinkcounter.util.DrinksPerDayCsv;
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

    private final DrinkLog drinkLog;
    private final UserAccounts userAccounts;

    private Clock clock = Clock.systemUTC();

    public ProfileApiController(DrinkLog drinkLog, UserAccounts userAccounts) {
        this.drinkLog = drinkLog;
        this.userAccounts = userAccounts;
    }

    @GetMapping
    public UserDTO getUser(@LoggedInUser User user) {
        UserDTO userDTO = UserDTO.fromUser(user);
        userDTO.setHistory(PromilleHistory.forUser(user));
        return userDTO;
    }

    @PostMapping
    public void updateUser(@LoggedInUser User user,
                        @RequestParam("name") String name,
                        @RequestParam("email") String email,
                        @RequestParam("sex") User.Sex sex,
                        @RequestParam("weight") Float weight){
        user.setName(name);
        user.setEmail(email);
        user.setSex(sex);
        user.setWeight(weight); 
        userAccounts.update(user);
    }

    @PostMapping("drinks")
    public DrinkDTO drink(@LoggedInUserId int userId,
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
        return DrinkDTO.fromDrink(drinkLog.record(userId, time, (float)alcoholAmount));
    }

    @GetMapping("drinks")
    public List<DrinkDTO> getDrinks(@LoggedInUser User user){
        return user.getDrinks().stream().map(DrinkDTO::fromDrink).toList();
    }

    @PutMapping("drinks/{drinkId}")
    public void changeDrink(@LoggedInUserId int userId, @PathVariable Integer drinkId,
            @RequestParam("volume") Float volume,
            @RequestParam("alcohol") Float alcoholPercentage){
        drinkLog.correctAlcohol(userId, drinkId,
                AlcoholCalculator.getAlcoholAmount(volume, alcoholPercentage));
    }

    @DeleteMapping("drinks/{drinkId}")
    public void deleteDrink(@LoggedInUserId int userId, @PathVariable Integer drinkId){
        drinkLog.undo(userId, drinkId);
    }

    @GetMapping("drink-history")
    public ResponseEntity<byte[]> getDrinkHistory(@LoggedInUser User user) throws IOException{
        String csv = DrinksPerDayCsv.build(user.getDrinks(), clock);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "text/plain;charset=utf-8");
        return new ResponseEntity<byte[]>(csv.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<String> handleInvalidTimestamp(DateTimeParseException ex) {
        return ResponseEntity.badRequest().body("Invalid timestamp: " + ex.getMessage());
    }
}
