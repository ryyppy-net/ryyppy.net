/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.web.controllers.api.v2;

import drinkcounter.alcoholcalculator.BloodAlcohol;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author thardas
 */
public class PromilleHistory {
    // In promilles: above float rounding, far below what a chart can show.
    private static final float BEND_TOLERANCE = 0.00001f;

    private static final Duration PROJECTION_LIMIT = Duration.ofHours(24);

    /**
     * The last 300 minutes up to now, then, for someone who is not sober, the minutes
     * until they are (at most 24 hours ahead) marked as projected.
     */
    public static List<HistoryPoint> of(BloodAlcohol bloodAlcohol) {
        Duration interval = Duration.ofMinutes(1);
        Instant now = bloodAlcohol.now();
        Instant start = now.minus(Duration.ofMinutes(300));

        List<Float> history = new ArrayList<Float>(bloodAlcohol.promillesAt(start, now, interval));
        int nowIndex = history.size() - 1;
        if (history.get(nowIndex) > 0) {
            Instant limit = now.plus(PROJECTION_LIMIT);
            for (Instant t = now.plus(interval); !t.isAfter(limit); t = t.plus(interval)) {
                float promilles = bloodAlcohol.promillesAt(t, t, interval).get(0);
                history.add(promilles);
                if (promilles <= 0) {
                    break;
                }
            }
        }

        List<HistoryPoint> slopes = new ArrayList<HistoryPoint>();

        // Promilles fall in straight lines between drinks and the charts draw straight
        // lines between points, so only the minutes where the line bends are sent,
        // plus the ends and now, where the chart switches to the projection.
        long x = start.toEpochMilli();
        for (int i = 0; i < history.size(); i++, x += interval.toMillis()) {
            boolean bends = i == 0 || i == nowIndex || i == history.size() - 1
                    || Math.abs(history.get(i + 1) - 2 * history.get(i) + history.get(i - 1)) > BEND_TOLERANCE;
            if (bends) {
                HistoryPoint point = new HistoryPoint();
                point.setTimestamp(x);
                point.setPromilles(history.get(i));
                point.setProjected(i > nowIndex);
                slopes.add(point);
            }
        }
        return slopes;
    }
}
