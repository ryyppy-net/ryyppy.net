package drinkcounter.authentication;

/** Implemented by the principal of every login method; CurrentUser resolves the user through it. */
public interface LoggedInUserPrincipal {
    LoggedInUser getLoggedInUser();
}
