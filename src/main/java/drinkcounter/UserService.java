package drinkcounter;

import drinkcounter.repository.DrinkRepository;
import drinkcounter.repository.UserRepository;
import drinkcounter.model.User;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 *
 * @author Toni
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(DrinkCounterService.class);

    @Autowired UserRepository userRepository;
    @Autowired DrinkRepository drinkRepository;

    @Transactional
    public void updateUser(User user) {
        userRepository.save(user);
    }

    @Transactional
    public User addUser(User user) {
        if (!user.isGuest())
            user.setEmail(user.getEmail().toLowerCase());
        userRepository.save(user);
        log.info("User with name {} was added", user.getName());
        return user;
    }

    public User getUser(int userId) {
        return userRepository.findById(userId).orElse(null);
    }

    @Transactional
    public void deleteUser(int userId) {
        User user = getUser(userId);
        
        /*
         * TODO Does this cascade automatically? Test if these are needed
        List<Drink> drinks = drinkRepository.findByDrinker(user);
        for (Drink drink : drinks) {
            drinkRepository.delete(drink);
        }

        // not sure if necessary, stupid object db's
        List<Party> parties = user.getParties();
        if (parties != null) {
            for (Party party : parties) {
                party.getParticipants().remove(user);
            }
        }
         * 
         */

        userRepository.delete(user);
    }

    public User getUserByOpenId(String openId) {
        if (openId == null || openId.length() == 0)
            throw new IllegalArgumentException("openId");

        return userRepository.findByOpenId(openId);
    }
    
    public boolean emailIsCorrect(String email) {
        if (email == null || email.length() == 0) return false;
        
        final String expression = "^(.+)@(.+)\\.(.+)$"; 
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);  
        Matcher matcher = pattern.matcher(email);  
        return matcher.matches();
    }
    
    public User getUserByEmail(String email) {
        if (email == null || email.length() == 0) return null;
        
        return userRepository.findByEmail(email.toLowerCase());
    }

    public void generatePassphrase(User user) {
        String passphrase = UUID.randomUUID().toString();
        user.setPassphrase(passphrase);
        updateUser(user);
    }

    public User getUserByPassphrase(String passphrase) {
        return userRepository.findByPassphrase(passphrase);
    }
}
