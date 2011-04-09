/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter;

import drinkcounter.model.Drink;
import drinkcounter.model.User;
import drinkcounter.model.Party;
import java.util.List;

/**
 *
 * @author Toni
 */
public interface DrinkCounterService {

    // Party methods
    Party startParty(String name);
    void updateParty(Party party);
    List<Party> listParties();
    Party getParty(int partyId);
    void linkUserToParty(int userId, int partyId);
    void unlinkUserFromParty(int userId, int partyId);
    List<User> listUsersByParty(int partyId);

    // Drinks
    List<Drink> getDrinks(int userId);
    void addDrink(int userId);
}
