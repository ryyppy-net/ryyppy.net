package drinkcounter.repository;

import drinkcounter.model.Drink;
import drinkcounter.model.User;
import org.springframework.data.repository.CrudRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author Toni
 */
public interface DrinkRepository extends CrudRepository<Drink, Integer> {
    List<Drink> findByDrinker(User drinker);

    List<Drink> findByDrinkerIdInAndTimeStampAfterOrderByTimeStampAsc(Collection<Integer> drinkerIds, Instant since);
}
