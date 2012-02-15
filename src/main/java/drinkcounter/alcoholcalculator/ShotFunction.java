package drinkcounter.alcoholcalculator;

import java.util.Date;

/**
 *
 * @author murgo
 */
public class ShotFunction {
    private double burnRate;
    private double startAlcoholAmount;
    private double cutter;
    private long timeTaken;

    public ShotFunction(Date timeTaken, double burnRate, double startAlcoholAmount)
    {
        this.burnRate = burnRate;
        this.startAlcoholAmount = startAlcoholAmount;
        this.timeTaken = timeTaken.getTime();
    }

    public double getCutter() { return cutter; }
    public void setCutter(double c) { cutter = c; }

    public double calc(long milliseconds)
    {
        double result = (burnRate * (double)(milliseconds - timeTaken)) + startAlcoholAmount ;

        if (result > startAlcoholAmount)
            return 0;

        if (result <= cutter)
            return 0;

        return result;
    }

    public double calc(Date t)
    {
        return calc(t.getTime());
    }
}
