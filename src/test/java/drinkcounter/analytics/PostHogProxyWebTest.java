package drinkcounter.analytics;

import drinkcounter.web.ControllerWebTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerWebTest
@WithAnonymousUser
class PostHogProxyWebTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void proxyIsReachableWithoutSigningIn() throws Exception {
        // Analytics is off in tests, so the proxy answers 404 rather than sending the visitor to the login page.
        mvc.perform(get("/ph/static/array.js")).andExpect(status().isNotFound());
        mvc.perform(post("/ph/e/").content("x")).andExpect(status().isNotFound());
    }

    @Test
    void proxyRefusesOtherMethods() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/ph/e/"))
                .andExpect(status().isMethodNotAllowed());
    }
}
