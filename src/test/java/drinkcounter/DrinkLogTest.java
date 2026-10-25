package drinkcounter;

import drinkcounter.repository.DrinkRepository;
import drinkcounter.repository.UserRepository;
import drinkcounter.model.Drink;
import drinkcounter.model.User;
import jakarta.persistence.EntityNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DrinkLogTest {

    private DrinkLog drinkLog;
    private UserRepository userRepository;
    private User user;
    private DrinkRepository drinkRepository;

    @BeforeEach
    public void setUp() {
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
    public void correctAlcoholUpdatesTheUsersDrink() {
        Drink drink = usersDrink();

        drinkLog.correctAlcohol(1, drink.getId(), 30f);

        assertEquals(30f, drink.getAlcohol());
    }

    @Test
    public void correctAlcoholRejectsAnotherUsersDrink() {
        Drink othersDrink = othersDrink();

        assertThrows(EntityNotFoundException.class, () -> drinkLog.correctAlcohol(1, othersDrink.getId(), 1f));
    }

    @Test
    public void undoDeletesTheUsersDrinkFoundInTheirDrinkList() {
        Drink drink = usersDrink();

        drinkLog.undo(1, drink.getId());

        assertTrue(user.getDrinks().isEmpty());
        verify(drinkRepository).delete(drink);
        verify(drinkRepository, never()).findById(any());
    }

    @Test
    public void undoRejectsAnotherUsersDrink() {
        Drink othersDrink = othersDrink();

        assertThrows(EntityNotFoundException.class, () -> drinkLog.undo(1, othersDrink.getId()));
        verify(drinkRepository, never()).delete(any(Drink.class));
    }

    private Drink usersDrink() {
        int drinkId = drinkLog.record(1, new Date());
        return user.getDrinks().get(0);
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

    @Test
    public void totalIsCountedAtMostOnceAMinute() {
        Instant start = Instant.parse("2024-03-05T12:00:00Z");
        ReflectionTestUtils.setField(drinkLog, "clock", Clock.fixed(start, ZoneOffset.UTC));
        when(drinkRepository.count()).thenReturn(10L, 11L);

        assertEquals(10L, drinkLog.total());
        ReflectionTestUtils.setField(drinkLog, "clock", Clock.fixed(start.plusSeconds(59), ZoneOffset.UTC));
        assertEquals(10L, drinkLog.total());
        verify(drinkRepository, times(1)).count();

        ReflectionTestUtils.setField(drinkLog, "clock", Clock.fixed(start.plusSeconds(60), ZoneOffset.UTC));
        assertEquals(11L, drinkLog.total());
    }
}
