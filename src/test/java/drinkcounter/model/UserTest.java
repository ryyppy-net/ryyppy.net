package drinkcounter.model;

import drinkcounter.AlcoholServiceImpl;
import drinkcounter.alcoholcalculator.AlcoholCalculator;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class UserTest {
    private User man80 = new User();
    private User man110 = new User();
    private User woman60 = new User();
    private User woman90 = new User();

    private static final float TOLERANCE = 0.05f;

    @Before
    public void setUp () {
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

        AlcoholServiceImpl.getInstance().reset();
    }

    // Basic tests
    @Test
    public void testBloodAlcohol() {
        User user = new User();
        user.setId(5);
        Drink drink = new Drink();
        user.drink(drink);
        assertEquals((float)AlcoholCalculator.STANDARD_DRINK_ALCOHOL_GRAMS, user.getBloodAlcoholGrams(), 0.1f);
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
        assertEquals(MAN_80_DRINK_COURSE_1, man80.getPromilles(), TOLERANCE);
    }
    
    @Test
    public void testPromilles_man110_drinkCourse1() {
        drinkCourse1(man110);
        assertEquals(MAN_110_DRINK_COURSE_1, man110.getPromilles(), TOLERANCE);
    }

    @Test
    public void testPromilles_woman60_drinkCourse1() {
        drinkCourse1(woman60);
        assertEquals(WOMAN_60_DRINK_COURSE_1, woman60.getPromilles(), TOLERANCE);
    }
    
    @Test
    public void testPromilles_woman90_drinkCourse1() {
        drinkCourse1(woman90);
        assertEquals(WOMAN_90_DRINK_COURSE_1, woman90.getPromilles(), TOLERANCE);
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
        assertEquals(MAN_80_DRINK_COURSE_2, man80.getPromilles(), TOLERANCE);
    }
    
    @Test
    public void testPromilles_man110_drinkCourse2() {
        drinkCourse2(man110);
        assertEquals(MAN_110_DRINK_COURSE_2, man110.getPromilles(), TOLERANCE);
    }

    @Test
    public void testPromilles_woman60_drinkCourse2() {
        drinkCourse2(woman60);
        assertEquals(WOMAN_60_DRINK_COURSE_2, woman60.getPromilles(), TOLERANCE);
    }
    
    @Test
    public void testPromilles_woman90_drinkCourse2() {
        drinkCourse2(woman90);
        assertEquals(WOMAN_90_DRINK_COURSE_2, woman90.getPromilles(), TOLERANCE);
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
        assertEquals(MAN_80_DRINK_COURSE_3, man80.getPromilles(), TOLERANCE);
    }
    
    @Test
    public void testPromilles_man110_drinkCourse3() {
        drinkCourse3(man110);
        assertEquals(MAN_110_DRINK_COURSE_3, man110.getPromilles(), TOLERANCE);
    }

    @Test
    public void testPromilles_woman60_drinkCourse3() {
        drinkCourse3(woman60);
        assertEquals(WOMAN_60_DRINK_COURSE_3, woman60.getPromilles(), TOLERANCE);
    }
    
    @Test
    public void testPromilles_woman90_drinkCourse3() {
        drinkCourse3(woman90);
        assertEquals(WOMAN_90_DRINK_COURSE_3, woman90.getPromilles(), TOLERANCE);
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
        assertEquals(MAN_80_DRINK_COURSE_4, man80.getPromilles(), TOLERANCE);
    }
    
    @Test
    public void testPromilles_man110_drinkCourse4() {
        drinkCourse4(man110);
        assertEquals(MAN_110_DRINK_COURSE_4, man110.getPromilles(), TOLERANCE);
    }

    @Test
    public void testPromilles_woman60_drinkCourse4() {
        drinkCourse4(woman60);
        assertEquals(WOMAN_60_DRINK_COURSE_4, woman60.getPromilles(), TOLERANCE);
    }
    
    @Test
    public void testPromilles_woman90_drinkCourse4() {
        drinkCourse4(woman90);
        assertEquals(WOMAN_90_DRINK_COURSE_4, woman90.getPromilles(), TOLERANCE);
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
        assertEquals(MAN_80_DRINK_COURSE_5, man80.getPromilles(), TOLERANCE);
    }
    
    @Test
    public void testPromilles_man110_drinkCourse5() {
        drinkCourse5(man110);
        assertEquals(MAN_110_DRINK_COURSE_5, man110.getPromilles(), TOLERANCE);
    }

    @Test
    public void testPromilles_woman60_drinkCourse5() {
        drinkCourse5(woman60);
        assertEquals(WOMAN_60_DRINK_COURSE_5, woman60.getPromilles(), TOLERANCE);
    }
    
    @Test
    public void testPromilles_woman90_drinkCourse5() {
        drinkCourse5(woman90);
        assertEquals(WOMAN_90_DRINK_COURSE_5, woman90.getPromilles(), TOLERANCE);
    }
    
    // Helper functions
    public void drinkXDrinksNMinutesAgo(User user, int drinks, int minutes) {
        for (int i = 0; i < drinks; i++) {
            Drink drink = new Drink();
            drink.setTimeStamp(new DateTime().minusMinutes(minutes).toDate());
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

        DateTime now = new DateTime();
        DateTime end = now.plusHours(24);

        List<Float> user1promilles = user.getPromillesAtInterval(now.toDate(), end.toDate(), 10000);
        List<Float> user2promilles = user2.getPromillesAtInterval(now.toDate(), end.toDate(), 10000);

        int len1 = user1promilles.size();

        for (int i = 0; i < len1; i++) {
            float p1 = user1promilles.get(i);
            float p2 = user2promilles.get(i);
            assertEquals(p1, p2, 0.01);
        }
    }
}
