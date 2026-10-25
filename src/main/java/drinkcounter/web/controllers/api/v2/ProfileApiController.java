/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.web.controllers.api.v2;

import drinkcounter.DrinkLog;
import drinkcounter.Promilles;
import drinkcounter.UserAccounts;
import drinkcounter.alcoholcalculator.AlcoholCalculator;
import drinkcounter.authentication.LoggedInUser;
import drinkcounter.model.Drink;
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
    private final Promilles promilles;

    private Clock clock = Clock.systemUTC();

    public ProfileApiController(DrinkLog drinkLog, UserAccounts userAccounts, Promilles promilles) {
        this.drinkLog = drinkLog;
        this.userAccounts = userAccounts;
        this.promilles = promilles;
    }

    @GetMapping
    public UserDTO getUser(@LoggedInUser User user) {
        return OwnDrinksView.profile(promilles.of(user));
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
    public OwnDrinksDTO drink(@LoggedInUser User user,
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
        Drink drink = drinkLog.record(user.getId(), time, (float)alcoholAmount);
        return OwnDrinksDTO.of(drink, promilles.of(user), clock);
    }

    /** The user's drinks, oldest first; {@code latest} keeps only that many of the newest. */
    @GetMapping("drinks")
    public List<DrinkDTO> getDrinks(@LoggedInUser User user,
            @RequestParam(value="latest", required=false) Integer latest){
        List<Drink> drinks = latest == null ? user.getDrinks() : user.latestDrinks(latest);
        return drinks.stream().map(DrinkDTO::fromDrink).toList();
    }

    @PutMapping("drinks/{drinkId}")
    public OwnDrinksDTO changeDrink(@LoggedInUser User user, @PathVariable Integer drinkId,
            @RequestParam("volume") Float volume,
            @RequestParam("alcohol") Float alcoholPercentage){
        Drink drink = drinkLog.correctAlcohol(user.getId(), drinkId,
                AlcoholCalculator.getAlcoholAmount(volume, alcoholPercentage));
        return OwnDrinksDTO.of(drink, promilles.of(user), clock);
    }

    @DeleteMapping("drinks/{drinkId}")
    public OwnDrinksDTO deleteDrink(@LoggedInUser User user, @PathVariable Integer drinkId){
        drinkLog.undo(user.getId(), drinkId);
        return OwnDrinksDTO.of(null, promilles.of(user), clock);
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
