package drinkcounter;

import drinkcounter.alcoholcalculator.BloodAlcohol;
import drinkcounter.model.Drink;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import drinkcounter.repository.DrinkRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Blood alcohol of users now, read from their recent drinks only, or within a party from the
 * drinks logged during it only.
 */
@Service
public class Promilles {

    // Drinks before this are ignored, so a drinking session longer than a week is cut short.
    static final Duration LOOKBACK = Duration.ofDays(7);

    private final DrinkRepository drinkRepository;

    private Clock clock = Clock.systemUTC();

    public Promilles(DrinkRepository drinkRepository) {
        this.drinkRepository = drinkRepository;
    }

    public BloodAlcohol of(User user) {
        return of(List.of(user)).get(0);
    }

    /** In the users' order; reads the drinks of all the users at once. */
    public List<BloodAlcohol> of(Collection<User> users) {
        if (users.isEmpty()) return List.of();
        Instant now = clock.instant();
        List<Integer> ids = users.stream().map(User::getId).toList();
        Map<Integer, List<Drink>> drinksByUser = drinkRepository.findByDrinkerIdInAndTimeStampAfterOrderByTimeStampAsc(ids, now.minus(LOOKBACK)).stream()
                .collect(Collectors.groupingBy(drink -> drink.getDrinker().getId()));
        return users.stream()
                .map(user -> new BloodAlcohol(user, drinksByUser.getOrDefault(user.getId(), List.of()), now))
                .toList();
    }

    /**
     * In the users' order, as of the party's end or now, whichever is first, from only the drinks
     * logged from its start to then: party members see each other through the party alone.
     */
    public List<BloodAlcohol> during(Party party, Collection<User> users) {
        if (users.isEmpty()) return List.of();
        Instant start = party.getStartTime();
        Instant end = latest(start, earliest(clock.instant(), party.getEndTime()));
        List<Integer> ids = users.stream().map(User::getId).toList();
        Map<Integer, List<Drink>> drinksByUser = drinkRepository.findByDrinkerIdInAndTimeStampBetweenOrderByTimeStampAsc(ids, start, end).stream()
                .collect(Collectors.groupingBy(drink -> drink.getDrinker().getId()));
        return users.stream()
                .map(user -> new BloodAlcohol(user, drinksByUser.getOrDefault(user.getId(), List.of()), end))
                .toList();
    }

    private static Instant earliest(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }

    private static Instant latest(Instant a, Instant b) {
        return a.isAfter(b) ? a : b;
    }
}
