package drinkcounter;

import drinkcounter.model.Drink;
import drinkcounter.model.User;
import drinkcounter.repository.DrinkRepository;
import drinkcounter.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DrinkLog {

    private static final Logger log = LoggerFactory.getLogger(DrinkLog.class);
    private static final DateTimeFormatter LOCAL_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    // The login page shows the total; counting every drink on each view reads the whole table.
    private static final Duration TOTAL_MAX_AGE = Duration.ofMinutes(1);

    private final DrinkRepository drinkRepository;
    private final UserRepository userRepository;

    private Clock clock = Clock.systemUTC();
    private volatile Instant totalCountedAt = Instant.EPOCH;
    private volatile long total;

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
        ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds((int) (-timezoneOffset * 60));
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
    public Drink correctAlcohol(int userId, int drinkId, float grams) {
        User user = userRepository.findById(userId).orElseThrow(EntityNotFoundException::new);
        Drink drink = findDrinkOf(user, drinkId);
        drink.setAlcohol(grams);
        log.info("{} has changed drink {} to {} grams", user, drink.getId(), grams);
        return drink;
    }

    /** All drinks ever recorded, counted at most once a minute. */
    public long total() {
        Instant now = clock.instant();
        if (Duration.between(totalCountedAt, now).compareTo(TOTAL_MAX_AGE) >= 0) {
            total = drinkRepository.count();
            totalCountedAt = now;
        }
        return total;
    }

    /** Looks in the user's own drinks, which the own-drink responses read anyway. */
    private Drink findDrinkOf(User user, int drinkId) {
        return user.getDrinks().stream()
                .filter(drink -> Objects.equals(drink.getId(), drinkId))
                .findFirst()
                .orElseThrow(() ->
                        new EntityNotFoundException("Drink " + drinkId + " does not belong to user " + user.getId()));
    }
}
