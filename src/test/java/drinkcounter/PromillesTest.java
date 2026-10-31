package drinkcounter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import drinkcounter.alcoholcalculator.BloodAlcohol;
import drinkcounter.model.Drink;
import drinkcounter.model.User;
import drinkcounter.repository.DrinkRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class PromillesTest {

    private static final Instant NOW = Instant.parse("2024-03-05T22:00:00Z");
    private static final float EXACT = 0.0001f;

    private final List<Drink> stored = new ArrayList<>();
    private DrinkRepository drinkRepository;
    private Promilles promilles;

    @BeforeEach
    public void setUp() {
        drinkRepository = mock(DrinkRepository.class);
        // Answers as the query does: the given drinkers' drinks after the cutoff, oldest first.
        when(drinkRepository.findByDrinkerIdInAndTimeStampAfterOrderByTimeStampAsc(any(), any()))
                .thenAnswer(invocation -> {
                    Collection<Integer> ids = invocation.getArgument(0);
                    Instant since = invocation.getArgument(1);
                    return stored.stream()
                            .filter(drink -> ids.contains(drink.getDrinker().getId()))
                            .filter(drink -> drink.getTimeStamp().isAfter(since))
                            .sorted(Comparator.comparing(Drink::getTimeStamp))
                            .toList();
                });
        promilles = new Promilles(drinkRepository);
        ReflectionTestUtils.setField(promilles, "clock", Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    public void readsOnlyTheDrinksOfTheLastWeek() {
        User user = user(1, 80);

        promilles.of(user);

        verify(drinkRepository)
                .findByDrinkerIdInAndTimeStampAfterOrderByTimeStampAsc(List.of(1), NOW.minus(Duration.ofDays(7)));
    }

    @Test
    public void drinksBeforeTheWeekDoNotMatterOnceTheUserSoberedUp() {
        User man80 = user(1, 80);
        drink(man80, 12, Duration.ofDays(-10));
        drink(man80, 12, Duration.ofHours(-1));

        // 80 kg burns 8 g an hour.
        assertEquals(4f, promilles.of(man80).grams(), EXACT);
    }

    @Test
    public void aSessionLongerThanAWeekIsCutShort() {
        User man10 = user(1, 10);
        drink(man10, 500, Duration.ofDays(-8));

        // 10 kg burns 1 g an hour, so 500 g drunk 192 hours ago would leave 308 g.
        assertEquals(0f, promilles.of(man10).grams(), EXACT);
    }

    @Test
    public void readsAPartyInOneQueryAndKeepsEachMembersDrinksApart() {
        User man80 = user(1, 80);
        User man100 = user(2, 100);
        User sober = user(3, 80);
        drink(man80, 12, Duration.ofHours(-1));
        drink(man100, 12, Duration.ofHours(-1));
        drink(man100, 12, Duration.ofMinutes(-30));

        List<BloodAlcohol> bloodAlcohol = promilles.of(List.of(man80, man100, sober));

        verify(drinkRepository, times(1)).findByDrinkerIdInAndTimeStampAfterOrderByTimeStampAsc(any(), any());
        assertEquals(4f, bloodAlcohol.get(0).grams(), EXACT);
        assertEquals(1, bloodAlcohol.get(0).drinksSinceSober());
        // 100 kg burns 10 g an hour: 12 g burn to 7 g by the second drink, 19 g burn to 14 g by now.
        assertEquals(14f, bloodAlcohol.get(1).grams(), EXACT);
        assertEquals(2, bloodAlcohol.get(1).drinksSinceSober());
        assertEquals(0f, bloodAlcohol.get(2).grams(), EXACT);
        assertEquals(0, bloodAlcohol.get(2).drinksSinceSober());
    }

    @Test
    public void aPartyWithoutMembersReadsNothing() {
        assertTrue(promilles.of(List.of()).isEmpty());
        verify(drinkRepository, never()).findByDrinkerIdInAndTimeStampAfterOrderByTimeStampAsc(any(), any());
    }

    private void drink(User user, float grams, Duration fromNow) {
        Drink drink = new Drink();
        drink.setAlcohol(grams);
        drink.setTimeStamp(NOW.plus(fromNow));
        user.drink(drink);
        stored.add(drink);
    }

    private static User user(int id, float weight) {
        User user = new User();
        user.setId(id);
        user.setWeight(weight);
        return user;
    }
}
