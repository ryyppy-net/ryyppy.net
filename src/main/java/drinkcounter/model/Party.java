package drinkcounter.model;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 *
 * @author Toni
 */
@Entity
public class Party extends AbstractEntity{
    private Set<User> participants;
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

    // A user's party list shows the participants of every party.
    @ManyToMany
    @BatchSize(size = 100)
    @JoinTable(name="participants", 
            joinColumns={@JoinColumn(name="party_id")}
            ,inverseJoinColumns={@JoinColumn(name="participant_id")}
            )
    @OrderBy("id")
    public Set<User> getParticipants() {
        if(participants == null){
            participants =  new LinkedHashSet<User>();
        }
        return participants;
    }

    public void setParticipants(Set<User> participants) {
        this.participants = participants;
    }

    public void addParticipant(User participant){
        getParticipants().add(participant);
    }

    public void removeParticipant(User toKick) {
        getParticipants().remove(toKick);
    }
}
