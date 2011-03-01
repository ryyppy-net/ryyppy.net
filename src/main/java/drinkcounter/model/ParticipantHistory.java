/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

/**
 *
 * @author paloniemit
 */
@Entity
public class ParticipantHistory extends AbstractEntity{
    private String participantName;
    private float alcoholLevel;
    private int totalDrinks;
    private PartyHistory partySnapshot;

    public float getAlcoholLevel() {
        return alcoholLevel;
    }

    public void setAlcoholLevel(float alcoholLevel) {
        this.alcoholLevel = alcoholLevel;
    }

    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    public PartyHistory getPartySnapshot() {
        return partySnapshot;
    }

    public void setPartySnapshot(PartyHistory partySnapshot) {
        this.partySnapshot = partySnapshot;
    }

    public void setTotalDrinks(Integer totalDrinks){
        this.totalDrinks = totalDrinks != null ? totalDrinks : 0;
    }

    public Integer getTotalDrinks(){
        return totalDrinks;
    }
}
