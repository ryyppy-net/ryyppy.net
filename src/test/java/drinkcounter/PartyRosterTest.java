package drinkcounter;

import drinkcounter.analytics.Analytics;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import drinkcounter.repository.PartyRepository;
import drinkcounter.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PartyRosterTest {

    private PartyRepository partyRepository;
    private Analytics analytics;
    private PartyRoster partyRoster;
    private User user;
    private Party party;

    @BeforeEach
    void setUp() {
        partyRepository = mock(PartyRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        analytics = mock(Analytics.class);
        partyRoster = new PartyRoster(partyRepository, userRepository, analytics);

        user = new User();
        user.setId(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        party = new Party();
        party.setId(5);
        when(partyRepository.findById(5)).thenReturn(Optional.of(party));
        when(partyRepository.save(any(Party.class))).thenAnswer(invocation -> {
            Party saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(5);
                when(partyRepository.findById(5)).thenReturn(Optional.of(saved));
            }
            return saved;
        });
    }

    @Test
    void joiningAPartyIsReportedToAnalytics() {
        partyRoster.join(5, 1);

        verify(analytics).capture(user, "party_joined", Map.of("party_id", 5));
    }

    @Test
    void joiningAPartyTwiceIsReportedOnce() {
        partyRoster.join(5, 1);
        partyRoster.join(5, 1);

        verify(analytics).capture(any(), any(), any());
    }

    @Test
    void startingAPartyForAUserAddsThemAndReportsTheCreation() {
        Party started = partyRoster.start("Sauna", 1);

        org.junit.jupiter.api.Assertions.assertTrue(started.getParticipants().contains(user));
        verify(analytics).capture(user, "party_created", Map.of("party_id", 5));
    }

    @Test
    void startingAPartyWithoutAUserReportsNothing() {
        partyRoster.start("Sauna");

        verify(analytics, never()).capture(any(), any(), any());
    }
}
