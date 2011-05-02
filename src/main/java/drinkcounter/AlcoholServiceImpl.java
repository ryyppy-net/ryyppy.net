package drinkcounter;

import drinkcounter.alcoholcalculator.AlcoholCalculator;
import drinkcounter.model.Drink;
import drinkcounter.model.User;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlcoholServiceImpl implements AlcoholService {

    private final Map<Integer, AlcoholCalculator> alcoholCalculators = new HashMap<Integer, AlcoholCalculator>();
    private final Object maplock = new Object();
    private final static AlcoholService instance = new AlcoholServiceImpl();

    private AlcoholServiceImpl() {

    }

    @Override
    public void initializeUser(User user) {
        if (user.getId() == null) return;
        AlcoholCalculator alc;
        synchronized(maplock) {
            if (!alcoholCalculators.containsKey(user.getId())) {
                alc = new AlcoholCalculator(user.getWeight());
                alcoholCalculators.put(user.getId(), alc);
            }

            alc = alcoholCalculators.get(user.getId());
        }

        alc.setWeight(user.getWeight());
        alc.reset();
        
        for (Drink drink : user.getDrinks()) {
            if (drink.getTimeStamp() == null) continue;
            alc.calculateDrink(drink);
        }
    }

    private AlcoholCalculator getOrInitializeAlcoholCalculator(User user) {
        synchronized(maplock) {
            if (!alcoholCalculators.containsKey(user.getId()))
                initializeUser(user);
            return alcoholCalculators.get(user.getId());
        }
    }

    @Override
    public void drinkAdded(User user, Drink drink) {
        AlcoholCalculator alc = getOrInitializeAlcoholCalculator(user);
        alc.calculateDrink(drink);
    }

    @Override
    public void drinkRemoved(User user, Drink drink) {
        initializeUser(user);
    }

    @Override
    public float getPromilles(User user) {
        AlcoholCalculator alc = getOrInitializeAlcoholCalculator(user);
        return alc.getAlcoholAmountAt(new Date()) / (user.getSex().factor * user.getWeight());
    }

    @Override
    public List<Float> getPromillesAtInterval(User user, Date start, Date end, int intervalMs) {
        AlcoholCalculator alc = getOrInitializeAlcoholCalculator(user);

        List<Float> list = new ArrayList<Float>();
        for (long i = start.getTime(); i  <= end.getTime(); i += intervalMs) {
            list.add(alc.getAlcoholAmountAt(new Date(i)) / (user.getSex().factor * user.getWeight()));
        }
        return list;
    }

    /**
     * When was the user sober last time? Accuracy is 15 minutes :P
     * This could be done in client side, if optimization is needed
     * @return Time when user was last sober
     */
    @Override
    public Date getTimeWhenUserLastSober(User user){
        AlcoholCalculator alc = getOrInitializeAlcoholCalculator(user);
        final int maxMinutes = 10080;
        final int interval = 15;

        long time = new Date().getTime();

        for (int i= 0; i < maxMinutes; i += interval) {
            float a = alc.getAlcoholAmountAt(new Date(time));
            if (a < 0.01) {
                break;
            }
            time -= interval * 60 * 1000;
        }
        return new Date(time);
    }

    /**
     * What is this i don't even.
     * As far as I understand, this doesn't return total drinks like the name implies
     * but how many drinks has the user drunk since he was last sober
     * @return
     */
    @Override
    public int getTotalDrinks(User user) {
        if (user.getDrinks().isEmpty()) return 0;
        Date soberTime = getTimeWhenUserLastSober(user);
        return user.drinksSince(soberTime);
    }

    public static AlcoholService getInstance() {
        return instance;
    }

    @Override
    public float getBloodAlcoholGrams(User user) {
        AlcoholCalculator alc = getOrInitializeAlcoholCalculator(user);
        return alc.getAlcoholAmountAt(new Date());
    }

    @Override
    public void reset() {
        alcoholCalculators.clear();
    }
}
