package drinkcounter.alcoholcalculator;

import drinkcounter.model.Drink;
import drinkcounter.model.User;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A user's blood alcohol at a moment, worked out from their drinks before it.
 */
public class BloodAlcohol {

    private static final Duration SOBER_STEP = Duration.ofMinutes(15);
    private static final int SOBER_STEPS = (int) (Duration.ofDays(7).toMinutes() / SOBER_STEP.toMinutes());

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

    public User user() {
        return user;
    }

    public Instant now() {
        return now;
    }

    public float promilles() {
        return toPromilles(grams());
    }

    /** Grams of alcohol in the blood. */
    public float grams() {
        return gramsAt(now);
    }

    public List<Float> promillesAt(Instant start, Instant end, Duration step) {
        List<Float> list = new ArrayList<Float>();
        for (Instant t = start; !t.isAfter(end); t = t.plus(step)) {
            list.add(toPromilles(gramsAt(t)));
        }
        return list;
    }

    /** Standard drinks taken since the user was last sober, which is looked for in 15-minute steps. */
    public int drinksSinceSober() {
        Instant sober = now;
        for (int step = 0; step < SOBER_STEPS && gramsAt(sober) >= 0.01; step++) {
            sober = sober.minus(SOBER_STEP);
        }
        float shots = 0f;
        for (int i = drinks.size() - 1; i >= 0 && !drinks.get(i).getTimeStamp().isBefore(sober); i--) {
            shots += drinks.get(i).getAmountOfShots();
        }
        return Math.round(shots);
    }

    /** Standard drinks in all the drinks this was worked out from. */
    public int standardDrinks() {
        float shots = 0f;
        for (Drink drink : drinks) {
            shots += drink.getAmountOfShots();
        }
        return Math.round(shots);
    }

    private float gramsAt(Instant time) {
        return calculator.getAlcoholAmountAt(Date.from(time));
    }

    private float toPromilles(float grams) {
        return grams / (user.getSex().factor * user.getWeight());
    }
}
