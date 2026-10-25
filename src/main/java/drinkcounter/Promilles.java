package drinkcounter;

import drinkcounter.alcoholcalculator.BloodAlcohol;
import drinkcounter.model.Drink;
import drinkcounter.model.User;
import drinkcounter.repository.DrinkRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Blood alcohol of users now, read from their recent drinks only.
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
        return of(List.of(user)).get(user.getId());
    }

    /** Keyed by user id; reads the drinks of all the users at once. */
    public Map<Integer, BloodAlcohol> of(Collection<User> users) {
        if (users.isEmpty()) return Map.of();
        Instant now = clock.instant();
        List<Integer> ids = users.stream().map(User::getId).toList();
        Map<Integer, List<Drink>> drinksByUser = drinkRepository.findByDrinkersSince(ids, now.minus(LOOKBACK)).stream()
                .collect(Collectors.groupingBy(drink -> drink.getDrinker().getId()));

        Map<Integer, BloodAlcohol> result = new HashMap<>();
        for (User user : users) {
            result.put(user.getId(), new BloodAlcohol(user, drinksByUser.getOrDefault(user.getId(), List.of()), now));
        }
        return result;
    }
}
