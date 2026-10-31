package drinkcounter;

import drinkcounter.analytics.Analytics;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import drinkcounter.repository.PartyRepository;
import drinkcounter.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Limit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartyRoster {

    private static final Logger log = LoggerFactory.getLogger(PartyRoster.class);

    private final PartyRepository partyRepository;
    private final UserRepository userRepository;
    private final Analytics analytics;

    public PartyRoster(PartyRepository partyRepository, UserRepository userRepository, Analytics analytics) {
        this.partyRepository = partyRepository;
        this.userRepository = userRepository;
        this.analytics = analytics;
    }

    @Transactional
    public Party start(String partyName) {
        if(partyName == null || partyName.isBlank()) {
            throw new IllegalArgumentException("Party name cannot be empty");
        }

        Party party = new Party();
        party.setName(partyName);
        party.setStartTime(Instant.now());
        partyRepository.save(party);

        log.info("Party {}, id {} was started!", partyName, party.getId());
        return party;
    }

    /** Starts a party that the user joins as its first participant. */
    @Transactional
    public Party start(String partyName, int userId) {
        Party party = start(partyName);
        join(party.getId(), userId);
        analytics.capture(userRepository.findById(userId).orElseThrow(EntityNotFoundException::new),
                "party_created", Map.of("party_id", party.getId()));
        return party;
    }

    public Party get(int partyId) {
        return partyRepository.findById(partyId).orElseThrow(EntityNotFoundException::new);
    }

    /** The user's {@code count} newest parties by start time, newest first. */
    public List<Party> newestOf(int userId, int count) {
        return partyRepository.findNewestOfParticipant(userId, Limit.of(count));
    }

    /** As {@link #newestOf}, with each party's participants loaded in the same query. */
    public List<Party> newestWithParticipantsOf(int userId, int count) {
        return partyRepository.findNewestOfParticipantWithParticipants(userId, count);
    }

    public List<User> members(int partyId) {
        return new LinkedList<User>(get(partyId).getParticipants());
    }

    @Transactional
    public void join(int partyId, int userId) {
        Party party = get(partyId);
        User user = userRepository.findById(userId).orElseThrow(EntityNotFoundException::new);

        for (User current : get(party.getId()).getParticipants()) {
            if (current.getId().equals(user.getId())) {
                log.info("{} was already added to party {}. Skipping", user, party.getName());
                return;
            }
        }

        party.addParticipant(user);
        partyRepository.save(party);
        log.info("{} was added to party {}", user, party.getName());
        analytics.capture(user, "party_joined", Map.of("party_id", party.getId()));
    }

    @Transactional
    public void leave(int partyId, int userId) {
        Party party = get(partyId);
        User user = userRepository.findById(userId).orElseThrow(EntityNotFoundException::new);
        party.removeParticipant(user);
        partyRepository.save(party);
        log.info("{} was removed from party {}", user, party.getName());
    }
}
