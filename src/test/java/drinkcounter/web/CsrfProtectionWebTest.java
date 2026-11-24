package drinkcounter.web;

import drinkcounter.DrinkLog;
import drinkcounter.PassphraseLogin;
import drinkcounter.UserAccounts;
import drinkcounter.authentication.WithDrinkcounterUser;
import drinkcounter.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The session-cookie chain refuses an unsafe request without a CSRF token, except under the passphrase
 * endpoints. Unlike the other controller tests, requests here carry no token of their own.
 */
@ControllerWebTest
@WithDrinkcounterUser(userId = 42)
public class CsrfProtectionWebTest {

    private MockMvc mvc;

    @Autowired
    private DrinkLog drinkLog;

    @Autowired
    private UserAccounts userAccounts;

    @Autowired
    private PassphraseLogin passphraseLogin;

    @BeforeEach
    public void setUp(@Autowired WebApplicationContext context) {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        User user = new User();
        user.setId(42);
        when(userAccounts.get(42)).thenReturn(user);
    }

    @Test
    public void postWithoutATokenIsForbiddenAndChangesNothing() throws Exception {
        mvc.perform(post("/API/v2/profile/drinks"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(drinkLog);
    }

    @Test
    @WithAnonymousUser
    public void passphraseEndpointsTakeNoToken() throws Exception {
        mvc.perform(post("/API/passphrase/some-passphrase/undo-drink"));

        verify(passphraseLogin).findUser("some-passphrase");
    }
}
