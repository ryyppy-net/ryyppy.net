package drinkcounter.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Static assets carry no user data and are cached by the CDN, so they must load without a session.
 */
@ControllerWebTest
@WithAnonymousUser
public class StaticResourceAccessWebTest {

    @Autowired
    private MockMvc mvc;

    @ParameterizedTest
    @ValueSource(strings = {
            "/app/js/app.js",
            "/app/js/components/drinker.js",
            "/app/css/app.css",
            "/static/js/common.js"
    })
    public void anonymousVisitorGetsAPubliclyCacheableAsset(String path) throws Exception {
        mvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("public")));
    }

    @Test
    public void appIndexStillRequiresSignIn() throws Exception {
        mvc.perform(get("/app/index.html"))
                .andExpect(status().is3xxRedirection());
    }
}
