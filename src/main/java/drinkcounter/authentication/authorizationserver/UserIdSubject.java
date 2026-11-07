package drinkcounter.authentication.authorizationserver;

import drinkcounter.authentication.DrinkcounterUserDetails;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

/** Makes an access token's subject the user's id, which unlike their email never changes. */
public class UserIdSubject implements OAuth2TokenCustomizer<JwtEncodingContext> {

    @Override
    public void customize(JwtEncodingContext context) {
        if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())
                && context.getPrincipal().getPrincipal() instanceof DrinkcounterUserDetails user) {
            context.getClaims().subject(String.valueOf(user.getUserId()));
        }
    }
}
