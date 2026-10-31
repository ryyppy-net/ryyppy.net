package drinkcounter.authentication;

import drinkcounter.UserAccounts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

public class GoogleIdentityLinkingServiceTest {

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void establishSessionSignsInWithinTheVisitorsExistingSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("partyJoin.token", "AbCdEfGhIjKlMnOpQrSt-_");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);

        new GoogleIdentityLinkingService(mock(UserAccounts.class)).establishSession(42, "user@example.com", request);

        assertSame(session, request.getSession(false));
        assertEquals("AbCdEfGhIjKlMnOpQrSt-_", session.getAttribute("partyJoin.token"));
        SecurityContext context = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        DrinkcounterUserDetails principal = assertInstanceOf(DrinkcounterUserDetails.class, context.getAuthentication().getPrincipal());
        assertEquals(42, principal.getUserId());
    }
}
