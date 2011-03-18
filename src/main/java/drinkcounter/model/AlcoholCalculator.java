/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

// probably the wrong package for this, feel free to move
package drinkcounter.model;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author murgo
 */
public class AlcoholCalculator {
    // The rate at which alcohol is burned per millisecond
    private double burnRate;
    
    // The amount of a single dosage of alcohol
    public final static double STANDARD_DRINK_ALCOHOL_GRAMS = 12;

    private final List<ShotFunction> functions = new LinkedList<ShotFunction>();

    private ShotFunction lastAddedFunction = null;

    AlcoholCalculator(float weight) {
        calculateBurnRate(weight);
    }
    
    public void calculateBurnRate(float weight) {
        burnRate = -((double)weight / 10.0 / 60 / 60 / 1000);
    }
    
    public void reset() {
        functions.clear();
        lastAddedFunction = null;
    }
    
    /**
     * 
     * @param time
     * @return grams of alcohol in blood at given time
     */
    public float getAlcoholAmountAt(Date time) {
        if (functions == null) throw new IllegalStateException("calculate() must be called before getAlcoholAmountAt()");
        
        return (float)calculate(time);
    }
    
    public void calculateDrink(Date stamp) {
        double p = calculate(stamp);
        
        // If there is still alcohol to burn, then the last function will be "disabled" with a cutter
        if (p > 0 && lastAddedFunction != null) {
            lastAddedFunction.setCutter(p);
        }

        ShotFunction newFunction = new ShotFunction(stamp, burnRate, p + STANDARD_DRINK_ALCOHOL_GRAMS);
        functions.add(newFunction);
        lastAddedFunction = newFunction;
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
}
