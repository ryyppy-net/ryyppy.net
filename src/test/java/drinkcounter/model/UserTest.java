/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter.model;

import java.util.List;
import org.joda.time.DateTime;
import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Toni
 */
public class UserTest {
    @Test
    public void testBloodAlcohol(){
        User user = new User();
        Drink drink = new Drink();
        user.drink(drink);
        assertEquals((float)AlcoholCalculator.STANDARD_DRINK_ALCOHOL_GRAMS, user.getBloodAlcoholGrams(), 0.1f);
    }
    
    @Test
    public void testPromilles() {
        User user = new User();
        
        Drink drink1 = new Drink();
        drink1.setTimeStamp(new DateTime().minusMinutes(90).toDate());
        Drink drink2 = new Drink();
        drink2.setTimeStamp(new DateTime().minusMinutes(60).toDate());
        Drink drink3 = new Drink();
        drink3.setTimeStamp(new DateTime().minusMinutes(30).toDate());

        ArrayList<Drink> al = new ArrayList<Drink>();
        al.add(drink1);
        al.add(drink2);
        al.add(drink3);
        
        user.setDrinks(al);
        assertEquals(0.49, user.getPromilles(), 0.01);
    }
    
    @Test
    public void testDrinking() {
        User user = new User();
        user.drink(new Drink());
        user.drink(new Drink());
        user.drink(new Drink());
        user.drink(new Drink());
        user.drink(new Drink());

        assertEquals(1.143, user.getPromilles(), 0.01);
    }

    @Test
    public void testBurnrate() {
        User user = new User();
        user.setWeight(100.0f);

        User user2 = new User();
        user.setWeight(10.0f);

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
        int len2 = user2promilles.size();

        for (int i = 0; i < len1; i++) {
            assertEquals(user1promilles.get(i), user2promilles.get(i), 0.01);
        }
    }
}
