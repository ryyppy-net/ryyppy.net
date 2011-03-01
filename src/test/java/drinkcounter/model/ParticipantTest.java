/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter.model;

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
        participant.drink();;
        assertEquals((float)Participant.STANDARD_DRINK_ALCOHOL_GRAMS, participant.getBloodAlcoholGrams(), 0.1f);
    }
}
