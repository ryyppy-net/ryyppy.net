package drinkcounter.web.controllers.ui;

import drinkcounter.DrinkLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpSession;

import static drinkcounter.web.controllers.DefaultController.REDIRECT_TO_FRONTPAGE;

/**
 *
 * @author murgo
 */
@Controller
@RequestMapping("ui")
public class AuthenticationController {
    
    public static final String TIMEZONEOFFSET = "timeZoneOffset";

    @Autowired private DrinkLog drinkLog;

    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);

    /** The client's offset in the JS convention; 0 until the login page has reported it. */
    public static double timezoneOffset(HttpSession session) {
        Double offset = (Double) session.getAttribute(TIMEZONEOFFSET);
        return offset == null ? 0 : offset;
    }

    @RequestMapping("/timezone/{timezoneOffset}")
    public @ResponseBody String receiveTimezone(HttpSession session,  @PathVariable String timezoneOffset) {
        session.setAttribute(TIMEZONEOFFSET, Double.parseDouble(timezoneOffset));
        return null;
    }
    
    @RequestMapping("/login")
    public ModelAndView login(@CurrentSecurityContext SecurityContext context) {
        // Redirect already logged in users to frontpage
        if(!(context.getAuthentication() instanceof AnonymousAuthenticationToken)) {
            log.info("Heh");
            return new ModelAndView(REDIRECT_TO_FRONTPAGE);
        }

        ModelAndView mav = new ModelAndView("login");
        mav.addObject("totalDrinkCount", drinkLog.total());
        return mav;
    }
}
