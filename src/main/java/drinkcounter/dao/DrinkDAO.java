package drinkcounter.dao;

import drinkcounter.model.Drink;
import drinkcounter.model.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 *
 * @author Toni
 */
public interface DrinkDAO extends CrudRepository<Drink, Integer> {
    List<Drink> findByDrinker(User drinker);
}
