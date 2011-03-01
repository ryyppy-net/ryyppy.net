/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author paloniemit
 */
@Entity
@NamedQueries({
    @NamedQuery(name="PartyHistory.findByPartyId", query="SELECT h FROM PartyHistory h WHERE h.partyId = ?1 ORDER BY h.snapshotTime ASC")
})
public class PartyHistory extends AbstractEntity {
    private String partyId;
    private Date snapshotTime;
    private List<ParticipantHistory> participants;

    public String getPartyId() {
        return partyId;
    }

    public void setPartyId(String partyId) {
        this.partyId = partyId;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getSnapshotTime() {
        return snapshotTime;
    }

    public void setSnapshotTime(Date snapshotTime) {
        this.snapshotTime = snapshotTime;
    }

    @OneToMany(mappedBy="partySnapshot", cascade=CascadeType.PERSIST)
    public List<ParticipantHistory> getParticipants() {
        if(participants == null){
            participants = new ArrayList<ParticipantHistory>();
        }
        return participants;
    }

    public void addParticipantHistory(ParticipantHistory participantHistory){
        getParticipants().add(participantHistory);
        participantHistory.setPartySnapshot(this);

    }

    public void setParticipants(List<ParticipantHistory> participants) {
        this.participants = participants;
    }
}
