package drinkcounter.web.controllers.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import drinkcounter.model.User;
import drinkcounter.DrinkCounterService;
import drinkcounter.UserService;
import drinkcounter.authentication.AuthenticationChecks;
import drinkcounter.authentication.CurrentUser;
import drinkcounter.authentication.FacebookAuthenticationToken;
import drinkcounter.authentication.NotLoggedInException;
import drinkcounter.model.Party;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.apache.commons.beanutils.BeanComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.openid.OpenIDAuthenticationToken;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author murgo
 */
@Controller
public class UserController {
    public static final String OPENID_CREDENTIAL_KEY = "USER_OPENID_CREDENTIAL";
    
    
    @Autowired private DrinkCounterService drinkCounterService;
    @Autowired private UserService userService;

    @Autowired private AuthenticationChecks authenticationChecks;
    @Autowired private UserDetailsService userDetailsService;
    @Autowired private CurrentUser currentUser;
    
    @RequestMapping("/newuser")
    public String newUser(HttpSession session){
        if(session.getAttribute(AuthenticationController.OPENID) == null){
            return "redirect:/";
        }
        return "newuser";
    }
    
    @RequestMapping("/addUser")
    public String addUser(
            @RequestParam("name") String name,
            @RequestParam("sex") String sex,
            @RequestParam("weight") float weight, 
            @RequestParam("email") String email, 
            HttpSession session){
                
        if (session.getAttribute(AuthenticationController.TIMEZONEOFFSET) == null){
            session.setAttribute(AuthenticationController.TIMEZONEOFFSET, new Double(0));
        }

        if (session.getAttribute(AuthenticationController.OPENID) == null) {
            throw new NotLoggedInException();
        }
        
        if (name == null || name.length() == 0 || weight < 1 || !userService.emailIsCorrect(email) || userService.getUserByEmail(email) != null) {
            throw new IllegalArgumentException();
        }
        
        
        
        User user = new User();
        user.setName(name);
        user.setSex(User.Sex.valueOf(sex));
        user.setWeight(weight);
        user.setEmail(email);
        
        Authentication authToken = (Authentication) session.getAttribute(AuthenticationController.OPENID);
        if(authToken instanceof FacebookAuthenticationToken){
            FacebookAuthenticationToken facebookToken = (FacebookAuthenticationToken) authToken;
            user.setOpenId(facebookToken.getProfileId());
            user.setAuthMethod(User.AuthMethod.FACEBOOK);
        }else if(authToken instanceof OpenIDAuthenticationToken){
            OpenIDAuthenticationToken openIdToken = (OpenIDAuthenticationToken) authToken;
            user.setOpenId(openIdToken.getIdentityUrl());
            user.setAuthMethod(User.AuthMethod.OPENID);
        }
        
        userService.addUser(user);
        authenticate(user);
        session.removeAttribute(AuthenticationController.OPENID);
        return "redirect:user";
    }
    
    private void authenticate(User user){
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getOpenId());
        Authentication authentication = null;
        switch(user.getAuthMethod()){
            case OPENID:
                authentication = new OpenIDAuthenticationToken(userDetails, 
                    userDetails.getAuthorities(), 
                    user.getOpenId(),
                    Collections.EMPTY_LIST);
                break;
            case FACEBOOK:
                authentication = new FacebookAuthenticationToken(userDetails);
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @RequestMapping("/modifyUser")
    public String modifyUser(
            @RequestParam("userId") String userId,
            @RequestParam("name") String name,
            @RequestParam("sex") String sex,
            @RequestParam("weight") float weight, 
            @RequestParam("email") String email, 
            HttpSession session){
                
        int uid = Integer.parseInt(userId);
        authenticationChecks.checkLowLevelRightsToUser( uid);
        
        User user = userService.getUser(uid);

        if (!user.getEmail().equalsIgnoreCase(email) && (!userService.emailIsCorrect(email) || userService.getUserByEmail(email) != null))
            throw new IllegalArgumentException();

        if (name == null || name.length() == 0 || weight < 1)
            throw new IllegalArgumentException();

        user.setName(name);
        user.setSex(User.Sex.valueOf(sex));
        user.setWeight(weight);
        user.setEmail(email);
        userService.updateUser(user);
        return "redirect:user";
    }

    @RequestMapping("/addDrink")
    public String addDrink(HttpSession session, @RequestParam("id") String userId){
        int id = Integer.parseInt(userId);
        authenticationChecks.checkHighLevelRightsToUser(id);
        
        drinkCounterService.addDrink(id);
        return "redirect:parties";
    }
    
    @RequestMapping("/addDrinkToDate")
    public String addDrinkToDate(HttpSession session, @RequestParam("userId") String userId, @RequestParam("date") String date){
        int id = Integer.parseInt(userId);
        authenticationChecks.checkHighLevelRightsToUser(id);

        drinkCounterService.addDrinkToDate(id, date, (Double)session.getAttribute(AuthenticationController.TIMEZONEOFFSET));
        return "redirect:user";
    }

    @RequestMapping("/removeDrink")
    public String removeDrink(HttpSession session, @RequestParam("userId") String userId, @RequestParam("drinkId") String drinkId){
        int id = Integer.parseInt(userId);
        authenticationChecks.checkLowLevelRightsToUser(id);

        drinkCounterService.removeDrinkFromUser(id, Integer.parseInt(drinkId));
        return "redirect:user";
    }

    @RequestMapping("/user")
    public ModelAndView userPage(HttpSession session){
        User user = currentUser.getUser();
        
        ModelAndView mav = new ModelAndView();
        mav.setViewName("user");
        mav.addObject("user", user);
        List<Party> parties = new ArrayList<Party>(user.getParties());
        Collections.sort(parties, new BeanComparator("startTime"));
        mav.addObject("parties", user.getParties());
        
        return mav;
    }
    
    @RequestMapping("/checkEmail")
    public ResponseEntity<byte[]> checkEmail(HttpSession session, @RequestParam("email") String email){
        String data = userService.emailIsCorrect(email) && userService.getUserByEmail(email) == null ? "1" : "0";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "text/plain;charset=utf-8");
        return new ResponseEntity<byte[]>(data.getBytes(), headers, HttpStatus.OK);
    }
    
    @RequestMapping("/getUserByEmail")
    public @ResponseBody String getUserNotInPartyByEmail(HttpSession session, @RequestParam("email") String email, @RequestParam("partyId") String partyId){
        int id = Integer.parseInt(partyId);
        authenticationChecks.checkRightsForParty(id);

        if (!userService.emailIsCorrect(email)){
            return "0";
        }
        
        User user = userService.getUserByEmail(email);
        if (user == null){
            return "0";
        }
        else {
            if(drinkCounterService.isUserParticipant(id, user.getId())){
                return "0";
            }else{
                return Integer.toString(user.getId());
            }
        }
    }
    
    @RequestMapping("/passphrase")
    public ModelAndView passphrase(){
        ModelAndView mav = new ModelAndView();
        mav.setViewName("passphrase");
        mav.addObject("passphrase", currentUser.getUser().getPassphrase());
        return mav;
    }
    
    @RequestMapping("/passphrase-generate")
    public String generatePassphrase() {
        userService.generatePassphrase(currentUser.getUser());
        return "redirect:passphrase";
    }    
}
