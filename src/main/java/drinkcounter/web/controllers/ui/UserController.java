package drinkcounter.web.controllers.ui;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import drinkcounter.model.User;
import drinkcounter.DrinkCounterService;
import drinkcounter.EmailAddress;
import drinkcounter.PassphraseLogin;
import drinkcounter.UserAccounts;
import drinkcounter.authentication.LoggedInUser;
import drinkcounter.authentication.OwnUser;
import drinkcounter.authentication.OwnUserOrPartyMate;
import drinkcounter.authentication.PartyAccess;
import drinkcounter.authentication.PartyMember;
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
    private final PartyAccess partyAccess;
    private final UserAccounts userAccounts;
    private final PassphraseLogin passphraseLogin;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public UserController(
            DrinkCounterService drinkCounterService,
            PartyAccess partyAccess,
            UserAccounts userAccounts,
            PassphraseLogin passphraseLogin,
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        this.drinkCounterService = drinkCounterService;
        this.partyAccess = partyAccess;
        this.userAccounts = userAccounts;
        this.passphraseLogin = passphraseLogin;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @RequestMapping("/newuser")
    public String newUser(){
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

        if (name == null || name.length() == 0 || weight < 1 || !EmailAddress.isValid(email) || userAccounts.byEmail(email) != null) {
            throw new IllegalArgumentException();
        }



        User user = new User();
        user.setName(name);
        user.setSex(User.Sex.valueOf(sex));
        user.setWeight(weight);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setAuthMethod(User.AuthMethod.PASSWORD);

        userAccounts.add(user);
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
    @OwnUser
    public String modifyUser(
            @RequestParam("userId") int userId,
            @RequestParam("name") String name,
            @RequestParam("sex") String sex,
            @RequestParam("weight") float weight, 
            @RequestParam("email") String email){
                
        User user = userAccounts.get(userId);

        if (!user.getEmail().equalsIgnoreCase(email) && (!EmailAddress.isValid(email) || userAccounts.byEmail(email) != null))
            throw new IllegalArgumentException();

        if (name == null || name.length() == 0 || weight < 1)
            throw new IllegalArgumentException();

        user.setName(name);
        user.setSex(User.Sex.valueOf(sex));
        user.setWeight(weight);
        user.setEmail(email);
        userAccounts.update(user);
        return "redirect:user";
    }

    @RequestMapping("/addDrinkToDate")
    @OwnUserOrPartyMate
    public String addDrinkToDate(HttpSession session, @RequestParam("userId") int userId, @RequestParam("date") String date){
        drinkCounterService.addDrinkToDate(userId, date, AuthenticationController.timezoneOffset(session));
        return "redirect:user";
    }

    @RequestMapping("/removeDrink")
    @OwnUser
    public String removeDrink(@RequestParam("userId") int userId, @RequestParam("drinkId") int drinkId){
        drinkCounterService.removeDrinkFromUser(userId, drinkId);
        return "redirect:user";
    }

    @RequestMapping("/user")
    public ModelAndView userPage(@LoggedInUser User user){
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
    public ResponseEntity<byte[]> checkEmail(@RequestParam("email") String email){
        String data = EmailAddress.isValid(email) && userAccounts.byEmail(email) == null ? "1" : "0";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "text/plain;charset=utf-8");
        return new ResponseEntity<byte[]>(data.getBytes(), headers, HttpStatus.OK);
    }
    
    @RequestMapping("/getUserByEmail")
    @PartyMember
    public @ResponseBody String getUserNotInPartyByEmail(@RequestParam("email") String email, @RequestParam("partyId") int partyId){
        if (!EmailAddress.isValid(email)){
            return "0";
        }
        
        User user = userAccounts.byEmail(email);
        if (user == null){
            return "0";
        }
        else {
            if(partyAccess.isMember(partyId, user.getId())){
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
        passphraseLogin.issue(user);
        return "redirect:passphrase";
    }    
}
