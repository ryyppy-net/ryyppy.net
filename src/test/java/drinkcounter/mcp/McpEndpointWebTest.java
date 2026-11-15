package drinkcounter.mcp;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import drinkcounter.PartyRoster;
import drinkcounter.UserAccounts;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import drinkcounter.repository.PartyRepository;
import java.time.Instant;
import java.util.List;
import drinkcounter.web.ControllerWebTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** The MCP server and its authorization server, behind the real security filter chains. */
@ControllerWebTest
@TestPropertySource(properties = {
        "spring.ai.mcp.server.protocol=STATELESS",
        "spring.security.oauth2.authorizationserver.issuer=http://localhost"
})
@Import({McpSecurityConfiguration.class, McpErrorSerializer.class, PartyTools.class, StatusTools.class})
@ImportAutoConfiguration(classes = {
        org.springframework.boot.security.oauth2.server.authorization.autoconfigure.servlet.OAuth2AuthorizationServerAutoConfiguration.class,
        org.springframework.boot.security.oauth2.server.authorization.autoconfigure.servlet.OAuth2AuthorizationServerJwtAutoConfiguration.class,
        org.springframework.ai.mcp.server.common.autoconfigure.McpServerJsonMapperAutoConfiguration.class,
        org.springframework.ai.mcp.server.common.autoconfigure.McpServerStatelessAutoConfiguration.class,
        org.springframework.ai.mcp.server.common.autoconfigure.StatelessToolCallbackConverterAutoConfiguration.class,
        org.springframework.ai.mcp.server.common.autoconfigure.annotations.StatelessServerSpecificationFactoryAutoConfiguration.class,
        org.springframework.ai.mcp.server.common.autoconfigure.annotations.McpServerAnnotationScannerAutoConfiguration.class,
        org.springframework.ai.mcp.server.webmvc.autoconfigure.McpServerStatelessWebMvcAutoConfiguration.class
})
class McpEndpointWebTest {

    private static final String JSON_RPC_LIST_TOOLS = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}";
    private static final String JSON_AND_SSE = "application/json, text/event-stream";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserAccounts userAccounts;

    @Autowired
    PartyRoster partyRoster;

    @Autowired
    PartyRepository partyRepository;

    @Test
    void authorizationServerAcceptsClientsWithoutASecret() throws Exception {
        mockMvc.perform(get("/.well-known/oauth-authorization-server"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_endpoint_auth_methods_supported", hasItem("none")))
                .andExpect(jsonPath("$.client_id_metadata_document_supported").value(true));
    }

    @Test
    void listPartiesIsMarkedReadOnly() throws Exception {
        when(userAccounts.byEmail("matti@example.com")).thenReturn(new User());

        mockMvc.perform(post("/mcp").with(jwt().jwt(jwt -> jwt.subject("matti@example.com")))
                        .contentType(MediaType.APPLICATION_JSON).header("Accept", JSON_AND_SSE).content(JSON_RPC_LIST_TOOLS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools[?(@.name=='list_parties')].annotations.readOnlyHint").value(true))
                .andExpect(jsonPath("$.result.tools[?(@.name=='list_parties')].annotations.destructiveHint").value(false));
    }

    @Test
    void readToolsAreMarkedReadOnly() throws Exception {
        mockMvc.perform(post("/mcp").with(jwt().jwt(jwt -> jwt.subject("matti@example.com")))
                        .contentType(MediaType.APPLICATION_JSON).header("Accept", JSON_AND_SSE).content(JSON_RPC_LIST_TOOLS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools[?(@.name=='get_my_status')].annotations.readOnlyHint").value(true))
                .andExpect(jsonPath("$.result.tools[?(@.name=='list_my_drinks')].annotations.readOnlyHint").value(true))
                .andExpect(jsonPath("$.result.tools[?(@.name=='get_party_status')].annotations.readOnlyHint").value(true));
    }

    @Test
    void aMemberReadsThePartyStatus() throws Exception {
        signedInMatti();
        when(partyRepository.countUserParticipations(5, 7)).thenReturn(1L);
        Party party = new Party();
        party.setId(5);
        party.setName("Sauna night");
        party.setStartTime(Instant.parse("2026-01-01T18:00:00Z"));
        party.setEndTime(Instant.parse("2026-01-02T18:00:00Z"));
        when(partyRoster.get(5)).thenReturn(party);
        when(partyRoster.members(5)).thenReturn(List.of());

        mockMvc.perform(callTool("get_party_status", "{\"partyId\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isError").value(false))
                .andExpect(jsonPath("$.result.content[0].text", containsString("Sauna night")));
    }

    @Test
    void anOutsiderGetsAToolErrorAndNothingOfThePartyIsRead() throws Exception {
        signedInMatti();
        when(partyRepository.countUserParticipations(5, 7)).thenReturn(0L);

        mockMvc.perform(callTool("get_party_status", "{\"partyId\":5}"))
                .andExpect(jsonPath("$.result.isError").value(true))
                .andExpect(content().string(not(containsString("Sauna night"))));

        verifyNoInteractions(partyRoster);
    }

    private void signedInMatti() {
        User matti = new User();
        matti.setId(7);
        when(userAccounts.byEmail("matti@example.com")).thenReturn(matti);
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder callTool(String name, String arguments) {
        return post("/mcp").with(jwt().jwt(jwt -> jwt.subject("matti@example.com")))
                .contentType(MediaType.APPLICATION_JSON).header("Accept", JSON_AND_SSE)
                .content("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\",\"params\":{\"name\":\"" + name
                        + "\",\"arguments\":" + arguments + "}}");
    }

    @Test
    void rejectedRequestIsAJsonRpcErrorWithoutExceptionDetails() throws Exception {
        mockMvc.perform(post("/mcp").with(jwt().jwt(jwt -> jwt.subject("matti@example.com")))
                        .contentType(MediaType.APPLICATION_JSON).header("Accept", JSON_AND_SSE).content("{bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.error.code").value(-32600))
                .andExpect(jsonPath("$.error.message").value("Invalid message format"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(content().string(not(containsString("className"))));
    }
}
