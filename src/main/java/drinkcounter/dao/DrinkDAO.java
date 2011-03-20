/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter.dao;

import drinkcounter.model.Drink;
import drinkcounter.model.User;
import java.util.List;
import org.synyx.hades.dao.GenericDao;

/**
 *
 * @author Toni
 */
public interface DrinkDAO extends GenericDao<Drink, Integer>{

    List<Drink> findByDrinker(User drinker);

}
