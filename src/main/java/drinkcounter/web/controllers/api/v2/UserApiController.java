/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.web.controllers.api.v2;

import com.google.gson.Gson;
import drinkcounter.DrinkCounterService;
import drinkcounter.alcoholcalculator.AlcoholCalculator;
import drinkcounter.authentication.CurrentUser;
import drinkcounter.model.Drink;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 *
 * @author Toni
 */
@Controller
@RequestMapping("/v2")
public class UserApiController {
    
    @Autowired
    private DrinkCounterService drinkCounterService;
    
    @Autowired
    private CurrentUser currentUser;
    
    private Gson gson = new Gson();
    
    @RequestMapping(value="/profile", method=RequestMethod.GET)
    public @ResponseBody String getUser() {
        UserDTO userDTO = UserDTO.fromUser(currentUser.getUser());
        return gson.toJson(userDTO);
    }
    
    @RequestMapping(value="/profile/drinks", method= RequestMethod.POST)
    public void drink(
            @RequestParam(value="volume", required=false) Float volume,
            @RequestParam(value="alcohol", required=false) Float alcoholPercentage){
        Integer userId = currentUser.getUser().getId();
        if (volume != null && alcoholPercentage != null) {
            float alcoholAmount = AlcoholCalculator.getAlcoholAmount(volume, alcoholPercentage);
            drinkCounterService.addDrink(userId, alcoholAmount);
        } else {
            drinkCounterService.addDrink(userId);
        }
    }
    
    @RequestMapping(value="/profile/drinks", method=RequestMethod.GET)
    public @ResponseBody String getDrinks(){
        List<Drink> drinks = currentUser.getUser().getDrinks();
        List<DrinkDTO> drinkDTOs = new ArrayList<DrinkDTO>();
        for (Drink drink : drinks) {
            DrinkDTO drinkDTO = new DrinkDTO();
            drinkDTO.setId(drink.getId());
            drinkDTO.setTimestamp(new DateTime(drink.getTimeStamp()).toString());
            drinkDTOs.add(drinkDTO);
        }
        return gson.toJson(drinkDTOs);
    }
    
    @RequestMapping(value="/profile/drinks", method=RequestMethod.DELETE)
    public void deleteDrink(@RequestParam(value="drinkId") Integer drinkId){
        drinkCounterService.removeDrinkFromUser(currentUser.getUser().getId(), drinkId);
    }
}
