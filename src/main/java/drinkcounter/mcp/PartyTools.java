package drinkcounter.mcp;

import drinkcounter.InvitationSuggestions;
import drinkcounter.PartyJoinLinks;
import drinkcounter.PartyRoster;
import drinkcounter.UserAccounts;
import drinkcounter.authentication.PartyAccess;
import drinkcounter.authentication.relay.Origins;
import drinkcounter.model.Friend;
import drinkcounter.model.Party;
import drinkcounter.model.PartyJoinLink;
import drinkcounter.model.User;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** MCP tools that list the signed-in user's parties, start one and get friends into it. */
@Component
public class PartyTools {

    private final UserAccounts userAccounts;
    private final PartyRoster partyRoster;
    private final PartyAccess partyAccess;
    private final InvitationSuggestions invitationSuggestions;
    private final PartyJoinLinks joinLinks;
    private final HttpServletRequest request;

    public PartyTools(UserAccounts userAccounts, PartyRoster partyRoster, PartyAccess partyAccess,
            InvitationSuggestions invitationSuggestions, PartyJoinLinks joinLinks, HttpServletRequest request) {
        this.userAccounts = userAccounts;
        this.partyRoster = partyRoster;
        this.partyAccess = partyAccess;
        this.invitationSuggestions = invitationSuggestions;
        this.joinLinks = joinLinks;
        this.request = request;
    }

    public record PartySummary(int id, String name, Instant startTime, Instant endTime, boolean ended) {

        static PartySummary of(Party party, Instant now) {
            return new PartySummary(party.getId(), party.getName(), party.getStartTime(), party.getEndTime(),
                    party.hasEnded(now));
        }
    }

    public record Invitee(int id, String name) {

        static Invitee of(Friend friend) {
            return new Invitee(friend.getId(), friend.getName());
        }
    }

    public record JoinLink(int partyId, String url, Instant createdAt) {
    }

    public record Added(int partyId, int userId, String name) {
    }

    @McpTool(name = "list_parties", description = """
            Lists the parties the signed-in ryyppy.net user is a member of, newest first, \
            with each party's id, name, start and end time, and whether it has ended.""",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false))
    public List<PartySummary> listParties() {
        Instant now = Instant.now();
        return partyRoster.newestOf(signedInUser().getId(), Integer.MAX_VALUE).stream()
                .map(party -> PartySummary.of(party, now))
                .toList();
    }

    @McpTool(name = "start_party", description = """
            Starts a party and adds the signed-in ryyppy.net user to it as its first member. \
            It lasts days whole days (default 1) from now.""",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false))
    public PartySummary startParty(
            @McpToolParam(description = "The party's name") String name,
            @McpToolParam(description = "Length of the party in whole days, at least 1; default 1", required = false) Integer days) {
        int self = signedInUser().getId();
        Party party = days == null ? partyRoster.start(name) : partyRoster.start(name, days);
        partyRoster.join(party.getId(), self);
        return PartySummary.of(party, Instant.now());
    }

    @McpTool(name = "suggest_invitees", description = """
            Friends of the signed-in ryyppy.net user from their earlier parties who are not yet in \
            the given party, most recent party first. Use invite_friend to add one.""",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false))
    public List<Invitee> suggestInvitees(
            @McpToolParam(description = "The party's id, from list_parties") int partyId,
            @McpToolParam(description = "How many friends to suggest; default 10", required = false) Integer amount) {
        int self = requireMember(partyId);
        return invitationSuggestions.forParty(self, partyId, amount == null ? 10 : amount).stream()
                .map(Invitee::of)
                .toList();
    }

    @McpTool(name = "invite_friend", description = """
            Adds a friend to a party the signed-in ryyppy.net user is a member of. The friend must \
            share a party with the signed-in user; ids come from suggest_invitees. Adding someone \
            who is already in the party changes nothing.""",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = true))
    public Added inviteFriend(
            @McpToolParam(description = "The party's id, from list_parties") int partyId,
            @McpToolParam(description = "The friend's user id, from suggest_invitees") int userId) {
        int self = requireMember(partyId);
        if (userId != self && !partyAccess.isPartyMate(self, userId)) {
            throw new AccessDeniedException("You can only invite people you share a party with");
        }
        partyRoster.join(partyId, userId);
        return new Added(partyId, userId, userAccounts.get(userId).getName());
    }

    @McpTool(name = "add_user_by_email", description = """
            Adds an existing ryyppy.net user, found by their exact email address, to a party the \
            signed-in user is a member of. Adding someone who is already in the party changes nothing.""",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = true))
    public Added addUserByEmail(
            @McpToolParam(description = "The party's id, from list_parties") int partyId,
            @McpToolParam(description = "The user's email address") String email) {
        requireMember(partyId);
        User user = userAccounts.byEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("No ryyppy.net user has that email address");
        }
        partyRoster.join(partyId, user.getId());
        return new Added(partyId, user.getId(), user.getName());
    }

    @McpTool(name = "get_join_link", description = """
            The party's current join link, which any signed-in ryyppy.net user can open to join \
            until the party ends. Changes nothing; fails if the party has no link, in which case \
            create_join_link makes one.""",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false))
    public JoinLink getJoinLink(@McpToolParam(description = "The party's id, from list_parties") int partyId) {
        requireMember(partyId);
        PartyJoinLink link = joinLinks.current(partyId)
                .orElseThrow(() -> new IllegalArgumentException("Party " + partyId + " has no join link"));
        return toJoinLink(link);
    }

    @McpTool(name = "create_join_link", description = """
            Creates the party's join link, or replaces the current one: the old link stops working \
            for everyone it was shared with. Use get_join_link to show the current link without \
            replacing it.""",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true, idempotentHint = false))
    public JoinLink createJoinLink(@McpToolParam(description = "The party's id, from list_parties") int partyId) {
        int self = requireMember(partyId);
        return toJoinLink(joinLinks.regenerate(partyId, self));
    }

    private int requireMember(int partyId) {
        int self = signedInUser().getId();
        if (!partyAccess.isMember(partyId, self)) {
            throw new AccessDeniedException("No party " + partyId + " among your parties");
        }
        return self;
    }

    private JoinLink toJoinLink(PartyJoinLink link) {
        return new JoinLink(link.getPartyId(), Origins.of(request) + "/join/" + link.getToken(), link.getCreatedAt());
    }

    private User signedInUser() {
        // The token's subject is the email the user signed in with.
        return userAccounts.byEmail(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
