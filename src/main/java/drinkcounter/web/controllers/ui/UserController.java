package drinkcounter.web.controllers.ui;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import drinkcounter.model.User;
import drinkcounter.DrinkCounterService;
import drinkcounter.UserService;
import drinkcounter.authentication.AuthenticationChecks;
import drinkcounter.authentication.LoggedInUser;
import drinkcounter.model.Party;
import java.util.Comparator;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.ModelAndView;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static drinkcounter.web.controllers.DefaultController.REDIRECT_TO_FRONTPAGE;

/**
 *
 * @author murgo
 */
@Controller
@RequestMapping("ui")
public class UserController {
    private final DrinkCounterService drinkCounterService;
    private final UserService userService;
    private final AuthenticationChecks authenticationChecks;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public UserController(
            DrinkCounterService drinkCounterService,
            UserService userService,
            AuthenticationChecks authenticationChecks,
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        this.drinkCounterService = drinkCounterService;
        this.userService = userService;
        this.authenticationChecks = authenticationChecks;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @RequestMapping("/newuser")
    public String newUser(HttpSession session){
        return "newuser";
    }
    
    @RequestMapping("/addUser")
    public String addUser(
            @RequestParam("name") String name,
            @RequestParam("sex") String sex,
            @RequestParam("weight") float weight, 
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response){

        if (session.getAttribute(AuthenticationController.TIMEZONEOFFSET) == null){
            session.setAttribute(AuthenticationController.TIMEZONEOFFSET, (double) 0);
        }

        if (name == null || name.length() == 0 || weight < 1 || !userService.emailIsCorrect(email) || userService.getUserByEmail(email) != null) {
            throw new IllegalArgumentException();
        }



        User user = new User();
        user.setName(name);
        user.setSex(User.Sex.valueOf(sex));
        user.setWeight(weight);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setAuthMethod(User.AuthMethod.PASSWORD);

        userService.addUser(user);
        authenticate(user, request, response);

        return REDIRECT_TO_FRONTPAGE;
    }

    private void authenticate(User user, HttpServletRequest request, HttpServletResponse response){
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
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
    public ModelAndView userPage(HttpSession session, @LoggedInUser User user){
        ModelAndView mav = new ModelAndView();
        mav.setViewName("user");
        mav.addObject("user", user);
        mav.addObject("parties", user.getParties().stream()
            .sorted(Comparator.comparing(Party::getStartTime).reversed())
            .collect(Collectors.toList())
        );
        
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
    public ModelAndView passphrase(@LoggedInUser User user){
        ModelAndView mav = new ModelAndView();
        mav.setViewName("passphrase");
        mav.addObject("passphrase", user.getPassphrase());
        return mav;
    }
    
    @RequestMapping("/passphrase-generate")
    public String generatePassphrase(@LoggedInUser User user) {
        userService.generatePassphrase(user);
        return "redirect:passphrase";
    }    
}
