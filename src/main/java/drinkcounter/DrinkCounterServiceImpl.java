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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
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
    public Party startParty(String partyName) {
        if(StringUtils.isBlank(partyName)){
            throw new IllegalArgumentException("Party name cannot be empty");
        }

        Party party = new Party();
        party.setName(partyName);
        party.setStartTime(new Date());
        partyDao.save(party);

        log.info("Party {}, id {} was started!", partyName, party.getId());
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
    public Party getParty(int partyId) {
        return partyDao.readByPrimaryKey(partyId);
    }

    @Override
    public List<User> listUsersByParty(int partyId) {
        return new LinkedList<User>(getParty(partyId).getParticipants());
    }
    
    @Override
    @Transactional
    public void linkUserToParty(int userId, int partyIdentifier) {
        Party party = getParty(partyIdentifier);
        User user = userDAO.readByPrimaryKey(userId);
        
        for (User current : getParty(party.getId()).getParticipants()) {
            if (current.getId() == user.getId()) {
                log.info("{} was already added to party {}. Skipping", user, party.getName());
                return;
            }
        }
        
        party.addParticipant(user);
        partyDao.save(party);
        log.info("{} was added to party {}", user, party.getName());
    }

    @Override
    @Transactional
    public void unlinkUserFromParty(int userId, int partyId) {
        Party party = getParty(partyId);
        User user = userDAO.readByPrimaryKey(userId);
        party.removeParticipant(user);
        partyDao.save(party);
        log.info("{} was removed from party {}", user, party.getName());
    }

    @Override
    @Transactional
    public int addDrink(int userIdentifier) {
        return addDrink(userIdentifier, new Date());
    }
    
    @Override
    public List<Drink> getDrinks(int userIdentifier) {
        User user = userDAO.readByPrimaryKey(userIdentifier);
        return drinkDao.findByDrinker(user);
    }

    @Override
    @Transactional
    public void removeDrinkFromUser(int userId, int drinkId) {
        User user = userDAO.readByPrimaryKey(userId);
        Drink drink = drinkDao.readByPrimaryKey(drinkId);
        user.removeDrink(drink);
        drinkDao.delete(drink);
    }

    @Override
    public int addDrinkToDate(int userId, String date, double timezoneOffset) {
        DateTimeZone dtz = DateTimeZone.forOffsetMillis((int)(-timezoneOffset * 60 * 1000));
        DateTimeFormatter parser = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm").withZone(dtz);
        DateTime dt = parser.parseDateTime(date);

        if (dt.isAfterNow()) throw new IllegalArgumentException(date);

        return addDrink(userId, dt.toDate());
    }

    @Override
    @Transactional
    public int addDrink(int userId, Date date) {
        if (date.after(new Date())) throw new IllegalArgumentException("date");
        User user = userDAO.readByPrimaryKey(userId);
        Drink drink = new Drink();
        drink.setTimeStamp(date);
        user.drink(drink);
        drinkDao.save(drink);
        log.info("{} has drunk a drink at {}", user, date.toString());
        return drink.getId();
    }

    @Override
    public long getTotalDrinkCount() {
        return drinkDao.count();
    }

    @Override
    public boolean isUserParticipant(int partyId, int userId) {
        return partyDao.countUserParticipations(partyId, userId) > 0;
    }

    @Override
    public int addDrink(int userId, float alcoholAmount) {
        User user = userDAO.readByPrimaryKey(userId);
        Drink drink = new Drink();
        drink.setTimeStamp(new Date());
        drink.setAlcohol(alcoholAmount);
        user.drink(drink);
        drinkDao.save(drink);
        log.info("User {} has drunk a drink", user.getName());
        return drink.getId();
    }
}
