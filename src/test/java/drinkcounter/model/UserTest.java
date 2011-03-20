/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter.model;

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
        user.drink();
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
        user.drink();
        user.drink();
        user.drink();
        user.drink();
        user.drink();

        assertEquals(1.143, user.getPromilles(), 0.01);
    }
}
