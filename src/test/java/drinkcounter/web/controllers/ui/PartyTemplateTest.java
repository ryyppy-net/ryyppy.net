package drinkcounter.web.controllers.ui;

import drinkcounter.model.Party;
import drinkcounter.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Renders party.html directly (no full Spring context, no datasource), the
 * same way LoginTemplateTest and ErrorTemplateTest do for their pages.
 * party.html's riskiest piece is the kick dialog's self-exclusion guard
 * (${participant.id != user.id}) - without it a user could remove
 * themselves from their own party, which is exactly the /ui/viewParty bug
 * that #92 deleted (see #111) - so it gets direct coverage here rather than
 * relying only on the e2e suite.
 */
class PartyTemplateTest {

    private static User user(int id, String name) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        return u;
    }

    private static String render(Party party, User currentUser) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("messages", "drinkcounter.version");
        messageSource.setDefaultEncoding("UTF-8");

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.setTemplateEngineMessageSource(messageSource);

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse response = new MockHttpServletResponse();
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        WebContext context = new WebContext(webApplication.buildExchange(request, response), Locale.of("fi", "FI"));
        context.setVariable("party", party);
        context.setVariable("user", currentUser);

        return engine.process("party", context);
    }

    @Test
    void titleAndPartyIdAreProjectedFromModel() {
        Party party = new Party();
        party.setId(77);
        party.setName("Kattoparty");
        User owner = user(1, "Omistaja");
        party.addParticipant(owner);

        String html = render(party, owner);

        assertTrue(html.contains("<title>Kattoparty</title>"), "party.name wasn't projected into the master layout's <title>");
        assertTrue(html.contains("var partyId = 77;"), "party.id wasn't inlined into the page's <script> block for party.js/partygraph.js");
        assertFalse(html.contains("${"), "unresolved Thymeleaf expression leaked into output");
        assertFalse(html.contains("customHead"), "the customHead/content wrapper divs should be removed by th:remove=\"tag\"");
    }

    @Test
    void kickDialogListsOtherParticipantsButNotTheCurrentUser() {
        Party party = new Party();
        party.setId(5);
        party.setName("Kickable Party");
        User owner = user(1, "Isäntä");
        User guest = user(2, "Vieras Ville");
        party.setParticipants(List.of(owner, guest));

        String html = render(party, owner);

        assertTrue(html.contains("Vieras Ville"), "the other participant should be listed in the kick dialog");
        assertFalse(html.contains(">Isäntä<"), "the current user must never appear in their own kick dialog - see #111/#92");
    }

    @Test
    void kickConfirmDataAttributeCarriesTheParticipantNameBetweenBothMessages() {
        Party party = new Party();
        party.setId(5);
        party.setName("Kickable Party");
        User owner = user(1, "Isäntä");
        User guest = user(2, "Vieras Ville");
        party.setParticipants(List.of(owner, guest));

        String html = render(party, owner);

        assertTrue(html.contains("data-user-id=\"2\""), "the kick link should carry the participant's id for party.js to read");
        assertTrue(html.contains("data-confirm=\"Poistetaanko Vieras Ville bileistä?\""),
                "party.confirm.remove_user and party.confirm.from_party should sandwich the participant's name in one data-confirm attribute");
    }

    @Test
    void fewerThanTwoParticipantsShowsNoUsersMessageInsteadOfTheList() {
        Party party = new Party();
        party.setId(9);
        party.setName("Solo Party");
        User owner = user(1, "Yksin");
        party.setParticipants(List.of(owner));

        String html = render(party, owner);

        assertTrue(html.contains("Ei potkittavia juojia"), "party.no_users should render when fewer than two participants exist");
        assertFalse(html.contains("<ul>"), "the participant list should not render alongside the no-users message");
    }
}
