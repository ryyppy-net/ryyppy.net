package drinkcounter.web.controllers.api.v2;

import drinkcounter.alcoholcalculator.BloodAlcohol;
import drinkcounter.model.Drink;
import drinkcounter.model.User;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PromilleHistoryTest {

    private static final long WINDOW_MILLIS = Duration.ofMinutes(300).toMillis();
    private static final long TOLERANCE_MILLIS = 5000;

    @Test
    public void forUserCoversLast300MinutesAtOneMinuteResolution() {
        User user = new User();
        user.setId(1);
        user.setWeight(80);
        user.setSex(User.Sex.MALE);

        Drink drink = new Drink();
        drink.setTimeStamp(Instant.now().minus(Duration.ofMinutes(200)));
        user.drink(drink);

        long before = System.currentTimeMillis();
        BloodAlcohol bloodAlcohol = new BloodAlcohol(user, user.getDrinks(), Instant.now());
        List<HistoryPoint> slopes = PromilleHistory.of(bloodAlcohol);
        long after = System.currentTimeMillis();

        assertFalse(slopes.isEmpty());

        long firstTimestamp = slopes.get(0).getTimestamp();
        assertTrue(Math.abs(firstTimestamp - (before - WINDOW_MILLIS)) < TOLERANCE_MILLIS,
                "first point should be ~300 minutes before now, was " + firstTimestamp);

        HistoryPoint last = slopes.get(slopes.size() - 1);
        assertTrue(last.getTimestamp() >= before - TOLERANCE_MILLIS && last.getTimestamp() <= after + TOLERANCE_MILLIS,
                "last point should be ~now, was " + last.getTimestamp());
        assertEquals(bloodAlcohol.promilles(), (float) last.getPromilles(), 0.01f);

        long previousTimestamp = firstTimestamp;
        for (HistoryPoint point : slopes) {
            assertTrue(point.getTimestamp() >= previousTimestamp, "timestamps must be non-decreasing");
            previousTimestamp = point.getTimestamp();
        }
    }

    @Test
    public void straightLinesBetweenThePointsMatchEveryMinute() {
        User user = new User();
        user.setId(2);
        user.setWeight(70);
        user.setSex(User.Sex.FEMALE);
        // Sober again well before the drinks two hours ago; some drinks fall mid-minute.
        for (int secondsAgo : new int[] {290 * 60, 250 * 60 + 30, 120 * 60 + 30, 119 * 60, 30 * 60 + 15}) {
            Drink drink = new Drink();
            drink.setTimeStamp(Instant.now().minusSeconds(secondsAgo));
            user.drink(drink);
        }

        BloodAlcohol bloodAlcohol = new BloodAlcohol(user, user.getDrinks(), Instant.now());
        List<HistoryPoint> points = PromilleHistory.of(bloodAlcohol);
        long start = points.get(0).getTimestamp();
        List<Float> everyMinute = bloodAlcohol.promillesAt(
                Instant.ofEpochMilli(start), Instant.ofEpochMilli(start + WINDOW_MILLIS), Duration.ofMinutes(1));

        assertTrue(points.size() < everyMinute.size() / 5, "only bends are sent, got " + points.size());
        int segment = 0;
        for (int minute = 0; minute < everyMinute.size(); minute++) {
            long x = start + minute * 60_000L;
            while (points.get(segment + 1).getTimestamp() < x) {
                segment++;
            }
            HistoryPoint from = points.get(segment);
            HistoryPoint to = points.get(segment + 1);
            double drawn = from.getPromilles() + (to.getPromilles() - from.getPromilles())
                    * (x - from.getTimestamp()) / (double) (to.getTimestamp() - from.getTimestamp());
            assertEquals(everyMinute.get(minute), drawn, 0.001, "minute " + minute);
        }
    }
}
