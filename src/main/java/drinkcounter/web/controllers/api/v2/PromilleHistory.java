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

    // A party tile's chart is a few hundred pixels wide, so a long party needs no finer samples.
    private static final int MAX_PARTY_SAMPLES = 1440;

    /**
     * The last 300 minutes up to now, then, for someone who is not sober, the minutes
     * until they are (at most 24 hours ahead) marked as projected.
     */
    public static List<HistoryPoint> of(BloodAlcohol bloodAlcohol) {
        Duration interval = Duration.ofMinutes(1);
        Instant now = bloodAlcohol.now();
        Instant start = now.minus(Duration.ofMinutes(300));

        List<Float> history = new ArrayList<Float>(bloodAlcohol.promillesAt(start, now, interval));
        List<Long> timestamps = new ArrayList<Long>();
        for (int i = 0; i < history.size(); i++) {
            timestamps.add(start.plus(interval.multipliedBy(i)).toEpochMilli());
        }
        int nowIndex = history.size() - 1;
        project(bloodAlcohol, timestamps, history);
        return bends(timestamps, history, nowIndex);
    }

    /**
     * From {@code start} up to the blood alcohol's now, sampled every minute, or for a party over
     * a day long, at most {@value #MAX_PARTY_SAMPLES} times. While the party runs, that is until
     * {@code end}, the minutes until the drinker is sober follow, projected as in {@link #of}.
     */
    public static List<HistoryPoint> during(BloodAlcohol bloodAlcohol, Instant start, Instant end) {
        Instant now = bloodAlcohol.now();
        long minutes = Duration.between(start, now).toMinutes();
        Duration interval = Duration.ofMinutes(Math.max(1, Math.ceilDiv(minutes, MAX_PARTY_SAMPLES)));

        List<Float> history = new ArrayList<Float>(bloodAlcohol.promillesAt(start, now, interval));
        List<Long> timestamps = new ArrayList<Long>();
        for (int i = 0; i < history.size(); i++) {
            timestamps.add(start.plus(interval.multipliedBy(i)).toEpochMilli());
        }
        if (timestamps.get(timestamps.size() - 1) < now.toEpochMilli()) {
            timestamps.add(now.toEpochMilli());
            history.add(bloodAlcohol.promilles());
        }
        int nowIndex = history.size() - 1;
        if (now.isBefore(end)) {
            project(bloodAlcohol, timestamps, history);
        }
        return bends(timestamps, history, nowIndex);
    }

    // The last sample is now; for someone not sober, the minutes after it until they are follow.
    private static void project(BloodAlcohol bloodAlcohol, List<Long> timestamps, List<Float> history) {
        if (history.get(history.size() - 1) <= 0) {
            return;
        }
        Duration interval = Duration.ofMinutes(1);
        Instant now = bloodAlcohol.now();
        Instant limit = now.plus(PROJECTION_LIMIT);
        for (Instant t = now.plus(interval); !t.isAfter(limit); t = t.plus(interval)) {
            float promilles = bloodAlcohol.promillesAt(t, t, interval).get(0);
            timestamps.add(t.toEpochMilli());
            history.add(promilles);
            if (promilles <= 0) {
                break;
            }
        }
    }

    // Promilles fall in straight lines between drinks and the charts draw straight lines
    // between points, so only the samples where the line bends are sent, plus the ends, now,
    // where the chart switches to the projection, and wherever the samples are unevenly spaced.
    private static List<HistoryPoint> bends(List<Long> timestamps, List<Float> history, int nowIndex) {
        List<HistoryPoint> slopes = new ArrayList<HistoryPoint>();
        int last = history.size() - 1;
        for (int i = 0; i <= last; i++) {
            boolean bends = i == 0 || i == nowIndex || i == last
                    || timestamps.get(i + 1) - timestamps.get(i) != timestamps.get(i) - timestamps.get(i - 1)
                    || Math.abs(history.get(i + 1) - 2 * history.get(i) + history.get(i - 1)) > BEND_TOLERANCE;
            if (bends) {
                HistoryPoint point = new HistoryPoint();
                point.setTimestamp(timestamps.get(i));
                point.setPromilles(history.get(i));
                point.setProjected(i > nowIndex);
                slopes.add(point);
            }
        }
        return slopes;
    }
}
