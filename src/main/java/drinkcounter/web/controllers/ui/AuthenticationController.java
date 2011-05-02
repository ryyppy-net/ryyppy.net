package drinkcounter.web.controllers.ui;

import drinkcounter.DrinkCounterService;
import drinkcounter.UserService;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author murgo
 */
@Controller
public class AuthenticationController {
    
    public static final String OPENID = "openId";
    public static final String DISCOVERYINFORMATION = "discoveryInformation";
    public static final String TIMEZONEOFFSET = "timeZoneOffset";

    @Autowired private DrinkCounterService drinkCounterService;

    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);

    @RequestMapping("/timezone/{timezoneOffset}")
    public @ResponseBody String receiveTimezone(HttpSession session,  @PathVariable String timezoneOffset) {
        session.setAttribute(TIMEZONEOFFSET, Double.parseDouble(timezoneOffset));
        return null;
    }
    
    @RequestMapping("/login")
    public ModelAndView login() {
        ModelAndView mav = new ModelAndView("login");
        mav.addObject("totalDrinkCount", drinkCounterService.getTotalDrinkCount());
        return mav;
    }
    
    @RequestMapping("/loginerror")
    public String loginerror() {
        return "loginerror";
    }
}
