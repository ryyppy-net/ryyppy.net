/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter;

import drinkcounter.dao.PartyDAO;
import drinkcounter.dao.PartyHistoryDAO;
import drinkcounter.model.Participant;
import drinkcounter.model.ParticipantHistory;
import drinkcounter.model.Party;
import drinkcounter.model.PartyHistory;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author paloniemit
 */
@Service
public class HistoryServiceImpl implements HistoryService{

    private static final Logger log = LoggerFactory.getLogger(DrinkCounterServiceImpl.class);

    @Autowired
    private PartyDAO partyDAO;
    @Autowired
    private PartyHistoryDAO partyHistoryDAO;

    @Override
    public List<PartyHistory> getPartyHistory(String partyId) {
        return partyHistoryDAO.findByPartyId(partyId);
    }

    @Override
    @Transactional
    public void takeHistorySnapshot(String partyId) {
        log.info("Taking history snapshot for party {}", partyId);
        Party party = partyDAO.findById(partyId);
        PartyHistory snapshot = new PartyHistory();
        snapshot.setPartyId(partyId);
        snapshot.setSnapshotTime(new Date());
        for (Participant participant : party.getParticipants()) {
            ParticipantHistory participantSnapshot = new ParticipantHistory();
            participantSnapshot.setAlcoholLevel(participant.getPromilles());
            participantSnapshot.setParticipantName(participant.getName());
            participantSnapshot.setTotalDrinks(participant.getTotalDrinks());
            snapshot.addParticipantHistory(participantSnapshot);
        }
        partyHistoryDAO.save(snapshot);
    }

}
