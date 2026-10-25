package drinkcounter;

import drinkcounter.alcoholcalculator.BloodAlcohol;
import drinkcounter.model.Drink;
import drinkcounter.model.User;
import drinkcounter.repository.DrinkRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PromillesTest {

    private static final Instant NOW = Instant.parse("2024-03-05T22:00:00Z");

    private DrinkRepository drinkRepository;
    private Promilles promilles;

    @BeforeEach
    public void setUp() {
        drinkRepository = mock(DrinkRepository.class);
        promilles = new Promilles(drinkRepository);
        ReflectionTestUtils.setField(promilles, "clock", Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    public void readsOnlyTheDrinksOfTheLastWeek() {
        User user = user(1);

        promilles.of(user);

        verify(drinkRepository).findByDrinkersSince(List.of(1), NOW.minus(Duration.ofDays(7)));
    }

    @Test
    public void readsTheDrinksOfManyUsersAtOnce() {
        User drinker = user(1);
        User sober = user(2);
        Drink drink = new Drink();
        drink.setTimeStamp(NOW.minus(Duration.ofMinutes(30)));
        drinker.drink(drink);
        when(drinkRepository.findByDrinkersSince(List.of(1, 2), NOW.minus(Duration.ofDays(7))))
                .thenReturn(List.of(drink));

        Map<Integer, BloodAlcohol> bloodAlcohol = promilles.of(List.of(drinker, sober));

        assertTrue(bloodAlcohol.get(1).promilles() > 0);
        assertEquals(1, bloodAlcohol.get(1).drinksSinceSober());
        assertEquals(0f, bloodAlcohol.get(2).promilles());
    }

    private static User user(int id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
