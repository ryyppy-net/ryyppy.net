package drinkcounter.authentication;

import drinkcounter.model.Party;
import drinkcounter.model.User;
import drinkcounter.repository.PartyRepository;
import java.util.List;
import java.util.Optional;
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
 * signed in as user 42, with party membership answered by a mocked {@link PartyRepository}.
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

        @PartyStarterOrOnlyUser
        public void deleteParty(int partyId) {
        }
    }

    private PartyRepository partyRepository;
    private Handlers handlers;

    @BeforeEach
    public void setUp() {
        partyRepository = mock(PartyRepository.class);
        PartyAccess partyAccess = new PartyAccess(partyRepository);

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
        when(partyRepository.countUserParticipations(1, 42)).thenReturn(1L);

        assertDoesNotThrow(() -> handlers.party(1));
    }

    @Test
    public void partyMemberDeniesNonParticipant() {
        when(partyRepository.countUserParticipations(1, 42)).thenReturn(0L);

        assertThrows(AccessDeniedException.class, () -> handlers.party(1));
    }

    @Test
    public void ownUserAllowsSignedInUser() {
        assertDoesNotThrow(() -> handlers.ownUser(42));
    }

    @Test
    public void ownUserDeniesPartyMate() {
        when(partyRepository.countSharedParties(42, 7)).thenReturn(1L);

        assertThrows(AccessDeniedException.class, () -> handlers.ownUser(7));
    }

    @Test
    public void ownUserOrPartyMateAllowsSignedInUser() {
        assertDoesNotThrow(() -> handlers.ownUserOrPartyMate(42));
    }

    @Test
    public void ownUserOrPartyMateAllowsPartyMate() {
        when(partyRepository.countSharedParties(42, 7)).thenReturn(1L);

        assertDoesNotThrow(() -> handlers.ownUserOrPartyMate(7));
    }

    @Test
    public void ownUserOrPartyMateDeniesStranger() {
        when(partyRepository.countSharedParties(42, 7)).thenReturn(0L);

        assertThrows(AccessDeniedException.class, () -> handlers.ownUserOrPartyMate(7));
    }

    @Test
    public void partyMemberAndOwnUserOrPartyMateAllowsMemberActingOnThemself() {
        when(partyRepository.countUserParticipations(1, 42)).thenReturn(1L);

        assertDoesNotThrow(() -> handlers.partyMemberAndOwnUserOrPartyMate(1, 42));
    }

    @Test
    public void partyMemberAndOwnUserOrPartyMateAllowsMemberActingOnPartyMate() {
        when(partyRepository.countUserParticipations(1, 42)).thenReturn(1L);
        when(partyRepository.countSharedParties(42, 7)).thenReturn(1L);

        assertDoesNotThrow(() -> handlers.partyMemberAndOwnUserOrPartyMate(1, 7));
    }

    @Test
    public void partyMemberAndOwnUserOrPartyMateDeniesNonParticipantEvenForPartyMate() {
        when(partyRepository.countUserParticipations(1, 42)).thenReturn(0L);
        when(partyRepository.countSharedParties(42, 7)).thenReturn(1L);

        assertThrows(AccessDeniedException.class, () -> handlers.partyMemberAndOwnUserOrPartyMate(1, 7));
        assertThrows(AccessDeniedException.class, () -> handlers.partyMemberAndOwnUserOrPartyMate(1, 42));
    }

    @Test
    public void partyMemberAndOwnUserOrPartyMateDeniesMemberActingOnStranger() {
        when(partyRepository.countUserParticipations(1, 42)).thenReturn(1L);
        when(partyRepository.countSharedParties(42, 7)).thenReturn(0L);

        assertThrows(AccessDeniedException.class, () -> handlers.partyMemberAndOwnUserOrPartyMate(1, 7));
    }

    @Test
    public void partyStarterOrOnlyUserAllowsTheStarter() {
        Party party = partyWith(42, 7);
        party.setStartedBy(42);
        when(partyRepository.findById(1)).thenReturn(Optional.of(party));

        assertDoesNotThrow(() -> handlers.deleteParty(1));
    }

    @Test
    public void partyStarterOrOnlyUserAllowsTheOnlyUserLeft() {
        when(partyRepository.findById(1)).thenReturn(Optional.of(partyWith(42)));

        assertDoesNotThrow(() -> handlers.deleteParty(1));
    }

    @Test
    public void partyStarterOrOnlyUserDeniesAnotherMemberWhileOtherUsersAreIn() {
        Party party = partyWith(42, 7);
        party.setStartedBy(7);
        when(partyRepository.findById(1)).thenReturn(Optional.of(party));

        assertThrows(AccessDeniedException.class, () -> handlers.deleteParty(1));
    }

    @Test
    public void partyStarterOrOnlyUserDeniesNonParticipantAndUnknownParty() {
        Party party = partyWith(7);
        party.setStartedBy(42);
        when(partyRepository.findById(1)).thenReturn(Optional.of(party));
        when(partyRepository.findById(2)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> handlers.deleteParty(1));
        assertThrows(AccessDeniedException.class, () -> handlers.deleteParty(2));
    }

    private static Party partyWith(int... userIds) {
        Party party = new Party();
        for (int userId : userIds) {
            User user = new User();
            user.setId(userId);
            party.addParticipant(user);
        }
        return party;
    }
}
