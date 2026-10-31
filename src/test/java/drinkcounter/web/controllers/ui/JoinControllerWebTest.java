package drinkcounter.web.controllers.ui;

import drinkcounter.PartyJoinLinks;
import drinkcounter.authentication.WithDrinkcounterUser;
import drinkcounter.web.ControllerWebTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerWebTest
public class JoinControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PartyJoinLinks joinLinks;

    @Test
    @WithDrinkcounterUser(userId = 42)
    public void signedInUserIsSentToTheJoinPageOfTheApp() throws Exception {
        mvc.perform(get("/join/AbCdEfGhIjKlMnOpQrSt-_"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/index.html#!/join/AbCdEfGhIjKlMnOpQrSt-_"));

        verifyNoInteractions(joinLinks);
    }

    @Test
    @WithDrinkcounterUser(userId = 42)
    public void tokenIsEncodedIntoTheJoinPageAddress() throws Exception {
        mvc.perform(get("/join/{token}", "a b#c"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/index.html#!/join/a%20b%23c"));
    }

    @Test
    @WithAnonymousUser
    public void signedOutVisitorIsSentToTheLoginPage() throws Exception {
        mvc.perform(get("/join/AbCdEfGhIjKlMnOpQrSt-_"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/login"));

        verifyNoInteractions(joinLinks);
    }
}
