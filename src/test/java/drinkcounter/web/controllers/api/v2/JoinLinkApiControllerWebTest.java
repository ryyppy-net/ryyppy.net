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

import drinkcounter.JoinLinkPreview;
import drinkcounter.PartyJoinLinks;
import drinkcounter.PartyRoster;
import drinkcounter.UnusableJoinLinkException;
import drinkcounter.authentication.WithDrinkcounterUser;
import drinkcounter.model.Party;
import drinkcounter.web.ControllerWebTest;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

@ControllerWebTest
@WithDrinkcounterUser(userId = 42)
public class JoinLinkApiControllerWebTest {

    private static final String TOKEN = "abcdefghijklmnopqrstuv";
    private static final String LINK = "/API/v2/join-links/" + TOKEN;
    private static final String JOIN = LINK + "/participants";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PartyJoinLinks joinLinks;

    @Autowired
    private PartyRoster partyRoster;

    private Party party;

    @BeforeEach
    public void setUp() {
        party = new Party();
        party.setId(1);
        party.setName("Pikkujoulut");
        party.setStartTime(Instant.parse("2024-03-05T18:00:00Z"));
        party.setEndTime(Instant.parse("2024-03-06T18:00:00Z"));
    }

    @Test
    public void previewShowsOnlyThePartysNameAndTimes() throws Exception {
        when(joinLinks.preview(TOKEN, 42)).thenReturn(new JoinLinkPreview(party, false));

        mvc.perform(get(LINK))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"partyId": 1, "name": "Pikkujoulut",
                         "startTime": "2024-03-05T18:00:00.000Z", "endTime": "2024-03-06T18:00:00.000Z",
                         "alreadyMember": false}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    public void previewTellsAMemberTheyAreAlreadyIn() throws Exception {
        when(joinLinks.preview(TOKEN, 42)).thenReturn(new JoinLinkPreview(party, true));

        mvc.perform(get(LINK))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyMember").value(true));
    }

    @Test
    public void joiningAddsTheSignedInUserAndReturnsTheParty() throws Exception {
        when(joinLinks.join(TOKEN, 42)).thenReturn(party);

        mvc.perform(post(JOIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Pikkujoulut"));

        verify(joinLinks).join(TOKEN, 42);
    }

    @Test
    public void joiningTakesNoOtherUser() throws Exception {
        when(joinLinks.join(TOKEN, 42)).thenReturn(party);

        mvc.perform(post(JOIN).param("userId", "7").param("email", "someone@example.com"))
                .andExpect(status().isOk());

        verify(joinLinks).join(TOKEN, 42);
        verifyNoInteractions(partyRoster);
    }

    @Test
    public void unusableLinkIsNotFound() throws Exception {
        when(joinLinks.preview(TOKEN, 42)).thenThrow(new UnusableJoinLinkException());

        mvc.perform(get(LINK)).andExpect(status().isNotFound());
    }

    @Test
    public void joiningWithAnUnusableLinkIsNotFound() throws Exception {
        when(joinLinks.join(TOKEN, 42)).thenThrow(new UnusableJoinLinkException());

        mvc.perform(post(JOIN)).andExpect(status().isNotFound());

        verifyNoInteractions(partyRoster);
    }

    @Test
    @WithAnonymousUser
    public void anonymousRequestIsSentToTheLoginPage() throws Exception {
        mvc.perform(get(LINK))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/login"));
        mvc.perform(post(JOIN))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/login"));

        verifyNoInteractions(joinLinks);
    }
}
