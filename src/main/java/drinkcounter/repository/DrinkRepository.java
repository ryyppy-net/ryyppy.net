package drinkcounter.repository;

import drinkcounter.model.Drink;
import drinkcounter.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author Toni
 */
public interface DrinkRepository extends CrudRepository<Drink, Integer> {
    List<Drink> findByDrinker(User drinker);

    /** The user's drinks, oldest first. */
    List<Drink> findByDrinkerIdOrderByTimeStampAsc(int drinkerId);

    @Query("select d.drinker.id, max(d.timeStamp) from Drink d"
            + " where d.drinker.id in :drinkerIds group by d.drinker.id")
    List<LastDrink> findLastDrinks(@Param("drinkerIds") Collection<Integer> drinkerIds);

    List<Drink> findByDrinkerIdInAndTimeStampAfterOrderByTimeStampAsc(Collection<Integer> drinkerIds, Instant since);

    /** The drinkers' drinks from {@code start} to {@code end}, both included, oldest first. */
    List<Drink> findByDrinkerIdInAndTimeStampBetweenOrderByTimeStampAsc(Collection<Integer> drinkerIds, Instant start, Instant end);
}
