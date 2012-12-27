package drinkcounter;

import drinkcounter.model.Drink;
import drinkcounter.model.User;
import drinkcounter.model.Party;
import java.util.Date;
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
    boolean isUserParticipant(int partyId, int userId);

    // Drinks
    List<Drink> getDrinks(int userId);
    int addDrink(int userId);
    int addDrink(int userId, float alcoholAmount);
    void removeDrinkFromUser(int userId, int drinkId);
    int addDrinkToDate(int id, String date, double timezoneOffset);
    int addDrink(int userId, Date date);
    void addDrink(int userId, Date date, Float alcoholAmount);
    long getTotalDrinkCount();

}
