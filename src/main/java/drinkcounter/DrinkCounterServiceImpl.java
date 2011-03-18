/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter;

import drinkcounter.dao.DrinkDAO;
import drinkcounter.dao.ParticipantDAO;
import drinkcounter.dao.PartyDAO;
import drinkcounter.model.Drink;
import drinkcounter.model.Participant;
import drinkcounter.model.Party;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Toni
 */
@Service
public class DrinkCounterServiceImpl implements DrinkCounterService {

    private static final Logger log = LoggerFactory.getLogger(DrinkCounterServiceImpl.class);
    @Autowired
    private PartyDAO partyDao;
    @Autowired
    private DrinkDAO drinkDao;
    @Autowired
    private ParticipantDAO participantDAO;

    @Override
    public Party startParty(String identifier) {
        if(StringUtils.isBlank(identifier)){
            throw new IllegalArgumentException("Party id cannot be empty");
        }
        Party existingParty = partyDao.findById(identifier);
        if(existingParty != null){
            throw new IllegalArgumentException("Party id must be unique");
        }
        Party party = new Party();
        party.setId(identifier);
        partyDao.save(party);

        log.info("Party {} was started!", identifier);
        return party;
    }

    @Override
    public void updateParty(Party party) {
        partyDao.save(party);
    }

    @Override
    public List<Party> listParties() {
        return partyDao.readAll();
    }

    @Override
    public Party getParty(String identifier) {
        return partyDao.findById(identifier);
    }

    @Override
    public List<Participant> listParticipants(String partyIdentifier) {
        return new LinkedList(getParty(partyIdentifier).getParticipants());
    }

    @Override
    @Transactional
    public Participant addParticipant(Participant participant, String partyIdentifier) {
        Party party = getParty(partyIdentifier);
        party.addParticipant(participant);
        participantDAO.save(participant);
        log.info("Participant with name {} was added to party {}", participant.getName(), party.getName());
        return participant;
    }

    @Override
    @Transactional
    public void addDrink(String participantIdentifier) {
        Participant participant = participantDAO.readByPrimaryKey(Integer.parseInt(participantIdentifier));
        String partyId = participant.getParty().getId();
        participant.drink();
        Drink drink = new Drink();
        drink.setDrinker(participant);
        drink.setTimeStamp(new Date());
        drinkDao.save(drink);
        log.info("Participant {} has drunk a drink in party {}", participant.getName(), partyId);
    }

    @Override
    public List<Drink> getDrinks(String participantIdentifier) {
        Participant participant = getParticipant(participantIdentifier);
        return drinkDao.findByDrinker(participant);
    }

    @Override
    public Participant getParticipant(String participantId) {
        return participantDAO.readByPrimaryKey(Integer.parseInt(participantId));
    }

    @Override
    public String getPartyIdForParticipant(String participantId) {
        return getParticipant(participantId).getParty().getId();
    }

     * 
     * cascade perhaps? --murg
    @Override
    @Transactional
    public void deleteParticipant(String participantId) {
        Participant participant = getParticipant(participantId);
        List<Drink> drinks = getDrinks(participantId);
        for (Drink drink : drinks) {
            drinkDao.delete(drink);
        }
        participantDAO.delete(participant);
    }

    @Transactional
}
