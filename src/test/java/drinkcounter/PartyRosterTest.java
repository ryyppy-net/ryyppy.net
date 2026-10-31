package drinkcounter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.mock;

import drinkcounter.model.Party;
import drinkcounter.repository.PartyRepository;
import drinkcounter.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PartyRosterTest {

    private final PartyRepository parties = mock(PartyRepository.class);
    private final PartyRoster roster = new PartyRoster(parties, mock(UserRepository.class));

    @Test
    void newPartyEndsOneDayAfterItStarts() {
        Party party = roster.start("Sauna night");

        assertEquals(Duration.ofDays(1), Duration.between(party.getStartTime(), party.getEndTime()));
    }

    @Test
    void newPartyEndsGivenNumberOfDaysAfterItStarts() {
        Party party = roster.start("Mökki", 3);

        assertEquals(Duration.ofDays(3), Duration.between(party.getStartTime(), party.getEndTime()));
    }

    @Test
    void newPartyLastingLessThanADayIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> roster.start("Mökki", 0));

        verifyNoInteractions(parties);
    }

    @Test
    void changingDurationMovesTheEndRelativeToTheStart() {
        Party party = new Party();
        party.setStartTime(Instant.parse("2024-03-05T12:00:00Z"));
        party.setEndTime(Instant.parse("2024-03-06T12:00:00Z"));
        when(parties.findById(1)).thenReturn(Optional.of(party));

        roster.changeDuration(1, 4);

        assertEquals(Instant.parse("2024-03-09T12:00:00Z"), party.getEndTime());
    }

    @Test
    void changingDurationToLessThanADayIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> roster.changeDuration(1, 0));
    }
}
