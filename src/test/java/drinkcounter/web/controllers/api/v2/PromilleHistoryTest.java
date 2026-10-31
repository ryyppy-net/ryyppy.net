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

        HistoryPoint last = lastNotProjected(slopes);
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
        long end = points.get(points.size() - 1).getTimestamp();
        List<Float> everyMinute = bloodAlcohol.promillesAt(
                Instant.ofEpochMilli(start), Instant.ofEpochMilli(end), Duration.ofMinutes(1));

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

    @Test
    public void drinkingUserEndsWithProjectedZeroAtTheEstimatedSoberTime() {
        User user = user(3, 80);
        drinkAt(user, Instant.now().minus(Duration.ofMinutes(30)));

        Instant now = Instant.now();
        BloodAlcohol bloodAlcohol = new BloodAlcohol(user, user.getDrinks(), now);
        List<HistoryPoint> points = PromilleHistory.of(bloodAlcohol);

        HistoryPoint last = points.get(points.size() - 1);
        assertTrue(last.isProjected());
        assertEquals(0.0, last.getPromilles(), 0.0001);
        // Burn rate is weight / 10 grams per hour; the body holds sex factor * weight grams per promille.
        double hoursToSober = bloodAlcohol.grams() / (user.getWeight() / 10.0);
        long expected = now.toEpochMilli() + Math.round(hoursToSober * 3_600_000);
        assertEquals(expected, last.getTimestamp(), 60_000);
        assertFalse(lastNotProjected(points).isProjected());
        assertEquals(now.toEpochMilli(), lastNotProjected(points).getTimestamp(), 1);
    }

    @Test
    public void soberUserHasNoProjectedPoints() {
        User user = user(4, 80);
        drinkAt(user, Instant.now().minus(Duration.ofHours(20)));

        List<HistoryPoint> points = PromilleHistory.of(new BloodAlcohol(user, user.getDrinks(), Instant.now()));

        assertTrue(points.stream().noneMatch(HistoryPoint::isProjected));
    }

    @Test
    public void nowPointIsKeptWhenThePromilleLineIsStraightAroundNow() {
        User user = user(5, 80);
        drinkAt(user, Instant.now().minus(Duration.ofMinutes(60)));

        Instant now = Instant.now();
        List<HistoryPoint> points = PromilleHistory.of(new BloodAlcohol(user, user.getDrinks(), now));

        HistoryPoint nowPoint = lastNotProjected(points);
        assertEquals(now.toEpochMilli(), nowPoint.getTimestamp());
        assertTrue(points.get(points.size() - 1).isProjected());
    }

    @Test
    public void duringRunsFromTheStartToNowWithNothingProjected() {
        User user = user(6, 80);
        Instant now = Instant.parse("2024-03-05T22:00:30Z");
        Instant start = now.minus(Duration.ofHours(3));
        drinkAt(user, now.minus(Duration.ofMinutes(20)));

        List<HistoryPoint> points = PromilleHistory.during(new BloodAlcohol(user, user.getDrinks(), now), start);

        assertEquals(start.toEpochMilli(), points.get(0).getTimestamp());
        assertEquals(0.0, points.get(0).getPromilles(), 0.0001);
        assertEquals(now.toEpochMilli(), points.get(points.size() - 1).getTimestamp());
        assertTrue(points.get(points.size() - 1).getPromilles() > 0);
        assertTrue(points.stream().noneMatch(HistoryPoint::isProjected));
    }

    @Test
    public void duringALongPartyKeepsTheSamplesFewAndTheLineTrue() {
        User user = user(7, 80);
        Instant now = Instant.parse("2024-03-12T22:00:00Z");
        Instant start = now.minus(Duration.ofDays(7));
        for (int hoursAgo : new int[] {150, 149, 100, 30, 2}) {
            drinkAt(user, now.minus(Duration.ofHours(hoursAgo)));
        }
        BloodAlcohol bloodAlcohol = new BloodAlcohol(user, user.getDrinks(), now);

        List<HistoryPoint> points = PromilleHistory.during(bloodAlcohol, start);

        assertTrue(points.size() < 40, "only bends are sent, got " + points.size());
        assertEquals(start.toEpochMilli(), points.get(0).getTimestamp());
        assertEquals(now.toEpochMilli(), points.get(points.size() - 1).getTimestamp());
        assertEquals(bloodAlcohol.promilles(), (float) points.get(points.size() - 1).getPromilles(), 0.0001f);
        // 4 g left of the first drink when the second comes; 80 kg burns 8 g an hour, and seven
        // days in at most 1440 samples is a sample every 7 minutes, so the peak is seen within one.
        double peak = points.stream().mapToDouble(HistoryPoint::getPromilles).max().orElseThrow();
        double gramsPerPromille = User.Sex.MALE.factor * 80;
        assertEquals(16 / gramsPerPromille, peak, 8 * 7 / 60.0 / gramsPerPromille);
    }

    @Test
    public void duringAPartyThatJustStartedIsOnePoint() {
        Instant now = Instant.parse("2024-03-05T22:00:00Z");

        List<HistoryPoint> points = PromilleHistory.during(new BloodAlcohol(user(8, 80), List.of(), now), now);

        assertEquals(1, points.size());
        assertEquals(now.toEpochMilli(), points.get(0).getTimestamp());
    }

    private static HistoryPoint lastNotProjected(List<HistoryPoint> points) {
        return points.stream().filter(p -> !p.isProjected()).reduce((a, b) -> b).orElseThrow();
    }

    private static User user(int id, int weight) {
        User user = new User();
        user.setId(id);
        user.setWeight(weight);
        user.setSex(User.Sex.MALE);
        return user;
    }

    private static void drinkAt(User user, Instant time) {
        Drink drink = new Drink();
        drink.setTimeStamp(time);
        user.drink(drink);
    }
}
