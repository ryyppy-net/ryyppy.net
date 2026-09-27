package drinkcounter.authentication;

/**
 * The authenticated principal for every login method: form login, Google One Tap,
 * the auth relay and OAuth2/OIDC.
 */
public interface DrinkcounterPrincipal {
    int getUserId();
}
