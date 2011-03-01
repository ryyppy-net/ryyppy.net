/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter.model;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

/**
 *
 * @author Toni
 */
@Entity
public class Party extends AbstractEntity{
    private List<Participant> participants;
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Transient
    public String getName(){
        return getId();
    }

    @OneToMany(mappedBy="party")
    public List<Participant> getParticipants() {
        if(participants ==null){
            participants =  new ArrayList<Participant>();
        }
        return participants;
    }

    public void setParticipants(List<Participant> participants) {
        this.participants = participants;
    }

    public void addParticipant(Participant participant){
        getParticipants().add(participant);
        participant.setParty(this);
    }
}
