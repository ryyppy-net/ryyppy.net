package drinkcounter.web.controllers.ui;

import drinkcounter.UserService;
import drinkcounter.authentication.WithDrinkcounterUser;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;
import drinkcounter.web.ControllerWebTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Drives the signed-in user handlers of UserController through MockMvc and the app's
 * security filter chain.
 */
@ControllerWebTest
@WithDrinkcounterUser(userId = 42)
public class UserControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserService userService;

    private User user;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setId(42);
        when(userService.getUser(42)).thenReturn(user);
    }

    @Test
    public void userPageShowsTheSignedInUserWithPartiesNewestFirst() throws Exception {
        Party older = party("2024-01-01T00:00:00Z");
        Party newer = party("2024-02-01T00:00:00Z");
        user.setParties(new ArrayList<>(List.of(older, newer)));

        mvc.perform(get("/ui/user"))
                .andExpect(status().isOk())
                .andExpect(view().name("user"))
                .andExpect(model().attribute("user", user))
                .andExpect(model().attribute("parties", List.of(newer, older)));
    }

    @Test
    public void passphrasePageShowsThePassphraseOfTheSignedInUser() throws Exception {
        user.setPassphrase("correct horse");

        mvc.perform(get("/ui/passphrase"))
                .andExpect(status().isOk())
                .andExpect(view().name("passphrase"))
                .andExpect(model().attribute("passphrase", "correct horse"));
    }

    @Test
    public void passphraseIsGeneratedForTheSignedInUser() throws Exception {
        mvc.perform(get("/ui/passphrase-generate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("passphrase"));

        verify(userService).generatePassphrase(user);
    }

    @Test
    @WithAnonymousUser
    public void anonymousRequestsAreSentToTheLoginPage() throws Exception {
        for (String path : List.of("/ui/user", "/ui/passphrase", "/ui/passphrase-generate")) {
            mvc.perform(get(path))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/ui/login"));
        }

        verify(userService, never()).getUser(42);
        verify(userService, never()).generatePassphrase(any());
    }

    private Party party(String start) {
        Party party = new Party();
        party.setStartTime(Instant.parse(start));
        return party;
    }
}
