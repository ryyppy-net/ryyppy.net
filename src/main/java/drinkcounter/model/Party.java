package drinkcounter.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 *
 * @author Toni
 */
@Entity
public class Party extends AbstractEntity{
    private List<User> participants;
    private String name;
    private Instant startTime;

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    // Keep the existing "timestamp without time zone" column instead of Hibernate's
    // default TIMESTAMP_UTC mapping for Instant, which would map to timestamptz.
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    public Instant getStartTime() {
        return this.startTime;
    }

    public String getName(){
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    @ManyToMany
    @JoinTable(name="participants", 
            joinColumns={@JoinColumn(name="party_id")}
            ,inverseJoinColumns={@JoinColumn(name="participant_id")}
            )
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
