package drinkcounter;

import drinkcounter.model.Party;
import drinkcounter.model.User;
import drinkcounter.repository.PartyRepository;
import drinkcounter.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
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

    public PartyRoster(PartyRepository partyRepository, UserRepository userRepository) {
        this.partyRepository = partyRepository;
        this.userRepository = userRepository;
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

    public Party get(int partyId) {
        return partyRepository.findById(partyId).orElseThrow(EntityNotFoundException::new);
    }

    /** The user's {@code count} newest parties by start time, newest first. */
    public List<Party> newestOf(int userId, int count) {
        return partyRepository.findNewestOfParticipant(userId, Limit.of(count));
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
