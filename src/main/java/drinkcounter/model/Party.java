package drinkcounter.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Temporal;
import javax.persistence.Transient;

/**
 *
 * @author Toni
 */
@Entity
public class Party extends AbstractEntity{
    private List<User> participants;
    private String name;
    private Date startTime;

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getStartTime() {
        return this.startTime;
    }

    public String getName(){
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    @Transient
    public int getId(){
        return getStoreKey();
    }

    @ManyToMany
    @JoinTable(name="participants")
    public List<User> getParticipants() {
        if(participants == null){
            participants =  new ArrayList<User>();
        }
        return participants;
    }

    public void setParticipants(List<User> participants) {
        this.participants = participants;
    }

    public void addParticipant(User participant){
        getParticipants().add(participant);
    }

    public void removeParticipant(User toKick) {
        getParticipants().remove(toKick);
    }
}
