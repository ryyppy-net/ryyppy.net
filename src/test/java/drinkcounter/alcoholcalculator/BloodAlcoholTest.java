package drinkcounter.alcoholcalculator;

import static org.junit.jupiter.api.Assertions.*;

import drinkcounter.model.Drink;
import drinkcounter.model.User;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BloodAlcoholTest {
    private User man80 = new User();
    private User man110 = new User();
    private User woman60 = new User();
    private User woman90 = new User();

    private static final float TOLERANCE = 0.05f;

    @BeforeEach
    public void setUp() {
        man80.setWeight(80);
        man80.setSex(User.Sex.MALE);
        man80.setId(1);

        man110.setWeight(110);
        man110.setSex(User.Sex.MALE);
        man110.setId(2);

        woman60.setWeight(60);
        woman60.setSex(User.Sex.FEMALE);
        woman60.setId(3);

        woman90.setWeight(90);
        woman90.setSex(User.Sex.FEMALE);
        woman90.setId(4);
    }

    // Basic tests
    @Test
    public void testBloodAlcohol() {
        User user = new User();
        user.setId(5);
        Drink drink = new Drink();
        user.drink(drink);
        assertEquals(
                (float) AlcoholCalculator.STANDARD_DRINK_ALCOHOL_GRAMS,
                bloodAlcohol(user).grams(),
                0.1f);
    }

    // DRINK COURSES:

    // Drink course 1:
    // - One beer (0.33 ml, 4.7%) 30 minutes ago
    private static final float MAN_80_DRINK_COURSE_1 = 0.1f;
    private static final float MAN_110_DRINK_COURSE_1 = 0.1f;
    private static final float WOMAN_60_DRINK_COURSE_1 = 0.2f;
    private static final float WOMAN_90_DRINK_COURSE_1 = 0.1f;

    public void drinkCourse1(User user) {
        drinkXDrinksNMinutesAgo(user, 1, 30);
    }

    @Test
    public void testPromilles_man80_drinkCourse1() {
        drinkCourse1(man80);
        assertEquals(MAN_80_DRINK_COURSE_1, bloodAlcohol(man80).promilles(), TOLERANCE);
    }

    @Test
    public void testPromilles_man110_drinkCourse1() {
        drinkCourse1(man110);
        assertEquals(MAN_110_DRINK_COURSE_1, bloodAlcohol(man110).promilles(), TOLERANCE);
    }

    @Test
    public void testPromilles_woman60_drinkCourse1() {
        drinkCourse1(woman60);
        assertEquals(WOMAN_60_DRINK_COURSE_1, bloodAlcohol(woman60).promilles(), TOLERANCE);
    }

    @Test
    public void testPromilles_woman90_drinkCourse1() {
        drinkCourse1(woman90);
        assertEquals(WOMAN_90_DRINK_COURSE_1, bloodAlcohol(woman90).promilles(), TOLERANCE);
    }

    // Drink course 2:
    // - Three beers (0.33 ml, 4.7%) an hour ago
    private static final float MAN_80_DRINK_COURSE_2 = 0.5f;
    private static final float MAN_110_DRINK_COURSE_2 = 0.3f;
    private static final float WOMAN_60_DRINK_COURSE_2 = 0.8f;
    private static final float WOMAN_90_DRINK_COURSE_2 = 0.5f;

    public void drinkCourse2(User user) {
        drinkXDrinksNMinutesAgo(user, 3, 60);
    }

    @Test
    public void testPromilles_man80_drinkCourse2() {
        drinkCourse2(man80);
        assertEquals(MAN_80_DRINK_COURSE_2, bloodAlcohol(man80).promilles(), TOLERANCE);
    }

    @Test
    public void testPromilles_man110_drinkCourse2() {
        drinkCourse2(man110);
        assertEquals(MAN_110_DRINK_COURSE_2, bloodAlcohol(man110).promilles(), TOLERANCE);
    }

    @Test
    public void testPromilles_woman60_drinkCourse2() {
        drinkCourse2(woman60);
        assertEquals(WOMAN_60_DRINK_COURSE_2, bloodAlcohol(woman60).promilles(), TOLERANCE);
    }

    @Test
    public void testPromilles_woman90_drinkCourse2() {
        drinkCourse2(woman90);
        assertEquals(WOMAN_90_DRINK_COURSE_2, bloodAlcohol(woman90).promilles(), TOLERANCE);
    }

    // Drink course 3:
    // - Six beers (0.33 ml, 4.7%) three hours ago
    private static final float MAN_80_DRINK_COURSE_3 = 0.8f;
    private static final float MAN_110_DRINK_COURSE_3 = 0.5f;
    private static final float WOMAN_60_DRINK_COURSE_3 = 1.4f;
    private static final float WOMAN_90_DRINK_COURSE_3 = 0.8f;

    public void drinkCourse3(User user) {
        drinkXDrinksNMinutesAgo(user, 6, 180);
    }

    @Test
    public void testPromilles_man80_drinkCourse3() {
        drinkCourse3(man80);
        assertEquals(MAN_80_DRINK_COURSE_3, bloodAlcohol(man80).promilles(), TOLERANCE);
    }

    @Test
    public void testPromilles_man110_drinkCourse3() {
        drinkCourse3(man110);
        assertEquals(MAN_110_DRINK_COURSE_3, bloodAlcohol(man110).promilles(), TOLERANCE);
    }

    @Test
    public void testPromilles_woman60_drinkCourse3() {
        drinkCourse3(woman60);
        assertEquals(WOMAN_60_DRINK_COURSE_3, bloodAlcohol(woman60).promilles(), TOLERANCE);
    }

    @Test
    public void testPromilles_woman90_drinkCourse3() {
        drinkCourse3(woman90);
        assertEquals(WOMAN_90_DRINK_COURSE_3, bloodAlcohol(woman90).promilles(), TOLERANCE);
    }

    // Drink course 4:
    // - 24 beers (0.33 ml, 4.7%) 8 hours ago
    private static final float MAN_80_DRINK_COURSE_4 = 3.7f;
    private static final float MAN_110_DRINK_COURSE_4 = 2.4f;
    private static final float WOMAN_60_DRINK_COURSE_4 = 6.1f;
    private static final float WOMAN_90_DRINK_COURSE_4 = 3.6f;

    public void drinkCourse4(User user) {
        drinkXDrinksNMinutesAgo(user, 24, 8 * 60);
    }

    @Test
    public void testPromilles_man80_drinkCourse4() {
        drinkCourse4(man80);
        assertEquals(MAN_80_DRINK_COURSE_4, bloodAlcohol(man80).promilles(), TOLERANCE);
    }

    @Test
    public void testPromilles_man110_drinkCourse4() {
        drinkCourse4(man110);
        assertEquals(MAN_110_DRINK_COURSE_4, bloodAlcohol(man110).promilles(), TOLERANCE);
    }

    @Test
    public void testPromilles_woman60_drinkCourse4() {
        drinkCourse4(woman60);
        assertEquals(WOMAN_60_DRINK_COURSE_4, bloodAlcohol(woman60).promilles(), TOLERANCE);
    }

    @Test
    public void testPromilles_woman90_drinkCourse4() {
        drinkCourse4(woman90);
        assertEquals(WOMAN_90_DRINK_COURSE_4, bloodAlcohol(woman90).promilles(), TOLERANCE);
    }

    // Drink course 5:
    // - 24 beers (0.33 ml, 4.7%) 16 hours ago
    private static final float MAN_80_DRINK_COURSE_5 = 2.7f;
    private static final float MAN_110_DRINK_COURSE_5 = 1.4f;
    private static final float WOMAN_60_DRINK_COURSE_5 = 4.8f;
    private static final float WOMAN_90_DRINK_COURSE_5 = 2.4f;

    public void drinkCourse5(User user) {
        drinkXDrinksNMinutesAgo(user, 24, 16 * 60);
    }

    @Test
    public void testPromilles_man80_drinkCourse5() {
        drinkCourse5(man80);
        assertEquals(MAN_80_DRINK_COURSE_5, bloodAlcohol(man80).promilles(), TOLERANCE);
    }

    @Test
    public void testPromilles_man110_drinkCourse5() {
        drinkCourse5(man110);
        assertEquals(MAN_110_DRINK_COURSE_5, bloodAlcohol(man110).promilles(), TOLERANCE);
    }

    @Test
    public void testPromilles_woman60_drinkCourse5() {
        drinkCourse5(woman60);
        assertEquals(WOMAN_60_DRINK_COURSE_5, bloodAlcohol(woman60).promilles(), TOLERANCE);
    }

    @Test
    public void testPromilles_woman90_drinkCourse5() {
        drinkCourse5(woman90);
        assertEquals(WOMAN_90_DRINK_COURSE_5, bloodAlcohol(woman90).promilles(), TOLERANCE);
    }

    // Helper functions
    private static BloodAlcohol bloodAlcohol(User user) {
        return new BloodAlcohol(user, user.getDrinks(), Instant.now());
    }

    public void drinkXDrinksNMinutesAgo(User user, int drinks, int minutes) {
        for (int i = 0; i < drinks; i++) {
            Drink drink = new Drink();
            drink.setTimeStamp(Instant.now().minus(Duration.ofMinutes(minutes)));
            user.drink(drink);
        }
    }

    @Test
    public void testBurnrate() {
        User user = new User();
        user.setWeight(100.0f);
        user.setId(1);

        User user2 = new User();
        user2.setWeight(10.0f);
        user2.setId(2);

        for (int i = 0; i < 30; i++) {
            user.drink(new Drink());
        }

        user2.drink(new Drink());
        user2.drink(new Drink());
        user2.drink(new Drink());

        Instant now = Instant.now();
        Instant end = now.plus(Duration.ofHours(24));

        List<Float> user1promilles = bloodAlcohol(user).promillesAt(now, end, Duration.ofSeconds(10));
        List<Float> user2promilles = bloodAlcohol(user2).promillesAt(now, end, Duration.ofSeconds(10));

        int len1 = user1promilles.size();

        for (int i = 0; i < len1; i++) {
            float p1 = user1promilles.get(i);
            float p2 = user2promilles.get(i);
            assertEquals(p1, p2, 0.01);
        }
    }

    @Test
    public void drinksSinceSoberCountsOnlyTheCurrentSession() {
        Instant now = Instant.now();
        User user = new User();
        user.setWeight(80);
        drinkAt(user, now.minus(Duration.ofHours(30)));
        drinkAt(user, now.minus(Duration.ofHours(30)));
        drinkAt(user, now.minus(Duration.ofHours(2)));
        drinkAt(user, now.minus(Duration.ofHours(1)));

        assertEquals(2, new BloodAlcohol(user, user.getDrinks(), now).drinksSinceSober());
    }

    @Test
    public void withoutDrinksTheUserIsSober() {
        User user = new User();

        BloodAlcohol bloodAlcohol = new BloodAlcohol(user, List.of(), Instant.now());

        assertEquals(0f, bloodAlcohol.promilles());
        assertEquals(0, bloodAlcohol.drinksSinceSober());
    }

    private static void drinkAt(User user, Instant timeStamp) {
        Drink drink = new Drink();
        drink.setTimeStamp(timeStamp);
        user.drink(drink);
    }

    // An 80 kg man burns 8 g an hour and his promilles are grams / (0.75 * 80) = grams / 60.
    private static final Instant NOW = Instant.parse("2026-10-25T21:00:00Z");
    private static final float EXACT = 0.0001f;

    @Test
    public void aDrinkBurnsOffAtOneGramPerTenKilosAnHour() {
        BloodAlcohol bloodAlcohol = man80(drink(12, Duration.ofHours(-1)));

        assertEquals(4f, bloodAlcohol.grams(), EXACT);
        assertEquals(4f / 60, bloodAlcohol.promilles(), EXACT);
    }

    @Test
    public void burningIsSteadyThroughTheHour() {
        assertEquals(8f, man80(drink(12, Duration.ofMinutes(-30))).grams(), EXACT);
    }

    @Test
    public void aBurntOffDrinkLeavesNothing() {
        assertEquals(0f, man80(drink(12, Duration.ofHours(-2))).grams(), EXACT);
    }

    @Test
    public void drinksAddUp() {
        // 12 g burn to 4 g by the second drink, which brings it to 16 g, an hour before now.
        BloodAlcohol bloodAlcohol = man80(drink(12, Duration.ofHours(-2)), drink(12, Duration.ofHours(-1)));

        assertEquals(8f, bloodAlcohol.grams(), EXACT);
    }

    @Test
    public void soberTimeIsNotCreditedToTheNextDrink() {
        BloodAlcohol bloodAlcohol = man80(drink(12, Duration.ofHours(-10)), drink(12, Duration.ofHours(-1)));

        assertEquals(4f, bloodAlcohol.grams(), EXACT);
    }

    @Test
    public void aHeavierDrinkerBurnsFasterAndHasMoreBodyWater() {
        User man100 = user(100, User.Sex.MALE);

        BloodAlcohol bloodAlcohol = new BloodAlcohol(man100, List.of(drink(12, Duration.ofHours(-1))), NOW);

        assertEquals(2f, bloodAlcohol.grams(), EXACT);
        assertEquals(2f / 75, bloodAlcohol.promilles(), EXACT);
    }

    @Test
    public void aWomanBurnsAtTheSameRateButHasLessBodyWater() {
        User woman80 = user(80, User.Sex.FEMALE);

        BloodAlcohol bloodAlcohol = new BloodAlcohol(woman80, List.of(drink(12, Duration.ofHours(-1))), NOW);

        assertEquals(4f, bloodAlcohol.grams(), EXACT);
        assertEquals(4f / (0.66f * 80), bloodAlcohol.promilles(), EXACT);
    }

    @Test
    public void theCurveJumpsAtADrinkAndFallsInAStraightLine() {
        BloodAlcohol bloodAlcohol = man80(drink(12, Duration.ofHours(-1)));

        List<Float> curve = bloodAlcohol.promillesAt(
                NOW.minus(Duration.ofHours(2)), NOW.plus(Duration.ofHours(1)), Duration.ofMinutes(30));

        List<Float> expected = List.of(0f, 0f, 12f / 60, 8f / 60, 4f / 60, 0f, 0f);
        assertEquals(expected.size(), curve.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), curve.get(i), EXACT, "point " + i);
        }
    }

    @Test
    public void aDrinkTimedInTheFutureCountsFromItsTime() {
        BloodAlcohol bloodAlcohol = man80(drink(12, Duration.ofMinutes(30)));

        assertEquals(0f, bloodAlcohol.grams(), EXACT);
        List<Float> anHourFromNow = bloodAlcohol.promillesAt(
                NOW.plus(Duration.ofHours(1)), NOW.plus(Duration.ofHours(1)), Duration.ofMillis(1));
        assertEquals(8f / 60, anHourFromNow.get(0), EXACT);
    }

    @Test
    public void drinksSinceSoberCountsStandardDrinksOfTheCurrentSession() {
        BloodAlcohol bloodAlcohol = man80(
                drink(12, Duration.ofHours(-20)),
                drink(12, Duration.ofHours(-20)),
                drink(12, Duration.ofMinutes(-60)),
                drink(6, Duration.ofMinutes(-50)),
                drink(6, Duration.ofMinutes(-40)));

        assertEquals(2, bloodAlcohol.drinksSinceSober());
    }

    @Test
    public void drinksSinceSoberIsZeroOnceSober() {
        assertEquals(0, man80(drink(12, Duration.ofHours(-5))).drinksSinceSober());
    }

    private static BloodAlcohol man80(Drink... drinks) {
        return new BloodAlcohol(user(80, User.Sex.MALE), List.of(drinks), NOW);
    }

    private static User user(float weight, User.Sex sex) {
        User user = new User();
        user.setWeight(weight);
        user.setSex(sex);
        return user;
    }

    private static Drink drink(float grams, Duration fromNow) {
        Drink drink = new Drink();
        drink.setAlcohol(grams);
        drink.setTimeStamp(NOW.plus(fromNow));
        return drink;
    }
}
