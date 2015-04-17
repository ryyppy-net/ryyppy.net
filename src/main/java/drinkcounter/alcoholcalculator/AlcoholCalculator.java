// probably the wrong package for this, feel free to move
package drinkcounter.alcoholcalculator;

import drinkcounter.model.Drink;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author murgo
 */
public class AlcoholCalculator {
    
    /**
     * Gram's per litre
     */
    public static final float ALCOHOL_DENSITY = 789f;
    
    // The rate at which alcohol is burned per millisecond
    private double burnRate;
    
    // The amount of a single dosage of alcohol
    public final static double STANDARD_DRINK_ALCOHOL_GRAMS = 12;

    private final List<ShotFunction> functions = new LinkedList<ShotFunction>();

    public AlcoholCalculator(float weight) {
        setWeight(weight);
    }
    
    public void setWeight(float weight) {
        synchronized(this) {
            // Typical male burns one gram of alcohol for every 10 kilograms of weight in an hour.
            burnRate = -((double)weight / 10.0 / 60 / 60 / 1000);
        }
    }
    
    public void reset() {
        synchronized(this) {
            functions.clear();
        }
    }
    
    /**
     * 
     * @param time
     * @return grams of alcohol in blood at given time
     */
    public float getAlcoholAmountAt(Date time) {
        synchronized(this) {
            return (float)calculate(time);
        }
    }
    
    public void calculateDrink(Drink drink) {
        synchronized(this) {
            double currentAlcohol = calculate(drink.getTimeStamp());

            // If there is still alcohol to burn, then the last function will be "disabled" with a cutter
            int size = functions.size();
            if (currentAlcohol > 0 && size > 0) {
                functions.get(size - 1).setCutter(currentAlcohol);
            }

            ShotFunction newFunction = new ShotFunction(drink.getTimeStamp(), burnRate, currentAlcohol + drink.getAlcohol());
            functions.add(newFunction);
        }
    }
    
    // best function name ever
    private double calculate(Date time)
    {
        double alcohol = 0;
        
        for (ShotFunction shotFunction : functions)
        {
            alcohol = shotFunction.calc(time);

            // If the function returned a positive value then we can stop, because the functions don't overlap (due to cutters)
            if (alcohol > 0)
                return alcohol;
        }

        return alcohol;
    }
    
    public static float getAlcoholAmount(float volume, float alcoholPercentage){
        return volume*alcoholPercentage*ALCOHOL_DENSITY;
    }
}
