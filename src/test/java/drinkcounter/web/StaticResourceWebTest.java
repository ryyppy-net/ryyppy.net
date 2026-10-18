package drinkcounter.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.support.RequestContextUtils;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerWebTest
class StaticResourceWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void dispatcherKeepsFlashAttributesOutOfTheSession() throws Exception {
        mockMvc.perform(get("/sw.js"))
                .andExpect(status().isOk())
                .andExpect(result -> assertInstanceOf(NoFlashMapManager.class,
                        RequestContextUtils.getFlashMapManager(result.getRequest())));
    }
}
