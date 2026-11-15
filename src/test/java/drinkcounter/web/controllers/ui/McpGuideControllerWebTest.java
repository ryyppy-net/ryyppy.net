package drinkcounter.web.controllers.ui;

import drinkcounter.authentication.WithDrinkcounterUser;
import drinkcounter.web.ControllerWebTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ControllerWebTest
public class McpGuideControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @WithDrinkcounterUser(userId = 42)
    public void guideNamesTheServerAddressOfTheHostItIsServedFrom() throws Exception {
        mvc.perform(get("/ui/mcp").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(view().name("mcp"))
                .andExpect(model().attribute("mcpUrl", "http://localhost/mcp"))
                .andExpect(content().string(containsString("claude mcp add --transport http ryyppynet http://localhost/mcp")));
    }

    @Test
    @WithDrinkcounterUser(userId = 42)
    public void guideIsTranslatedToEnglishAndFinnish() throws Exception {
        mvc.perform(get("/ui/mcp").param("lang", "en"))
                .andExpect(content().string(containsString("Connect an MCP client")))
                .andExpect(content().string(not(containsString("??"))));

        mvc.perform(get("/ui/mcp").param("lang", "fi"))
                .andExpect(content().string(containsString("Yhdistä MCP-asiakasohjelma")))
                .andExpect(content().string(not(containsString("??"))));
    }

    @Test
    @WithAnonymousUser
    public void guideIsForSignedInUsersOnly() throws Exception {
        mvc.perform(get("/ui/mcp"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/login"));
    }
}
