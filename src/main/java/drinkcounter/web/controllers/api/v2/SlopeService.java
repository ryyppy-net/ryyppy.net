/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.web.controllers.api.v2;

import drinkcounter.model.User;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author thardas
 */
public class SlopeService {
    public static List<HistoryPoint> getSlopes(User user) {
        int intervalMs = 60 * 1000;
        Instant now = Instant.now();
        Instant start = now.minus(Duration.ofMinutes(300));

        List<Float> history = user.getPromillesAtInterval(Date.from(start), Date.from(now), intervalMs);
        List<HistoryPoint> slopes = new LinkedList<HistoryPoint>();

        double lastSlope = Double.MAX_VALUE;
        Long lastX = null;
        Float lastY = null;
        long lastInserted = 0;

        Long x = start.toEpochMilli();
        for (Float y : history) {
            double slope = y / (x / 31536000000L);
            if (Math.abs(slope - lastSlope) >= 0.000000001) {
                if (lastX != null && lastY != null && lastInserted != lastX) {
                    HistoryPoint point = new HistoryPoint();
                    point.setTimestamp(lastX);
                    point.setPromilles(lastY);
                    slopes.add(point);
                }
                
                HistoryPoint point = new HistoryPoint();
                point.setTimestamp(x);
                point.setPromilles(y);
                slopes.add(point);
                lastInserted = x;
            }
            lastSlope = slope;
            lastX = x;
            lastY = y;
            x += intervalMs;
        }
        
        HistoryPoint point = new HistoryPoint();
        point.setTimestamp(Instant.now().toEpochMilli());
        point.setPromilles(user.getPromilles());
        slopes.add(point);
        return slopes;
    }
}
