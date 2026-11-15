package drinkcounter.mcp;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import drinkcounter.DrinkLog;
import drinkcounter.InvitationSuggestions;
import drinkcounter.PartyJoinLinks;
import drinkcounter.PartyRoster;
import drinkcounter.UserAccounts;
import drinkcounter.model.Drink;
import drinkcounter.model.Party;
import drinkcounter.model.PartyJoinLink;
import drinkcounter.model.User;
import drinkcounter.repository.PartyRepository;
import drinkcounter.web.ControllerWebTest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** The MCP server and its authorization server, behind the real security filter chains. */
@ControllerWebTest
@TestPropertySource(properties = {
        "spring.ai.mcp.server.protocol=STATELESS",
        "spring.security.oauth2.authorizationserver.issuer=http://localhost"
})
@Import({McpSecurityConfiguration.class, McpErrorSerializer.class, PartyTools.class, StatusTools.class, DrinkTools.class})
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

    @Autowired
    DrinkLog drinkLog;

    @Autowired
    InvitationSuggestions invitationSuggestions;

    @Autowired
    PartyJoinLinks joinLinks;

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
        signedInAs(7);
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
        signedInAs(7);
        when(partyRepository.countUserParticipations(5, 7)).thenReturn(0L);

        mockMvc.perform(callTool("get_party_status", "{\"partyId\":5}"))
                .andExpect(jsonPath("$.result.isError").value(true))
                .andExpect(content().string(not(containsString("Sauna night"))));

        verifyNoInteractions(partyRoster);
    }

    @Test
    void partyToolsAreAnnotatedByWhatTheyDo() throws Exception {
        mockMvc.perform(post("/mcp").with(jwt().jwt(jwt -> jwt.subject("matti@example.com")))
                        .contentType(MediaType.APPLICATION_JSON).header("Accept", JSON_AND_SSE).content(JSON_RPC_LIST_TOOLS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools[?(@.name=='start_party')].annotations.readOnlyHint").value(false))
                .andExpect(jsonPath("$.result.tools[?(@.name=='start_party')].annotations.destructiveHint").value(false))
                .andExpect(jsonPath("$.result.tools[?(@.name=='suggest_invitees')].annotations.readOnlyHint").value(true))
                .andExpect(jsonPath("$.result.tools[?(@.name=='invite_friend')].annotations.destructiveHint").value(false))
                .andExpect(jsonPath("$.result.tools[?(@.name=='add_user_by_email')].annotations.destructiveHint").value(false))
                .andExpect(jsonPath("$.result.tools[?(@.name=='get_join_link')].annotations.readOnlyHint").value(true))
                .andExpect(jsonPath("$.result.tools[?(@.name=='create_join_link')].annotations.readOnlyHint").value(false))
                .andExpect(jsonPath("$.result.tools[?(@.name=='create_join_link')].annotations.destructiveHint").value(true));
    }

    @Test
    void anOutsiderCanDoNothingWithAPartyThroughThePartyTools() throws Exception {
        signedInAs(7);
        when(partyRepository.countUserParticipations(5, 7)).thenReturn(0L);

        mockMvc.perform(callTool("suggest_invitees", "{\"partyId\":5}"))
                .andExpect(jsonPath("$.result.isError").value(true));
        mockMvc.perform(callTool("invite_friend", "{\"partyId\":5,\"userId\":8}"))
                .andExpect(jsonPath("$.result.isError").value(true));
        mockMvc.perform(callTool("add_user_by_email", "{\"partyId\":5,\"email\":\"make@example.com\"}"))
                .andExpect(jsonPath("$.result.isError").value(true));
        mockMvc.perform(callTool("get_join_link", "{\"partyId\":5}"))
                .andExpect(jsonPath("$.result.isError").value(true));
        mockMvc.perform(callTool("create_join_link", "{\"partyId\":5}"))
                .andExpect(jsonPath("$.result.isError").value(true));

        verifyNoInteractions(partyRoster, invitationSuggestions, joinLinks);
    }

    @Test
    void aMemberReplacesTheJoinLink() throws Exception {
        signedInAs(7);
        when(partyRepository.countUserParticipations(5, 7)).thenReturn(1L);
        PartyJoinLink link = new PartyJoinLink();
        link.setPartyId(5);
        link.setToken("fresh");
        link.setCreatedAt(Instant.parse("2026-11-15T20:00:00Z"));
        when(joinLinks.regenerate(5, 7)).thenReturn(link);

        mockMvc.perform(callTool("create_join_link", "{\"partyId\":5}"))
                .andExpect(jsonPath("$.result.isError").value(false))
                .andExpect(jsonPath("$.result.content[0].text", containsString("/join/fresh")));
    }

    void drinkToolsAreWritesAndOnlyUndoIsDestructive() throws Exception {
        mockMvc.perform(post("/mcp").with(jwt().jwt(jwt -> jwt.subject("matti@example.com")))
                        .contentType(MediaType.APPLICATION_JSON).header("Accept", JSON_AND_SSE).content(JSON_RPC_LIST_TOOLS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools[?(@.name=='log_drink')].annotations.readOnlyHint").value(false))
                .andExpect(jsonPath("$.result.tools[?(@.name=='log_drink')].annotations.destructiveHint").value(false))
                .andExpect(jsonPath("$.result.tools[?(@.name=='correct_drink')].annotations.destructiveHint").value(false))
                .andExpect(jsonPath("$.result.tools[?(@.name=='undo_drink')].annotations.destructiveHint").value(true));
    }

    @Test
    void anOutsiderLogsNoDrinkForAPartyMemberThroughTheirParty() throws Exception {
        signedInAs(7);
        when(partyRepository.countUserParticipations(3, 7)).thenReturn(0L);
        when(partyRepository.countUserParticipations(3, 8)).thenReturn(1L);

        mockMvc.perform(callTool("log_drink", "{\"partyId\":3,\"userId\":8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isError").value(true));

        verifyNoInteractions(drinkLog);
    }

    @Test
    void aMemberLogsADrinkForAPartyMate() throws Exception {
        signedInAs(7);
        when(partyRepository.countUserParticipations(3, 7)).thenReturn(1L);
        when(partyRepository.countUserParticipations(3, 8)).thenReturn(1L);
        Drink drink = new Drink();
        drink.setId(41);
        drink.setTimeStamp(Instant.parse("2026-11-15T20:00:00Z"));
        drink.setAlcohol(12f);
        when(drinkLog.recordInParty(eq(3), eq(8), any(), eq(12f))).thenReturn(drink);

        mockMvc.perform(callTool("log_drink", "{\"partyId\":3,\"userId\":8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isError").value(false))
                .andExpect(jsonPath("$.result.content[0].text").value(containsString("\"id\":41")))
                .andExpect(jsonPath("$.result.content[0].text").value(containsString("\"userId\":8")));
    }

    @Test
    void anOutsiderCorrectsAndUndoesNothingOfAPartyMate() throws Exception {
        signedInAs(7);
        when(partyRepository.countUserParticipations(3, 7)).thenReturn(0L);
        when(partyRepository.countUserParticipations(3, 8)).thenReturn(1L);

        mockMvc.perform(callTool("correct_drink", "{\"drinkId\":5,\"volumeMl\":500,\"alcoholPercent\":5,\"partyId\":3,\"userId\":8}"))
                .andExpect(jsonPath("$.result.isError").value(true));
        mockMvc.perform(callTool("undo_drink", "{\"drinkId\":5,\"partyId\":3,\"userId\":8}"))
                .andExpect(jsonPath("$.result.isError").value(true));

        verifyNoInteractions(drinkLog);
    }

    private void signedInAs(int userId) {
        User user = new User();
        user.setId(userId);
        when(userAccounts.byEmail("matti@example.com")).thenReturn(user);
    }

    private static MockHttpServletRequestBuilder callTool(String name, String arguments) {
        return post("/mcp").with(jwt().jwt(jwt -> jwt.subject("matti@example.com")))
                .contentType(MediaType.APPLICATION_JSON).header("Accept", JSON_AND_SSE)
                .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"" + name
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
