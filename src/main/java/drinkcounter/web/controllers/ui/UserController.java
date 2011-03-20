package drinkcounter.web.controllers.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import drinkcounter.model.User;
import drinkcounter.DrinkCounterService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author murgo
 */
@Controller
public class UserController {

    @Autowired private DrinkCounterService drinkCounterService;

    @RequestMapping("/addUser")
    public String addUser(
            @RequestParam("name") String name,
            @RequestParam("sex") String sex,
            @RequestParam("weight") float weight){
            User user = new User();
            user.setName(name);
            user.setSex(User.Sex.valueOf(sex));
            user.setWeight(weight);
            drinkCounterService.addUser(user);
            return "redirect:parties";
    }

    @RequestMapping("/addDrink")
    public String addDrink(@RequestParam("id") String userId){
        drinkCounterService.addDrink(userId);
        return "redirect:parties";
    }
}
