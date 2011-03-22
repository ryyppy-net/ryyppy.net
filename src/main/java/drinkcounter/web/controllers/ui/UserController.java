package drinkcounter.web.controllers.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import drinkcounter.model.User;
import drinkcounter.DrinkCounterService;
import drinkcounter.model.Party;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;

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
            @RequestParam("weight") float weight, 
            HttpSession session){
        String openId = (String)session.getAttribute(AuthenticationController.OPENID);
        if (openId == null)
            throw new RuntimeException("gtfo");

        User user = new User();
        user.setName(name);
        user.setSex(User.Sex.valueOf(sex));
        user.setWeight(weight);
        user.setOpenId(openId);
        drinkCounterService.addUser(user);
        return "redirect:user";
    }

    @RequestMapping("/addDrink")
    public String addDrink(@RequestParam("id") String userId){
        drinkCounterService.addDrink(userId);
        return "redirect:parties";
    }
    
    @RequestMapping("/user")
    public ModelAndView userPage(HttpSession session){
        String openId = (String)session.getAttribute(AuthenticationController.OPENID);
        if (openId == null)
            throw new RuntimeException("gtfo");
        
        User user = drinkCounterService.getUserByOpenId(openId);
        if (user == null)
            throw new RuntimeException("gtfo");
        
        ModelAndView mav = new ModelAndView();
        mav.setViewName("user");
        mav.addObject("user", user);
        
        List<Party> parties = drinkCounterService.getPartiesByUserId(user.getId());
        mav.addObject("parties", parties);
        
        return mav;
    }
    
    @RequestMapping("/newuser")
    public ModelAndView newUser(HttpSession session){
        String openId = (String)session.getAttribute(AuthenticationController.OPENID);
        if (openId == null)
            throw new RuntimeException("gtfo");
        
        ModelAndView mav = new ModelAndView();
        mav.setViewName("newuser");
        return mav;
    }
}
