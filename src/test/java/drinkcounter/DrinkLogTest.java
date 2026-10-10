package drinkcounter;

import drinkcounter.repository.DrinkRepository;
import drinkcounter.repository.UserRepository;
import drinkcounter.model.Drink;
import drinkcounter.model.User;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DrinkLogTest {

    private DrinkLog drinkLog;
    private UserRepository userRepository;
    private User user;
    private DrinkRepository drinkRepository;

    @BeforeEach
    public void setUp() {
        PromilleTracker.getInstance().reset();

        userRepository = mock(UserRepository.class);
        drinkRepository = mock(DrinkRepository.class);
        AtomicInteger nextDrinkId = new AtomicInteger(1);
        when(drinkRepository.save(any(Drink.class))).thenAnswer(invocation -> {
            Drink drink = invocation.getArgument(0);
            drink.setId(nextDrinkId.getAndIncrement());
            return drink;
        });
        drinkLog = new DrinkLog(drinkRepository, userRepository);

        user = new User();
        user.setId(1);
        user.setWeight(80);
        user.setSex(User.Sex.MALE);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
    }

    @Test
    public void recordAtParsesLocalTimeUsingClientTimezoneOffset() {
        // Client timezone offset is JS-style: UTC+02:00 is reported as -120.
        drinkLog.recordAt(1, "05.03.2024 13:37", -120);

        assertEquals(1, user.getDrinks().size());
        assertEquals(Instant.parse("2024-03-05T11:37:00Z"), user.getDrinks().get(0).getTimeStamp());
    }

    @Test
    public void recordAtRejectsFutureDates() {
        String farFuture = "01.01.2099 00:00";
        assertThrows(IllegalArgumentException.class, () -> drinkLog.recordAt(1, farFuture, 0));
    }

    @Test
    public void correctAlcoholUpdatesDrinkAndPromilles() {
        int drinkId = drinkLog.record(1, new Date());
        Drink drink = user.getDrinks().get(0);
        when(drinkRepository.findById(drinkId)).thenReturn(Optional.of(drink));
        float promillesBefore = user.getPromilles();

        drinkLog.correctAlcohol(1, drinkId, drink.getAlcohol() * 2);

        assertTrue(user.getPromilles() > promillesBefore);
    }

    @Test
    public void correctAlcoholRejectsAnotherUsersDrink() {
        Drink othersDrink = othersDrink();

        assertThrows(EntityNotFoundException.class, () -> drinkLog.correctAlcohol(1, othersDrink.getId(), 1f));
    }

    @Test
    public void undoRejectsAnotherUsersDrink() {
        Drink othersDrink = othersDrink();

        assertThrows(EntityNotFoundException.class, () -> drinkLog.undo(1, othersDrink.getId()));
        verify(drinkRepository, never()).delete(any(Drink.class));
    }

    private Drink othersDrink() {
        User other = new User();
        other.setId(2);
        Drink drink = new Drink();
        drink.setId(99);
        drink.setDrinker(other);
        when(drinkRepository.findById(99)).thenReturn(Optional.of(drink));
        return drink;
    }
}
