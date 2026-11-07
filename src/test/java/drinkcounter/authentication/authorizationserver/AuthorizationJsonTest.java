package drinkcounter.authentication.authorizationserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import drinkcounter.authentication.DrinkcounterUserDetails;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import tools.jackson.databind.json.JsonMapper;

public class AuthorizationJsonTest {

    private final JsonMapper mapper = AuthorizationJson.mapper();

    @Test
    public void signedInUserSurvivesTheRoundTripWithTheirUserId() {
        String json = mapper.writeValueAsString(attributes(signedIn("ville@example.com", "$2a$10$hash", 42)));

        Object principal = ((UsernamePasswordAuthenticationToken) read(json).get(Principal.class.getName())).getPrincipal();

        DrinkcounterUserDetails details = assertInstanceOf(DrinkcounterUserDetails.class, principal);
        assertEquals(42, details.getUserId());
        assertEquals("ville@example.com", details.getUsername());
        assertNull(details.getPassword());
    }

    @Test
    public void passwordHashIsNotWritten() {
        String json = mapper.writeValueAsString(attributes(signedIn("ville@example.com", "$2a$10$hash", 42)));

        assertFalse(json.contains("$2a$10$hash"), json);
    }

    private static UsernamePasswordAuthenticationToken signedIn(String email, String password, int userId) {
        Set<SimpleGrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("ROLE_USER"));
        DrinkcounterUserDetails details = new DrinkcounterUserDetails(email, password, true, true, true, true, authorities, userId);
        return UsernamePasswordAuthenticationToken.authenticated(details, null, authorities);
    }

    private static Map<String, Object> attributes(UsernamePasswordAuthenticationToken principal) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(Principal.class.getName(), principal);
        return attributes;
    }

    private Map<String, Object> read(String json) {
        return mapper.readValue(json, mapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));
    }
}
