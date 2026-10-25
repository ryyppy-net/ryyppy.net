package drinkcounter.alcoholcalculator;

import drinkcounter.model.Drink;
import drinkcounter.model.User;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

/**
 * A user's blood alcohol at a moment, worked out from their drinks before it.
 */
public class BloodAlcohol {

    private final User user;
    private final List<Drink> drinks;
    private final Instant now;
    private final AlcoholCalculator calculator;

    /** @param drinks the user's drinks, oldest first */
    public BloodAlcohol(User user, List<Drink> drinks, Instant now) {
        this.user = user;
        this.drinks = drinks;
        this.now = now;
        calculator = new AlcoholCalculator(user.getWeight());
        for (Drink drink : drinks) {
            calculator.calculateDrink(drink);
        }
    }

    public float promilles() {
        return toPromilles(grams());
    }

    /** Grams of alcohol in the blood. */
    public float grams() {
        return calculator.getAlcoholAmountAt(Date.from(now));
    }

    public List<Float> promillesAtInterval(Date start, Date end, int intervalMs) {
        List<Float> list = new ArrayList<Float>();
        for (long i = start.getTime(); i <= end.getTime(); i += intervalMs) {
            list.add(toPromilles(calculator.getAlcoholAmountAt(new Date(i))));
        }
        return list;
    }

    /** Standard drinks taken since the user was last sober. */
    public int drinksSinceSober() {
        if (drinks.isEmpty()) return 0;
        return drinksSince(lastSober());
    }

    /** Accurate to 15 minutes. */
    private Date lastSober() {
        final int maxMinutes = 10080;
        final int interval = 15;

        long time = now.toEpochMilli();

        for (int i = 0; i < maxMinutes; i += interval) {
            float a = calculator.getAlcoholAmountAt(new Date(time));
            if (a < 0.01) {
                break;
            }
            time -= interval * 60 * 1000;
        }
        return new Date(time);
    }

    private int drinksSince(Date date) {
        float shots = 0f;
        ListIterator<Drink> iterator = drinks.listIterator(drinks.size());
        while (iterator.hasPrevious()) {
            Drink drink = iterator.previous();
            if (drink.getTimeStamp().isBefore(date.toInstant()))
                break;
            shots += drink.getAmountOfShots();
        }
        return Math.round(shots);
    }

    private float toPromilles(float grams) {
        return grams / (user.getSex().factor * user.getWeight());
    }
}
