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

    /** Oldest first. */
    @Query("select d from Drink d where d.drinker.id in :drinkerIds and d.timeStamp > :since order by d.timeStamp")
    List<Drink> findByDrinkersSince(@Param("drinkerIds") Collection<Integer> drinkerIds, @Param("since") Instant since);
}
