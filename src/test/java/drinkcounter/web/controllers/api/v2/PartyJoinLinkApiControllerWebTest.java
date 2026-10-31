package drinkcounter.web.controllers.api.v2;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import drinkcounter.PartyJoinLinks;
import drinkcounter.authentication.WithDrinkcounterUser;
import drinkcounter.model.PartyJoinLink;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@ControllerWebTest
@WithDrinkcounterUser(userId = 42)
public class PartyJoinLinkApiControllerWebTest {

    private static final String JOIN_LINK = "/API/v2/parties/1/join-link";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PartyJoinLinks joinLinks;

    @Autowired
    private PartyRepository partyRepository;

    private PartyJoinLink link;

    @BeforeEach
    public void setUp() {
        link = new PartyJoinLink();
        link.setPartyId(1);
        link.setToken("abcdefghijklmnopqrstuv");
        link.setCreatedBy(42);
        link.setCreatedAt(Instant.parse("2024-03-05T12:00:00Z"));
        when(partyRepository.countUserParticipations(1, 42)).thenReturn(1L);
    }

    @Test
    public void memberSeesTheCurrentLink() throws Exception {
        when(joinLinks.current(1)).thenReturn(Optional.of(link));

        mvc.perform(get(JOIN_LINK))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("http://localhost/join/abcdefghijklmnopqrstuv"))
                .andExpect(jsonPath("$.createdAt").value("2024-03-05T12:00:00.000Z"));
    }

    @Test
    public void partyWithoutALinkIsNotFound() throws Exception {
        when(joinLinks.current(1)).thenReturn(Optional.empty());

        mvc.perform(get(JOIN_LINK)).andExpect(status().isNotFound());
    }

    @Test
    public void linkPointsToTheEnvironmentItWasCreatedOn() throws Exception {
        when(joinLinks.regenerate(1, 42)).thenReturn(link);

        mvc.perform(post(JOIN_LINK).with(request -> {
                    request.setScheme("https");
                    request.setServerName("pr-12.ryyppy.net");
                    request.setServerPort(443);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://pr-12.ryyppy.net/join/abcdefghijklmnopqrstuv"));
    }

    @Test
    public void memberCreatesTheLinkAsThemselves() throws Exception {
        when(joinLinks.regenerate(1, 42)).thenReturn(link);

        mvc.perform(post(JOIN_LINK))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("http://localhost/join/abcdefghijklmnopqrstuv"))
                .andExpect(jsonPath("$.createdAt").value("2024-03-05T12:00:00.000Z"));

        verify(joinLinks).regenerate(1, 42);
    }

    @Test
    public void memberRevokesTheLink() throws Exception {
        mvc.perform(delete(JOIN_LINK)).andExpect(status().isNoContent());

        verify(joinLinks).revoke(1);
    }

    static Stream<Arguments> joinLinkRequests() {
        return Stream.of(
                Arguments.of("get link", get(JOIN_LINK)),
                Arguments.of("create link", post(JOIN_LINK)),
                Arguments.of("revoke link", delete(JOIN_LINK)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("joinLinkRequests")
    public void outsiderIsForbiddenAndChangesNothing(String name, MockHttpServletRequestBuilder request) throws Exception {
        when(partyRepository.countUserParticipations(1, 42)).thenReturn(0L);

        mvc.perform(request).andExpect(status().isForbidden());

        verify(partyRepository).countUserParticipations(1, 42);
        verifyNoInteractions(joinLinks);
    }

    @Test
    @WithAnonymousUser
    public void anonymousRequestIsSentToTheLoginPage() throws Exception {
        mvc.perform(get(JOIN_LINK))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/login"));

        verifyNoInteractions(joinLinks);
    }
}
