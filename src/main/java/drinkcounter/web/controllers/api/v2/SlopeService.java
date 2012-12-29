/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.web.controllers.api.v2;

import drinkcounter.model.User;
import java.util.LinkedList;
import java.util.List;
import org.joda.time.DateTime;

/**
 *
 * @author thardas
 */
public class SlopeService {
    public static List<String[]> getSlopes(User user, boolean getId) {
        int intervalMs = 60 * 1000;
        DateTime now = new DateTime();
        DateTime start = now.minusMinutes(300);

        List<Float> history = user.getPromillesAtInterval(start.toDate(), now.toDate(), intervalMs);
        List<String[]> slopes = new LinkedList<String[]>();

        double lastSlope = Double.MAX_VALUE;
        Long lastX = null;
        Float lastY = null;
        long lastInserted = 0;
        
        Long x = start.getMillis();
        for (Float y : history) {
            double slope = y / (x / 31536000000L);
            if (Math.abs(slope - lastSlope) >= 0.000000001) {
                if (lastX != null && lastY != null && lastInserted != lastX) {
                    slopes.add(getCsvValues(lastX, lastY, user, getId));
                }
                slopes.add(getCsvValues(x, y, user, getId));
                lastInserted = x;
            }
            lastSlope = slope;
            lastX = x;
            lastY = y;
            x += intervalMs;
        }
        slopes.add(getCsvValues(new DateTime().getMillis(), user.getPromilles(), user, getId));
        return slopes;
    }

    private static String[] getCsvValues(long x, float y, User user, boolean getId) {
        if (getId)
            return new String[]{Integer.toString(user.getId()), Long.toString(x), Float.toString(y)};
        return new String[]{Long.toString(x), Float.toString(y)};
    }
}
