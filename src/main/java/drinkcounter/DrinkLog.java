package drinkcounter;

import drinkcounter.model.Drink;
import drinkcounter.model.User;
import drinkcounter.repository.DrinkRepository;
import drinkcounter.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Service
public class DrinkLog {

    private static final Logger log = LoggerFactory.getLogger(DrinkLog.class);
    private static final DateTimeFormatter LOCAL_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final DrinkRepository drinkRepository;
    private final UserRepository userRepository;

    public DrinkLog(DrinkRepository drinkRepository, UserRepository userRepository) {
        this.drinkRepository = drinkRepository;
        this.userRepository = userRepository;
    }

    /** Records a drink at the given time; a time in the future is rejected. */
    @Transactional
    public int record(int userId, Date at) {
        if (at.after(new Date())) throw new IllegalArgumentException("date");
        User user = userRepository.findById(userId).orElseThrow(EntityNotFoundException::new);
        Drink drink = new Drink();
        drink.setTimeStamp(at.toInstant());
        user.drink(drink);
        drinkRepository.save(drink);
        log.info("{} has drunk a drink at {}", user, at.toString());
        return drink.getId();
    }

    /** Records a drink taken now. */
    @Transactional
    public int record(int userId, float grams) {
        User user = userRepository.findById(userId).orElseThrow(EntityNotFoundException::new);
        Drink drink = new Drink();
        drink.setTimeStamp(Instant.now());
        drink.setAlcohol(grams);
        user.drink(drink);
        drinkRepository.save(drink);
        log.info("User {} has drunk a drink", user.getName());
        return drink.getId();
    }

    /** Records a drink; a null time means now and null grams leave the default amount. */
    @Transactional
    public Drink record(int userId, Date at, Float grams) {
        User user = userRepository.findById(userId).orElseThrow(EntityNotFoundException::new);
        Drink drink = new Drink();
        if (at != null) {
            drink.setTimeStamp(at.toInstant());
        } else {
            drink.setTimeStamp(Instant.now());
        }

        if (grams != null) {
            drink.setAlcohol(grams);
        }
        user.drink(drink);
        drinkRepository.save(drink);
        log.info("{} has drunk a drink at {}", user, drink.getTimeStamp());
        return drink;
    }

    /**
     * Records a drink at a "dd.MM.yyyy HH:mm" local time. The offset is the browser's
     * {@code getTimezoneOffset()}, so UTC+02:00 is -120.
     */
    public int recordAt(int userId, String localTime, double timezoneOffset) {
        ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds((int)(-timezoneOffset * 60));
        Instant instant = LocalDateTime.parse(localTime, LOCAL_TIME_FORMAT).toInstant(zoneOffset);

        if (instant.isAfter(Instant.now())) throw new IllegalArgumentException(localTime);

        return record(userId, Date.from(instant));
    }

    @Transactional
    public void undo(int userId, int drinkId) {
        User user = userRepository.findById(userId).orElseThrow(EntityNotFoundException::new);
        Drink drink = findDrinkOf(user, drinkId);
        user.removeDrink(drink);
        drinkRepository.delete(drink);
        log.info("{} has removed a drink {}", user, drink.getTimeStamp());
    }

    @Transactional
    public void correctAlcohol(int userId, int drinkId, float grams) {
        User user = userRepository.findById(userId).orElseThrow(EntityNotFoundException::new);
        Drink drink = findDrinkOf(user, drinkId);
        user.changeDrinkAlcohol(drink, grams);
        log.info("{} has changed drink {} to {} grams", user, drink.getId(), grams);
    }

    public long total() {
        return drinkRepository.count();
    }

    private Drink findDrinkOf(User user, int drinkId) {
        Drink drink = drinkRepository.findById(drinkId).orElseThrow(EntityNotFoundException::new);
        if (drink.getDrinker() == null || !user.getId().equals(drink.getDrinker().getId())) {
            throw new EntityNotFoundException("Drink " + drinkId + " does not belong to user " + user.getId());
        }
        return drink;
    }
}
