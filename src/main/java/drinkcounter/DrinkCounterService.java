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
    Party startParty(String identifier);
    void updateParty(Party party);
    List<Party> listParties();
    Party getParty(String identifier);
    void linkUserToParty(String userId, String partyIdentifier);
    void unlinkUserFromParty(String partyId, String toKick);
    List<User> listUsersByParty(String partyIdentifier);

    // Users
    List<User> listUsers();
    User addUser(User user);
    User getUser(String userid);
    User getUserByOpenId(String openId);
    void deleteUser(String userId);
    void addDrink(String userIdentifier);

    // Global
    List<Drink> getDrinks(String userIdentifier);
}
