package drinkcounter.mcp;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import drinkcounter.authentication.WithDrinkcounterUser;
import drinkcounter.mcp.McpConsentRequests.McpConsentRequest;
import drinkcounter.web.ControllerWebTest;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

@ControllerWebTest
class McpConsentControllerWebTest {

    private static final String CLIENT_ID = "https://claude.ai/oauth/claude-code-client-metadata";

    @Autowired
    MockMvc mvc;

    @Autowired
    McpConsentRequests consentRequests;

    @Test
    @WithDrinkcounterUser(email = "matti@example.com")
    void showsTheClientWhereTheAccessGoesAndTheAccount() throws Exception {
        waiting("localhost");

        mvc.perform(get("/ui/mcp/consent").param("state", "consent-state").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(view().name("mcp-consent"))
                .andExpect(content().string(containsString("Allow Claude Code to use your account?")))
                .andExpect(content().string(containsString("matti@example.com")))
                .andExpect(content().string(containsString("<code id=\"consentClientHost\">claude.ai</code>")))
                .andExpect(content().string(containsString("<code id=\"consentRedirectHost\">localhost</code>")))
                .andExpect(content().string(containsString("id=\"consentLocalhostWarning\"")))
                .andExpect(content().string(containsString("name=\"client_id\" value=\"" + CLIENT_ID + "\"")))
                .andExpect(content().string(containsString("name=\"state\" value=\"consent-state\"")))
                .andExpect(content().string(containsString("name=\"scope\" value=\"mcp\"")))
                .andExpect(content().string(not(containsString("??"))));
    }

    @Test
    @WithDrinkcounterUser(email = "matti@example.com")
    void isTranslatedToFinnish() throws Exception {
        waiting("localhost");

        mvc.perform(get("/ui/mcp/consent").param("state", "consent-state").param("lang", "fi"))
                .andExpect(content().string(containsString("Sallitko asiakasohjelman Claude Code käyttää tiliäsi?")))
                .andExpect(content().string(not(containsString("??"))));
    }

    @Test
    @WithDrinkcounterUser(email = "matti@example.com")
    void aRedirectToAWebsiteHasNoLocalhostWarning() throws Exception {
        waiting("claude.ai");

        mvc.perform(get("/ui/mcp/consent").param("state", "consent-state"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("id=\"consentLocalhostWarning\""))));
    }

    @Test
    @WithDrinkcounterUser(email = "matti@example.com")
    void aRequestThatIsNotWaitingForThisUserIsABadRequest() throws Exception {
        when(consentRequests.find(anyString(), anyString())).thenReturn(Optional.empty());

        mvc.perform(get("/ui/mcp/consent").param("state", "someone-elses"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithAnonymousUser
    void aVisitorWhoIsNotSignedInIsSentToTheLoginPage() throws Exception {
        mvc.perform(get("/ui/mcp/consent").param("state", "consent-state"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/login"));
    }

    private void waiting(String redirectHost) {
        when(consentRequests.find("consent-state", "matti@example.com")).thenReturn(Optional.of(new McpConsentRequest(
                CLIENT_ID, "Claude Code", "claude.ai", redirectHost, "consent-state", Set.of("mcp"))));
    }
}
