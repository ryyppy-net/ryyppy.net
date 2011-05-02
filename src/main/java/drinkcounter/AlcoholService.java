package drinkcounter;

import drinkcounter.model.Drink;
import drinkcounter.model.User;
import java.util.Date;
import java.util.List;

public interface AlcoholService {
    void initializeUser(User user);
    void drinkAdded(User user, Drink drink);
    void drinkRemoved(User user, Drink drink);
    float getPromilles(User user);
    List<Float> getPromillesAtInterval(User user, Date start, Date end, int intervalMs);
    int getTotalDrinks(User user);
    Date getTimeWhenUserLastSober(User user);
    float getBloodAlcoholGrams(User user);
    void reset();
}
