package drinkcounter.web.controllers.api.v2;

import drinkcounter.AlcoholServiceImpl;
import drinkcounter.model.Drink;
import drinkcounter.model.User;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SlopeServiceTest {

    private static final long WINDOW_MILLIS = Duration.ofMinutes(300).toMillis();
    private static final long TOLERANCE_MILLIS = 5000;

    @BeforeEach
    public void setUp() {
        AlcoholServiceImpl.getInstance().reset();
    }

    @Test
    public void getSlopesCoversLast300MinutesAtOneMinuteResolution() {
        User user = new User();
        user.setId(1);
        user.setWeight(80);
        user.setSex(User.Sex.MALE);

        Drink drink = new Drink();
        drink.setTimeStamp(Instant.now().minus(Duration.ofMinutes(200)));
        user.drink(drink);

        long before = System.currentTimeMillis();
        List<HistoryPoint> slopes = SlopeService.getSlopes(user);
        long after = System.currentTimeMillis();

        assertFalse(slopes.isEmpty());

        long firstTimestamp = slopes.get(0).getTimestamp();
        assertTrue(Math.abs(firstTimestamp - (before - WINDOW_MILLIS)) < TOLERANCE_MILLIS,
                "first point should be ~300 minutes before now, was " + firstTimestamp);

        HistoryPoint last = slopes.get(slopes.size() - 1);
        assertTrue(last.getTimestamp() >= before - TOLERANCE_MILLIS && last.getTimestamp() <= after + TOLERANCE_MILLIS,
                "last point should be ~now, was " + last.getTimestamp());
        assertEquals(user.getPromilles(), (float) last.getPromilles(), 0.01f);

        long previousTimestamp = firstTimestamp;
        for (HistoryPoint point : slopes) {
            assertTrue(point.getTimestamp() >= previousTimestamp, "timestamps must be non-decreasing");
            previousTimestamp = point.getTimestamp();
        }
    }
}
