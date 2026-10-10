package drinkcounter;

import drinkcounter.repository.UserRepository;
import drinkcounter.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Toni
 */
@Service
public class UserAccounts {

    private static final Logger log = LoggerFactory.getLogger(DrinkCounterService.class);

    private final UserRepository userRepository;

    public UserAccounts(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void update(User user) {
        userRepository.save(user);
    }

    @Transactional
    public User add(User user) {
        if (!user.isGuest())
            user.setEmail(user.getEmail().toLowerCase());
        userRepository.save(user);
        log.info("User with name {} was added", user.getName());
        return user;
    }

    public User get(int userId) {
        return userRepository.findById(userId).orElse(null);
    }

    @Transactional
    public void delete(int userId) {
        User user = get(userId);
        
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

    public User byOpenId(String openId) {
        if (openId == null || openId.length() == 0)
            throw new IllegalArgumentException("openId");

        return userRepository.findByOpenId(openId);
    }

    public User byEmail(String email) {
        if (email == null || email.length() == 0) return null;
        
        return userRepository.findByEmail(email.toLowerCase());
    }
}
