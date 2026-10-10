package drinkcounter.authentication;

import drinkcounter.DrinkCounterService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Evaluates each access annotation through Spring Security's {@code @PreAuthorize} interceptor,
 * signed in as user 42, with party membership answered by a mocked {@link DrinkCounterService}.
 */
public class AccessAnnotationsTest {

    static class Handlers {
        @PartyMember
        public void party(int partyId) {
        }

        @OwnUser
        public void ownUser(int userId) {
        }

        @OwnUserOrPartyMate
        public void ownUserOrPartyMate(int userId) {
        }

        @PartyMemberAndOwnUserOrPartyMate
        public void partyMemberAndOwnUserOrPartyMate(int partyId, int userId) {
        }
    }

    private DrinkCounterService drinkCounterService;
    private Handlers handlers;

    @BeforeEach
    public void setUp() {
        drinkCounterService = mock(DrinkCounterService.class);
        PartyAccess partyAccess = new PartyAccess(drinkCounterService);

        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        StaticApplicationContext beans = new StaticApplicationContext();
        beans.getBeanFactory().registerSingleton("partyAccess", partyAccess);
        expressionHandler.setApplicationContext(beans);
        PreAuthorizeAuthorizationManager manager = new PreAuthorizeAuthorizationManager();
        manager.setExpressionHandler(expressionHandler);

        ProxyFactory proxyFactory = new ProxyFactory(new Handlers());
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(AuthorizationManagerBeforeMethodInterceptor.preAuthorize(manager));
        handlers = (Handlers) proxyFactory.getProxy();

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        DrinkcounterUserDetails principal = new DrinkcounterUserDetails("user@example.com", "",
                true, true, true, true, authorities, 42);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void partyMemberAllowsParticipant() {
        when(drinkCounterService.isUserParticipant(1, 42)).thenReturn(true);

        assertDoesNotThrow(() -> handlers.party(1));
    }

    @Test
    public void partyMemberDeniesNonParticipant() {
        when(drinkCounterService.isUserParticipant(1, 42)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> handlers.party(1));
    }

    @Test
    public void ownUserAllowsSignedInUser() {
        assertDoesNotThrow(() -> handlers.ownUser(42));
    }

    @Test
    public void ownUserDeniesPartyMate() {
        when(drinkCounterService.shareParty(42, 7)).thenReturn(true);

        assertThrows(AccessDeniedException.class, () -> handlers.ownUser(7));
    }

    @Test
    public void ownUserOrPartyMateAllowsSignedInUser() {
        assertDoesNotThrow(() -> handlers.ownUserOrPartyMate(42));
    }

    @Test
    public void ownUserOrPartyMateAllowsPartyMate() {
        when(drinkCounterService.shareParty(42, 7)).thenReturn(true);

        assertDoesNotThrow(() -> handlers.ownUserOrPartyMate(7));
    }

    @Test
    public void ownUserOrPartyMateDeniesStranger() {
        when(drinkCounterService.shareParty(42, 7)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> handlers.ownUserOrPartyMate(7));
    }

    @Test
    public void partyMemberAndOwnUserOrPartyMateAllowsMemberActingOnThemself() {
        when(drinkCounterService.isUserParticipant(1, 42)).thenReturn(true);

        assertDoesNotThrow(() -> handlers.partyMemberAndOwnUserOrPartyMate(1, 42));
    }

    @Test
    public void partyMemberAndOwnUserOrPartyMateAllowsMemberActingOnPartyMate() {
        when(drinkCounterService.isUserParticipant(1, 42)).thenReturn(true);
        when(drinkCounterService.shareParty(42, 7)).thenReturn(true);

        assertDoesNotThrow(() -> handlers.partyMemberAndOwnUserOrPartyMate(1, 7));
    }

    @Test
    public void partyMemberAndOwnUserOrPartyMateDeniesNonParticipantEvenForPartyMate() {
        when(drinkCounterService.isUserParticipant(1, 42)).thenReturn(false);
        when(drinkCounterService.shareParty(42, 7)).thenReturn(true);

        assertThrows(AccessDeniedException.class, () -> handlers.partyMemberAndOwnUserOrPartyMate(1, 7));
        assertThrows(AccessDeniedException.class, () -> handlers.partyMemberAndOwnUserOrPartyMate(1, 42));
    }

    @Test
    public void partyMemberAndOwnUserOrPartyMateDeniesMemberActingOnStranger() {
        when(drinkCounterService.isUserParticipant(1, 42)).thenReturn(true);
        when(drinkCounterService.shareParty(42, 7)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> handlers.partyMemberAndOwnUserOrPartyMate(1, 7));
    }
}
