package drinkcounter;

import drinkcounter.dao.DrinkDAO;
import drinkcounter.dao.UserDAO;
import drinkcounter.dao.PartyDAO;
import drinkcounter.model.Drink;
import drinkcounter.model.User;
import drinkcounter.model.Party;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Toni
 */
@Service
public class DrinkCounterServiceImpl implements DrinkCounterService {

    private static final Logger log = LoggerFactory.getLogger(DrinkCounterServiceImpl.class);
    @Autowired
    private PartyDAO partyDao;
    @Autowired
    private DrinkDAO drinkDao;
    @Autowired
    private UserDAO userDAO;

    @Override
    @Transactional
    public Party startParty(String identifier) {
        if(StringUtils.isBlank(identifier)){
            throw new IllegalArgumentException("Party id cannot be empty");
        }
        Party existingParty = partyDao.findById(identifier);
        if(existingParty != null){
            throw new IllegalArgumentException("Party id must be unique");
        }
        Party party = new Party();
        party.setId(identifier);
        partyDao.save(party);

        log.info("Party {} was started!", identifier);
        return party;
    }

    @Override
    @Transactional
    public void updateParty(Party party) {
        partyDao.save(party);
    }

    @Override
    public List<Party> listParties() {
        return partyDao.readAll();
    }

    @Override
    public Party getParty(String identifier) {
        return partyDao.findById(identifier);
    }

    @Override
    public List<User> listUsersByParty(String partyIdentifier) {
        return new LinkedList<User>(getParty(partyIdentifier).getParticipants());
    }

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
        userDAO.save(user);
        log.info("User with name {} was added", user.getName());
        return user;
    }
    
    @Override
    @Transactional
    public void linkUserToParty(String userId, String partyIdentifier) {
        Party party = getParty(partyIdentifier);
        User user = getUser(userId);
        party.addParticipant(user);
        partyDao.save(party);
        log.info("User with name {} was added to party {}", user.getName(), party.getName());
    }

    @Override
    @Transactional
    public void addDrink(String userIdentifier) {
        User user = userDAO.readByPrimaryKey(Integer.parseInt(userIdentifier));
        Drink drink = new Drink();
        drink.setDrinker(user);
        drink.setTimeStamp(new Date());

        user.drink(drink);
        
        drinkDao.save(drink);
        log.info("User {} has drunk a drink", user.getName());
    }

    @Override
    public List<Drink> getDrinks(String userIdentifier) {
        User user = getUser(userIdentifier);
        return drinkDao.findByDrinker(user);
    }

    @Override
    public User getUser(String userId) {
        return userDAO.readByPrimaryKey(Integer.parseInt(userId));
    }

    // can't we use CASCADE
    @Override
    @Transactional
    public void deleteUser(String userId) {
        User user = getUser(userId);
        List<Drink> drinks = getDrinks(userId);
        for (Drink drink : drinks) {
            drinkDao.delete(drink);
        }
        
        // not sure if necessary, stupid object db's
        List<Party> parties = user.getParties();
        if (parties != null) {
            for (Party party : parties) {
                party.getParticipants().remove(user);
            }
        }
        
        userDAO.delete(user);
    }
}
