/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter.dao;

import com.google.appengine.api.datastore.Key;
import drinkcounter.model.Drink;
import java.util.List;
import org.synyx.hades.dao.GenericDao;

/**
 *
 * @author Toni
 */
public interface DrinkDAO extends GenericDao<Drink, Key>{

    List<Drink> findByDrinker(Key drinkerKey);

}
