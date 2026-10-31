package drinkcounter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import drinkcounter.model.Party;
import drinkcounter.repository.PartyRepository;
import drinkcounter.repository.UserRepository;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class PartyRosterTest {

    private final PartyRoster roster = new PartyRoster(mock(PartyRepository.class), mock(UserRepository.class));

    @Test
    void newPartyEndsOneDayAfterItStarts() {
        Party party = roster.start("Sauna night");

        assertEquals(Duration.ofDays(1), Duration.between(party.getStartTime(), party.getEndTime()));
    }
}
