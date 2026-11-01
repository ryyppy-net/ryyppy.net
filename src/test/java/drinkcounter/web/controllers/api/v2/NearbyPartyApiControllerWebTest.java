package drinkcounter.web.controllers.api.v2;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import drinkcounter.NearbyParties;
import drinkcounter.NearbyParty;
import drinkcounter.NearbyPartyPreview;
import drinkcounter.PartyRoster;
import drinkcounter.Position;
import drinkcounter.RoughDistance;
import drinkcounter.UnusableLocationInviteException;
import drinkcounter.authentication.WithDrinkcounterUser;
import drinkcounter.model.Party;
import drinkcounter.web.ControllerWebTest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

@ControllerWebTest
@WithDrinkcounterUser(userId = 42)
public class NearbyPartyApiControllerWebTest {

    private static final String SEARCH = "/API/v2/nearby-parties";
    private static final String PREVIEW = SEARCH + "/1";
    private static final String JOIN = PREVIEW + "/participants";
    private static final Position HERE = new Position(60.1699, 24.9384);
    private static final String HERE_JSON = """
            {"latitude": 60.1699, "longitude": 24.9384}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private NearbyParties nearbyParties;

    @Autowired
    private PartyRoster partyRoster;

    private Party party;

    @BeforeEach
    public void setUp() {
        party = party(1, "Pikkujoulut");
    }

    @Test
    public void searchListsNearbyPartiesWithOnlyTheirNameTimesAndRoughDistance() throws Exception {
        Party other = party(2, "Saunailta");
        when(nearbyParties.around(HERE)).thenReturn(List.of(
                new NearbyParty(party, new RoughDistance(100, true)),
                new NearbyParty(other, new RoughDistance(1500, false))));

        mvc.perform(get(SEARCH).param("lat", "60.1699").param("lon", "24.9384"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"partyId": 1, "name": "Pikkujoulut",
                          "startTime": "2024-03-05T18:00:00.000Z", "endTime": "2024-03-06T18:00:00.000Z",
                          "distance": {"metres": 100, "lessThan": true}},
                         {"partyId": 2, "name": "Saunailta",
                          "startTime": "2024-03-05T18:00:00.000Z", "endTime": "2024-03-06T18:00:00.000Z",
                          "distance": {"metres": 1500, "lessThan": false}}]
                        """, JsonCompareMode.STRICT));
    }

    @Test
    public void searchWithNothingNearbyIsAnEmptyList() throws Exception {
        when(nearbyParties.around(HERE)).thenReturn(List.of());

        mvc.perform(get(SEARCH).param("lat", "60.1699").param("lon", "24.9384"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]", JsonCompareMode.STRICT));
    }

    @Test
    public void previewShowsOnlyThePartysNameTimesAndRoughDistance() throws Exception {
        when(nearbyParties.preview(1, HERE, 42)).thenReturn(
                new NearbyPartyPreview(new NearbyParty(party, new RoughDistance(300, false)), false));

        mvc.perform(get(PREVIEW).param("lat", "60.1699").param("lon", "24.9384"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"partyId": 1, "name": "Pikkujoulut",
                         "startTime": "2024-03-05T18:00:00.000Z", "endTime": "2024-03-06T18:00:00.000Z",
                         "distance": {"metres": 300, "lessThan": false}, "alreadyMember": false}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    public void previewTellsAMemberTheyAreAlreadyIn() throws Exception {
        when(nearbyParties.preview(1, HERE, 42)).thenReturn(
                new NearbyPartyPreview(new NearbyParty(party, new RoughDistance(300, false)), true));

        mvc.perform(get(PREVIEW).param("lat", "60.1699").param("lon", "24.9384"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyMember").value(true));
    }

    @Test
    public void joiningAddsOnlyTheSignedInUserAndReturnsTheParty() throws Exception {
        when(nearbyParties.join(1, HERE, 42)).thenReturn(party);

        mvc.perform(post(JOIN).param("userId", "7").contentType(MediaType.APPLICATION_JSON).content("""
                        {"latitude": 60.1699, "longitude": 24.9384, "userId": 7}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Pikkujoulut"));

        verify(nearbyParties).join(1, HERE, 42);
        verifyNoInteractions(partyRoster);
    }

    @Test
    public void unusablePartyIsNotFound() throws Exception {
        when(nearbyParties.preview(1, HERE, 42)).thenThrow(new UnusableLocationInviteException());

        mvc.perform(get(PREVIEW).param("lat", "60.1699").param("lon", "24.9384"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    public void joiningFromOutsideTheRadiusIsNotFoundAndJoinsNobody() throws Exception {
        when(nearbyParties.join(1, HERE, 42)).thenThrow(new UnusableLocationInviteException());

        mvc.perform(post(JOIN).contentType(MediaType.APPLICATION_JSON).content(HERE_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        verifyNoInteractions(partyRoster);
    }

    @Test
    public void invalidOrMissingCoordinatesAreABadRequest() throws Exception {
        mvc.perform(get(SEARCH).param("lat", "91").param("lon", "24.9384")).andExpect(status().isBadRequest());
        mvc.perform(get(SEARCH).param("lat", "60.1699").param("lon", "-181")).andExpect(status().isBadRequest());
        mvc.perform(get(SEARCH).param("lat", "NaN").param("lon", "24.9384")).andExpect(status().isBadRequest());
        mvc.perform(get(SEARCH).param("lat", "north").param("lon", "24.9384")).andExpect(status().isBadRequest());
        mvc.perform(get(SEARCH).param("lat", "60.1699")).andExpect(status().isBadRequest());
        mvc.perform(get(PREVIEW).param("lat", "60.1699")).andExpect(status().isBadRequest());
        mvc.perform(post(JOIN).contentType(MediaType.APPLICATION_JSON).content("""
                {"latitude": 60.1699}""")).andExpect(status().isBadRequest());
        mvc.perform(post(JOIN).contentType(MediaType.APPLICATION_JSON).content("""
                {"latitude": 95, "longitude": 24.9384}""")).andExpect(status().isBadRequest());
        mvc.perform(post(JOIN).contentType(MediaType.APPLICATION_JSON).content("""
                {"latitude": "north", "longitude": 24.9384}""")).andExpect(status().isBadRequest());

        verifyNoInteractions(nearbyParties, partyRoster);
    }

    @Test
    @WithAnonymousUser
    public void anonymousRequestIsSentToTheLoginPage() throws Exception {
        mvc.perform(get(SEARCH).param("lat", "60.1699").param("lon", "24.9384"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/login"));
        mvc.perform(get(PREVIEW).param("lat", "60.1699").param("lon", "24.9384"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/login"));
        mvc.perform(post(JOIN).contentType(MediaType.APPLICATION_JSON).content(HERE_JSON))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/login"));

        verifyNoInteractions(nearbyParties, partyRoster);
    }

    private static Party party(int id, String name) {
        Party party = new Party();
        party.setId(id);
        party.setName(name);
        party.setStartTime(Instant.parse("2024-03-05T18:00:00Z"));
        party.setEndTime(Instant.parse("2024-03-06T18:00:00Z"));
        return party;
    }
}
