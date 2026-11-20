package drinkcounter.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationConsentAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AuthorizationCodeRequestAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AuthorizationConsentAuthenticationConverter;

class DefaultScopeAuthorizationRequestConverterTest {

    private static final String CLIENT_ID = "https://client.example/metadata.json";

    private final DefaultScopeAuthorizationRequestConverter authorizationRequests =
            new DefaultScopeAuthorizationRequestConverter(new OAuth2AuthorizationCodeRequestAuthenticationConverter());

    @BeforeEach
    void setAuthorizationServerContext() {
        AuthorizationServerSettings settings = AuthorizationServerSettings.builder().build();
        AuthorizationServerContextHolder.setContext(new AuthorizationServerContext() {
            @Override
            public String getIssuer() {
                return "https://ryyppy.net";
            }

            @Override
            public AuthorizationServerSettings getAuthorizationServerSettings() {
                return settings;
            }
        });
    }

    @AfterEach
    void resetAuthorizationServerContext() {
        AuthorizationServerContextHolder.resetContext();
    }

    @Test
    void aRequestThatNamesNoScopeAsksForTheMcpScope() {
        Authentication converted = authorizationRequests.convert(authorizationRequest());

        OAuth2AuthorizationCodeRequestAuthenticationToken request = (OAuth2AuthorizationCodeRequestAuthenticationToken) converted;
        assertThat(request.getScopes()).containsExactly("mcp");
        assertThat(request.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(request.getRedirectUri()).isEqualTo("http://localhost:4567/callback");
        assertThat(request.getState()).isEqualTo("client-state");
        assertThat(request.getAdditionalParameters()).containsEntry("code_challenge", "challenge");
    }

    @Test
    void aRequestThatNamesAScopeKeepsIt() {
        OAuth2AuthorizationCodeRequestAuthenticationToken request = (OAuth2AuthorizationCodeRequestAuthenticationToken)
                authorizationRequests.convert(authorizationRequest("scope", "mcp"));

        assertThat(request.getScopes()).isEqualTo(Set.of("mcp"));
    }

    @Test
    void aDeniedConsentStaysWithoutScopes() {
        DefaultScopeAuthorizationRequestConverter consents =
                new DefaultScopeAuthorizationRequestConverter(new OAuth2AuthorizationConsentAuthenticationConverter());
        MockHttpServletRequest httpRequest = new MockHttpServletRequest("POST", "/oauth2/authorize");
        httpRequest.setParameter("client_id", CLIENT_ID);
        httpRequest.setParameter("state", "consent-state");

        OAuth2AuthorizationConsentAuthenticationToken consent =
                (OAuth2AuthorizationConsentAuthenticationToken) consents.convert(httpRequest);

        assertThat(consent.getScopes()).isEmpty();
    }

    private static MockHttpServletRequest authorizationRequest(String... extraParameters) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorize");
        List<String> parameters = new ArrayList<>(List.of("response_type", "code", "client_id", CLIENT_ID,
                "redirect_uri", "http://localhost:4567/callback", "state", "client-state",
                "code_challenge", "challenge", "code_challenge_method", "S256"));
        parameters.addAll(List.of(extraParameters));
        StringJoiner query = new StringJoiner("&");
        for (int i = 0; i < parameters.size(); i += 2) {
            request.setParameter(parameters.get(i), parameters.get(i + 1));
            query.add(parameters.get(i) + "=" + URLEncoder.encode(parameters.get(i + 1), StandardCharsets.UTF_8));
        }
        request.setQueryString(query.toString());
        return request;
    }
}
