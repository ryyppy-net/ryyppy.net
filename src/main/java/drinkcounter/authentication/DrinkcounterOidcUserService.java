package drinkcounter.authentication;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.util.StringUtils;

/**
 * Carries the user id that {@link CustomOAuth2UserService} puts into the user info over
 * to the OIDC principal.
 */
public class DrinkcounterOidcUserService extends OidcUserService {

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        Integer userId = oidcUser.getAttribute(CustomOAuth2UserService.USER_ID_ATTRIBUTE);
        if (userId == null) {
            return oidcUser;
        }
        String nameAttributeKey = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();
        if (!StringUtils.hasText(nameAttributeKey)) {
            nameAttributeKey = IdTokenClaimNames.SUB;
        }
        return new DrinkcounterOidcUser(oidcUser.getAuthorities(), oidcUser.getIdToken(), oidcUser.getUserInfo(),
                nameAttributeKey, userId);
    }
}
