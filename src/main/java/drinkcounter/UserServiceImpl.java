/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter;

import drinkcounter.dao.DrinkDAO;
import drinkcounter.dao.UserDAO;
import drinkcounter.model.Drink;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import java.util.LinkedList;
import java.util.List;
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
public class UserServiceImpl implements UserService{

    private static final Logger log = LoggerFactory.getLogger(DrinkCounterServiceImpl.class);

    @Autowired UserDAO userDAO;
    @Autowired DrinkDAO drinkDAO;

    // this doesn't return include anonymous users
    @Override
    public List<User> listUsers() {
        List<User> list = userDAO.readAll();
        List<User> list2 = new LinkedList<User>();
        for (User user : list) {
            if (!user.isGuest())
                list2.add(user);
        }
        return list2;
    }

    @Override
    @Transactional
    public User addUser(User user) {
        if (!user.isGuest())
            user.setEmail(user.getEmail().toLowerCase());
        userDAO.save(user);
        log.info("User with name {} was added", user.getName());
        return user;
    }

    @Override
    public User getUser(int userId) {
        return userDAO.readByPrimaryKey(userId);
    }

    @Override
    @Transactional
    public void deleteUser(int userId) {
        User user = getUser(userId);
        
        /*
         * Does this cascade automatically? Test if these are needed
        List<Drink> drinks = drinkDAO.findByDrinker(user);
        for (Drink drink : drinks) {
            drinkDAO.delete(drink);
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

        userDAO.delete(user);
    }

    @Override
    public User getUserByOpenId(String openId) {
        if (openId == null || openId.length() == 0)
            throw new IllegalArgumentException("openId");

        return userDAO.findByOpenId(openId);
    }
    
    @Override
    public boolean emailIsCorrect(String email) {
        if (email == null || email.length() == 0) return false;
        
        final String expression = "^[\\w\\-]([\\.\\w])+[\\w]+@([\\w\\-]+\\.)+[A-Z]{2,4}$"; 
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);  
        Matcher matcher = pattern.matcher(email);  
        return matcher.matches();
    }
    
    @Override
    public User getUserByEmail(String email) {
        if (email == null || email.length() == 0) return null;
        
        return userDAO.findByEmail(email.toLowerCase());
    }    
}
