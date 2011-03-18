/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter.model;

import java.util.Date;
import org.joda.time.DateTime;
import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Toni
 */
public class ParticipantTest {
    @Test
    public void testBloodAlcohol(){
        Participant participant = new Participant();
        participant.drink();
        assertEquals((float)AlcoholCalculator.STANDARD_DRINK_ALCOHOL_GRAMS, participant.getBloodAlcoholGrams(), 0.1f);
    }
    
    @Test
    public void testPromilles() {
        Participant participant = new Participant();
        
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
        
        participant.setDrinks(al);
        assertEquals(0.49, participant.getPromilles(), 0.01);
    }
    
    @Test
    public void testDrinking() {
        Participant participant = new Participant();
        participant.drink();
        participant.drink();
        participant.drink();
        participant.drink();
        participant.drink();

        assertEquals(1.143, participant.getPromilles(), 0.01);
    }
}
