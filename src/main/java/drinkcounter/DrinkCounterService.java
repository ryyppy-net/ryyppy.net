package drinkcounter;

import drinkcounter.repository.DrinkRepository;
import drinkcounter.repository.PartyRepository;
import drinkcounter.repository.UserRepository;
import drinkcounter.model.Drink;
import drinkcounter.model.Friend;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import drinkcounter.web.controllers.api.v2.GravatarUrls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 *
 * @author Toni
 */
@Service
public class DrinkCounterService {

    private static final Logger log = LoggerFactory.getLogger(DrinkCounterService.class);
    private static final DateTimeFormatter ADD_DRINK_TO_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Autowired
    private PartyRepository partyRepository;
    @Autowired
    private DrinkRepository drinkRepository;
    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public Party startParty(String partyName) {
        if(partyName == null || partyName.isBlank()) {
            throw new IllegalArgumentException("Party name cannot be empty");
        }

        Party party = new Party();
        party.setName(partyName);
        party.setStartTime(Instant.now());
        partyRepository.save(party);

        log.info("Party {}, id {} was started!", partyName, party.getId());
        return party;
    }

    public Party getParty(int partyId) {
        return partyRepository.findById(partyId).orElseThrow(EntityNotFoundException::new);
    }

    public List<User> listUsersByParty(int partyId) {
        return new LinkedList<User>(getParty(partyId).getParticipants());
    }
    
    @Transactional
    public void linkUserToParty(int userId, int partyIdentifier) {
        Party party = getParty(partyIdentifier);
        User user = userRepository.findById(userId).orElseThrow(EntityNotFoundException::new);

        for (User current : getParty(party.getId()).getParticipants()) {
            if (current.getId().equals(user.getId())) {
                log.info("{} was already added to party {}. Skipping", user, party.getName());
                return;
            }
        }
        
        party.addParticipant(user);
        partyRepository.save(party);
        log.info("{} was added to party {}", user, party.getName());
    }

    @Transactional
    public void unlinkUserFromParty(int userId, int partyId) {
        Party party = getParty(partyId);
        User user = userRepository.findById(userId).orElseThrow(EntityNotFoundException::new);
        party.removeParticipant(user);
        partyRepository.save(party);
        log.info("{} was removed from party {}", user, party.getName());
    }

    @Transactional
    public int addDrink(int userIdentifier) {
        return addDrink(userIdentifier, new Date());
    }

    @Transactional
    public void removeDrinkFromUser(int userId, int drinkId) {
        User user = userRepository.findById(userId).orElseThrow(EntityNotFoundException::new);
        Drink drink = findDrinkOf(user, drinkId);
        user.removeDrink(drink);
        drinkRepository.delete(drink);
        log.info("{} has removed a drink {}", user, drink.getTimeStamp());
    }

    @Transactional
    public void changeDrinkAlcohol(int userId, int drinkId, float alcoholAmount) {
        User user = userRepository.findById(userId).orElseThrow(EntityNotFoundException::new);
        Drink drink = findDrinkOf(user, drinkId);
        user.changeDrinkAlcohol(drink, alcoholAmount);
        log.info("{} has changed drink {} to {} grams", user, drink.getId(), alcoholAmount);
    }

    private Drink findDrinkOf(User user, int drinkId) {
        Drink drink = drinkRepository.findById(drinkId).orElseThrow(EntityNotFoundException::new);
        if (drink.getDrinker() == null || !user.getId().equals(drink.getDrinker().getId())) {
            throw new EntityNotFoundException("Drink " + drinkId + " does not belong to user " + user.getId());
        }
        return drink;
    }

    public int addDrinkToDate(int userId, String date, double timezoneOffset) {
        ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds((int)(-timezoneOffset * 60));
        Instant instant = LocalDateTime.parse(date, ADD_DRINK_TO_DATE_FORMAT).toInstant(zoneOffset);

        if (instant.isAfter(Instant.now())) throw new IllegalArgumentException(date);

        return addDrink(userId, Date.from(instant));
    }

    @Transactional
    public int addDrink(int userId, Date date) {
        if (date.after(new Date())) throw new IllegalArgumentException("date");
        User user = userRepository.findById(userId).orElseThrow(EntityNotFoundException::new);
        Drink drink = new Drink();
        drink.setTimeStamp(date.toInstant());
        user.drink(drink);
        drinkRepository.save(drink);
        log.info("{} has drunk a drink at {}", user, date.toString());
        return drink.getId();
    }

    public long getTotalDrinkCount() {
        return drinkRepository.count();
    }

    @Transactional(readOnly = true)
    public List<Friend> suggestInvitations(int forUser, int partyId, int amount) {
        TypedQuery<Object[]> q = em.createQuery("select distinct par.id, p.startTime \n" +
                " from Party p join p.participants par \n" +
                " where par.id not in(select pp.id from Party p join p.participants pp where p.id = :partyId)\n" +
                " and p in (select party from Party party join party.participants participant where participant.id = :userId)\n" +
                " and par.guest = false\n" +
                " order by p.startTime desc", Object[].class);
        q.setParameter("partyId", partyId);
        q.setParameter("userId", forUser);
        q.setFirstResult(0);
        q.setMaxResults(amount);
        List<Object[]> results = q.getResultList();
        List<Friend> friends = new ArrayList<Friend>();
        for (Object[] tuple : results) {
            User user = userRepository.findById((Integer)tuple[0]).orElseThrow(EntityNotFoundException::new);
            friends.add(new Friend(user.getId(), user.getName(), GravatarUrls.forUser(user)));
        }
        return friends;
    }

    @Transactional
    public int addDrink(int userId, float alcoholAmount) {
        User user = userRepository.findById(userId).orElseThrow(EntityNotFoundException::new);
        Drink drink = new Drink();
        drink.setTimeStamp(Instant.now());
        drink.setAlcohol(alcoholAmount);
        user.drink(drink);
        drinkRepository.save(drink);
        log.info("User {} has drunk a drink", user.getName());
        return drink.getId();
    }

    @Transactional
    public Drink addDrink(int userId, Date date, Float alcoholAmount) {
        User user = userRepository.findById(userId).orElseThrow(EntityNotFoundException::new);
        Drink drink = new Drink();
        if(date != null){
            drink.setTimeStamp(date.toInstant());
        }else{
            drink.setTimeStamp(Instant.now());
        }
        
        if(alcoholAmount != null){
            drink.setAlcohol(alcoholAmount);
        }
        user.drink(drink);
        drinkRepository.save(drink);
        log.info("{} has drunk a drink at {}", user, drink.getTimeStamp());
        return drink;
    }
}
