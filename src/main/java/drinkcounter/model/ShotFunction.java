package drinkcounter.model;

import java.util.Date;

/**
 *
 * @author murgo
 */
public class ShotFunction {
    private double a;
    private double b;
    private double increment;
    private double cutter;

    public ShotFunction(Date timeTaken, double burnRate, double i)
    {
        a = burnRate;
        increment = i;

        b = -burnRate * timeTaken.getTime() + increment;
    }

    public double getCutter() { return cutter; }
    public void setCutter(double c) { cutter = c; }

    public double calc(long milliseconds)
    {
        double result = a * milliseconds + b;

        if (result > increment)
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
