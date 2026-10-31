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

    public static List<HistoryPoint> of(BloodAlcohol bloodAlcohol) {
        int intervalMs = 60 * 1000;
        Instant start = bloodAlcohol.now().minus(Duration.ofMinutes(300));

        List<Float> history = bloodAlcohol.promillesAt(start, bloodAlcohol.now(), Duration.ofMillis(intervalMs));
        List<HistoryPoint> slopes = new ArrayList<HistoryPoint>();

        // Promilles fall in straight lines between drinks and the charts draw straight
        // lines between points, so only the minutes where the line bends are sent.
        long x = start.toEpochMilli();
        for (int i = 0; i < history.size(); i++, x += intervalMs) {
            boolean bends = i == 0
                    || i == history.size() - 1
                    || Math.abs(history.get(i + 1) - 2 * history.get(i) + history.get(i - 1)) > BEND_TOLERANCE;
            if (bends) {
                HistoryPoint point = new HistoryPoint();
                point.setTimestamp(x);
                point.setPromilles(history.get(i));
                slopes.add(point);
            }
        }
        return slopes;
    }
}
