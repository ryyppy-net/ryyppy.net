package drinkcounter.web.controllers.api.v2;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import drinkcounter.PartyLocationInvites;
import drinkcounter.authentication.WithDrinkcounterUser;
import drinkcounter.model.PartyLocationInvite;
import drinkcounter.repository.PartyRepository;
import drinkcounter.web.ControllerWebTest;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@ControllerWebTest
@WithDrinkcounterUser(userId = 42)
public class PartyLocationInviteApiControllerWebTest {

    private static final String LOCATION_INVITE = "/API/v2/parties/1/location-invite";
    private static final String HELSINKI_1KM = "{\"latitude\": 60.1699, \"longitude\": 24.9384, \"radiusM\": 1000}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PartyLocationInvites locationInvites;

    @Autowired
    private PartyRepository partyRepository;

    private PartyLocationInvite invite;

    @BeforeEach
    public void setUp() {
        invite = new PartyLocationInvite();
        invite.setPartyId(1);
        invite.setLatitude(60.1699);
        invite.setLongitude(24.9384);
        invite.setRadiusM(1000);
        invite.setCreatedBy(42);
        invite.setCreatedAt(Instant.parse("2024-03-05T12:00:00Z"));
        when(partyRepository.countUserParticipations(1, 42)).thenReturn(1L);
    }

    @Test
    public void memberSeesTheCurrentInvite() throws Exception {
        when(locationInvites.current(1)).thenReturn(Optional.of(invite));

        mvc.perform(get(LOCATION_INVITE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(60.1699))
                .andExpect(jsonPath("$.longitude").value(24.9384))
                .andExpect(jsonPath("$.radiusM").value(1000))
                .andExpect(jsonPath("$.createdAt").value("2024-03-05T12:00:00.000Z"));
    }

    @Test
    public void partyWithoutAnInviteIsNotFound() throws Exception {
        when(locationInvites.current(1)).thenReturn(Optional.empty());

        mvc.perform(get(LOCATION_INVITE)).andExpect(status().isNotFound());
    }

    @Test
    public void memberSetsTheInviteAsThemselves() throws Exception {
        when(locationInvites.set(1, 60.1699, 24.9384, 1000, 42)).thenReturn(invite);

        mvc.perform(put(LOCATION_INVITE).contentType(MediaType.APPLICATION_JSON).content(HELSINKI_1KM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(60.1699))
                .andExpect(jsonPath("$.longitude").value(24.9384))
                .andExpect(jsonPath("$.radiusM").value(1000))
                .andExpect(jsonPath("$.createdAt").value("2024-03-05T12:00:00.000Z"));

        verify(locationInvites).set(1, 60.1699, 24.9384, 1000, 42);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"latitude\": 90.5, \"longitude\": 24.9384, \"radiusM\": 1000}",
            "{\"latitude\": -90.5, \"longitude\": 24.9384, \"radiusM\": 1000}",
            "{\"latitude\": 60.1699, \"longitude\": 180.5, \"radiusM\": 1000}",
            "{\"latitude\": 60.1699, \"longitude\": -180.5, \"radiusM\": 1000}",
            "{\"latitude\": 60.1699, \"longitude\": 24.9384, \"radiusM\": 99}",
            "{\"latitude\": 60.1699, \"longitude\": 24.9384, \"radiusM\": 5001}",
            "{\"longitude\": 24.9384, \"radiusM\": 1000}",
            "{\"latitude\": 60.1699, \"radiusM\": 1000}",
            "{\"latitude\": 60.1699, \"longitude\": 24.9384}",
            "{\"latitude\": \"north\", \"longitude\": 24.9384, \"radiusM\": 1000}",
            "not json"
    })
    public void invalidInviteIsABadRequestAndChangesNothing(String body) throws Exception {
        mvc.perform(put(LOCATION_INVITE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(locationInvites);
    }

    @Test
    public void memberTurnsTheInviteOff() throws Exception {
        mvc.perform(delete(LOCATION_INVITE)).andExpect(status().isNoContent());

        verify(locationInvites).revoke(1);
    }

    static Stream<Arguments> locationInviteRequests() {
        return Stream.of(
                Arguments.of("get invite", get(LOCATION_INVITE)),
                Arguments.of("set invite",
                        put(LOCATION_INVITE).contentType(MediaType.APPLICATION_JSON).content(HELSINKI_1KM)),
                Arguments.of("turn invite off", delete(LOCATION_INVITE)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("locationInviteRequests")
    public void outsiderIsForbiddenAndChangesNothing(String name, MockHttpServletRequestBuilder request)
            throws Exception {
        when(partyRepository.countUserParticipations(1, 42)).thenReturn(0L);

        mvc.perform(request).andExpect(status().isForbidden());

        verify(partyRepository).countUserParticipations(1, 42);
        verifyNoInteractions(locationInvites);
    }

    @Test
    @WithAnonymousUser
    public void anonymousRequestIsSentToTheLoginPage() throws Exception {
        mvc.perform(get(LOCATION_INVITE))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/login"));

        verifyNoInteractions(locationInvites);
    }
}
