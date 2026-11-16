package drinkcounter.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import drinkcounter.InvitationSuggestions;
import drinkcounter.NearbyParties;
import drinkcounter.NearbyParty;
import drinkcounter.PartyDeletions;
import drinkcounter.PartyJoinLinks;
import drinkcounter.PartyLocationInvites;
import drinkcounter.PartyRoster;
import drinkcounter.Position;
import drinkcounter.RoughDistance;
import drinkcounter.UserAccounts;
import drinkcounter.authentication.PartyAccess;
import drinkcounter.authentication.relay.Origins;
import drinkcounter.model.Friend;
import drinkcounter.model.Party;
import drinkcounter.model.PartyJoinLink;
import drinkcounter.model.PartyLocationInvite;
import drinkcounter.model.User;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * MCP tools for the signed-in user's parties: starting one, getting people into it, sharing it
 * and joining someone else's. Each does what the REST API does for the same request.
 */
@Component
public class PartyTools {

    private final UserAccounts userAccounts;
    private final PartyRoster partyRoster;
    private final PartyAccess partyAccess;
    private final InvitationSuggestions invitationSuggestions;
    private final PartyJoinLinks joinLinks;
    private final PartyLocationInvites locationInvites;
    private final NearbyParties nearbyParties;
    private final PartyDeletions partyDeletions;
    private final HttpServletRequest request;

    public PartyTools(UserAccounts userAccounts, PartyRoster partyRoster, PartyAccess partyAccess,
            InvitationSuggestions invitationSuggestions, PartyJoinLinks joinLinks,
            PartyLocationInvites locationInvites, NearbyParties nearbyParties, PartyDeletions partyDeletions,
            HttpServletRequest request) {
        this.userAccounts = userAccounts;
        this.partyRoster = partyRoster;
        this.partyAccess = partyAccess;
        this.invitationSuggestions = invitationSuggestions;
        this.joinLinks = joinLinks;
        this.locationInvites = locationInvites;
        this.nearbyParties = nearbyParties;
        this.partyDeletions = partyDeletions;
        this.request = request;
    }

    public record PartySummary(int id, String name, Instant startTime, Instant endTime, boolean ended) {

        static PartySummary of(Party party, Instant now) {
            return new PartySummary(party.getId(), party.getName(), party.getStartTime(), party.getEndTime(),
                    party.hasEnded(now));
        }
    }

    public record ListedParty(int id, String name, Instant startTime, Instant endTime, boolean ended,
            boolean deletable) {
    }

    public record JoinLink(int partyId, String url, Instant createdAt) {
    }

    public record LocationInvite(double latitude, double longitude, int radiusM) {

        static LocationInvite of(PartyLocationInvite invite) {
            return new LocationInvite(invite.getLatitude(), invite.getLongitude(), invite.getRadiusM());
        }
    }

    /**
     * One person to get into a party. Give userId, email, a friend's name, or name with sex and
     * weight for a guest.
     */
    public record Person(
            @JsonProperty(required = false) @JsonPropertyDescription("A friend's name as the user says it, such as Matti or Liisa Virtanen") String name,
            @JsonProperty(required = false) @JsonPropertyDescription("The exact email address of a ryyppy.net user") String email,
            @JsonProperty(required = false) @JsonPropertyDescription("Id of a candidate returned by an earlier invite_to_party call") Integer userId,
            @JsonProperty(required = false) @JsonPropertyDescription("Guest only, together with name and weight") User.Sex sex,
            @JsonProperty(required = false) @JsonPropertyDescription("Guest only, in kilograms, together with name and sex") Float weight) {
    }

    public record Invited(int userId, String name, boolean guest, boolean alreadyInParty) {
    }

    public record Candidate(int userId, String name) {

        static Candidate of(Friend friend) {
            return new Candidate(friend.getId(), friend.getName());
        }
    }

    public record Ambiguous(String request, List<Candidate> candidates) {
    }

    public record Unmatched(String request, String reason, List<Candidate> closest) {
    }

    public record InviteReport(List<Invited> invited, List<Ambiguous> ambiguous, List<Unmatched> unmatched) {
    }

    public record StartedParty(PartySummary party, JoinLink joinLink, LocationInvite locationInvite,
            InviteReport invited) {
    }

    public record PartyInvite(PartySummary party, JoinLink joinLink, LocationInvite locationInvite) {
    }

    public record UpdatedParty(PartySummary party, LocationInvite locationInvite) {
    }

    public record NearbyPartySummary(int id, String name, Instant startTime, Instant endTime, RoughDistance distance,
            boolean alreadyMember) {
    }

    public record JoinOutcome(PartySummary joined, List<NearbyPartySummary> nearby) {
    }

    public record Removed(int partyId, int userId, String name) {
    }

    public record Deletion(boolean deleted, PartySummary party, List<String> otherUsers, List<String> guests) {
    }

    @McpTool(name = "list_parties", description = """
            Lists the parties the signed-in ryyppy.net user is a member of, newest first, \
            with each party's id, name, start and end time, whether it has ended, and whether the \
            user can delete it with delete_party.""",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false))
    public List<ListedParty> listParties() {
        Instant now = Instant.now();
        int self = signedInUser().getId();
        return partyRoster.allWithParticipantsOf(self).stream()
                .map(party -> new ListedParty(party.getId(), party.getName(), party.getStartTime(),
                        party.getEndTime(), party.hasEnded(now), PartyDeletions.allowed(party, self)))
                .toList();
    }

    @McpTool(name = "start_party", description = """
            Starts a party with the signed-in ryyppy.net user as its first member and returns it with a \
            join link to share. Optionally turns on a location invite, so people who are near the place \
            can find and join the party, and invites people straight away (see invite_to_party for how \
            each person is given). The party lasts days whole days (default 1) from now. A location is \
            coordinates: ryyppy.net does no geocoding, so work latitude and longitude out from the place \
            the user names. Give latitude, longitude and radiusM (100 to 5000 metres) together.""",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false))
    public StartedParty startParty(
            @McpToolParam(description = "The party's name") String name,
            @McpToolParam(description = "Length of the party in whole days, at least 1; default 1", required = false) Integer days,
            @McpToolParam(description = "Latitude of the party's place in degrees, between -90 and 90", required = false) Double latitude,
            @McpToolParam(description = "Longitude of the party's place in degrees, between -180 and 180", required = false) Double longitude,
            @McpToolParam(description = "How far from the place, in metres from 100 to 5000, someone can find and join the party", required = false) Integer radiusM,
            @McpToolParam(description = "People to invite as soon as the party exists", required = false) List<Person> invite) {
        int self = signedInUser().getId();
        boolean located = location(latitude, longitude, radiusM);
        Party party = partyRoster.start(name, days == null ? PartyRoster.DEFAULT_DAYS : days, self);
        int partyId = party.getId();
        partyRoster.join(partyId, self);
        LocationInvite place = located
                ? LocationInvite.of(locationInvites.set(partyId, latitude, longitude, radiusM, self))
                : null;
        JoinLink link = toJoinLink(joinLinks.regenerate(partyId, self));
        InviteReport report = invite == null || invite.isEmpty() ? null : invitePeople(self, partyId, invite);
        return new StartedParty(PartySummary.of(party, Instant.now()), link, place, report);
    }

    @McpTool(name = "invite_to_party", description = """
            Gets people into a party the signed-in ryyppy.net user is a member of. Each person is \
            given as one of: name alone, for a friend from the user's earlier parties; email, for a \
            ryyppy.net user, by their exact email address; or name with sex and weight, for a guest \
            who has no account and belongs to this party only. People who are already in the party \
            stay as they are. The result lists who was added, who could not be matched with the \
            closest friends' names, and who matched more than one friend with those candidates: ask \
            the user which one they mean, then call again giving that candidate's userId. Adding a \
            guest again creates another guest.""",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false))
    public InviteReport inviteToParty(
            @McpToolParam(description = "The party's id, from list_parties") int partyId,
            @McpToolParam(description = "The people to add") List<Person> people) {
        int self = requireMember(partyId);
        return invitePeople(self, partyId, people);
    }

    @McpTool(name = "get_party_invite", description = """
            What to share so others can join a party the signed-in ryyppy.net user is a member of: \
            the party's join link, which any signed-in user can open to join until the party ends, and \
            its location invite if it has one. Returns the current join link and creates one only if \
            the party has none. With replaceLink the link is replaced by a new one and the old link \
            stops working for everyone it was shared with: use that only when the user asks for it.""",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true, idempotentHint = false))
    public PartyInvite getPartyInvite(
            @McpToolParam(description = "The party's id, from list_parties") int partyId,
            @McpToolParam(description = "true to replace the join link with a new one; default false", required = false) Boolean replaceLink) {
        int self = requireMember(partyId);
        PartyJoinLink link = Boolean.TRUE.equals(replaceLink)
                ? joinLinks.regenerate(partyId, self)
                : joinLinks.current(partyId).orElseGet(() -> joinLinks.regenerate(partyId, self));
        return new PartyInvite(PartySummary.of(partyRoster.get(partyId), Instant.now()), toJoinLink(link),
                locationInvites.current(partyId).map(LocationInvite::of).orElse(null));
    }

    @McpTool(name = "join_party", description = """
            Joins the signed-in ryyppy.net user to a party, from a join link or from where they are. \
            Give joinLink, the link itself or just its token. Or give latitude and longitude: the tool \
            lists the parties whose location invite reaches that position and joins at once if exactly \
            one does; if several do, call again with the partyId of the one the user means. ryyppy.net \
            does no geocoding, so work coordinates out from where the user says they are. A link or \
            position that cannot be used gets one generic error, which does not say why. Joining a \
            party the user is already in changes nothing. The position is used for the distance check \
            only and is not stored.""",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = true))
    public JoinOutcome joinParty(
            @McpToolParam(description = "A party's join link, or its token", required = false) String joinLink,
            @McpToolParam(description = "Latitude of the user's position in degrees", required = false) Double latitude,
            @McpToolParam(description = "Longitude of the user's position in degrees", required = false) Double longitude,
            @McpToolParam(description = "With a position: the party to join, from an earlier result listing several", required = false) Integer partyId) {
        int self = signedInUser().getId();
        boolean positioned = latitude != null || longitude != null;
        if (joinLink != null && (positioned || partyId != null)) {
            throw new IllegalArgumentException("Give either joinLink or a position, not both");
        }
        if (joinLink != null) {
            return joined(joinLinks.join(tokenOf(joinLink), self));
        }
        Position position = positionOf(latitude, longitude);
        if (partyId != null) {
            return joined(nearbyParties.join(partyId, position, self));
        }
        List<NearbyParty> found = nearbyParties.around(position);
        if (found.size() == 1) {
            return joined(nearbyParties.join(found.get(0).party().getId(), position, self));
        }
        return new JoinOutcome(null, summaries(found, self));
    }

    @McpTool(name = "find_nearby_parties", description = """
            Finds the parties whose location invite reaches a position, nearest first, each with its \
            name, start and end time and a rough distance. It shows nothing of who is in them. Use it \
            for "is anyone partying near me?"; join_party joins one. ryyppy.net does no geocoding, so \
            work coordinates out from where the user says they are. The position is used for the \
            distance check only and is not stored.""",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false))
    public List<NearbyPartySummary> findNearbyParties(
            @McpToolParam(description = "Latitude of the user's position in degrees, between -90 and 90") double latitude,
            @McpToolParam(description = "Longitude of the user's position in degrees, between -180 and 180") double longitude) {
        int self = signedInUser().getId();
        return summaries(nearbyParties.around(new Position(latitude, longitude)), self);
    }

    @McpTool(name = "update_party", description = """
            Changes a party the signed-in ryyppy.net user is a member of: its length, its location \
            invite, or both. days sets the length in whole days from the party's start. Giving \
            latitude, longitude and radiusM (100 to 5000 metres) together turns the location invite on \
            or moves it, replacing the previous place; ryyppy.net does no geocoding, so work \
            coordinates out from the place the user names. stopLocationInvite turns the location \
            invite off. The join link is not touched.""",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true, idempotentHint = true))
    public UpdatedParty updateParty(
            @McpToolParam(description = "The party's id, from list_parties") int partyId,
            @McpToolParam(description = "New length of the party in whole days, at least 1", required = false) Integer days,
            @McpToolParam(description = "Latitude of the new place in degrees, between -90 and 90", required = false) Double latitude,
            @McpToolParam(description = "Longitude of the new place in degrees, between -180 and 180", required = false) Double longitude,
            @McpToolParam(description = "How far from the place, in metres from 100 to 5000, someone can find and join the party", required = false) Integer radiusM,
            @McpToolParam(description = "true to turn the location invite off", required = false) Boolean stopLocationInvite) {
        int self = requireMember(partyId);
        boolean stop = Boolean.TRUE.equals(stopLocationInvite);
        boolean located = location(latitude, longitude, radiusM);
        if (stop && located) {
            throw new IllegalArgumentException("Give a place or stopLocationInvite, not both");
        }
        if (days == null && !stop && !located) {
            throw new IllegalArgumentException("Give days, a place or stopLocationInvite");
        }
        Party party = days == null ? partyRoster.get(partyId) : partyRoster.changeDuration(partyId, days);
        if (stop) {
            locationInvites.revoke(partyId);
        } else if (located) {
            locationInvites.set(partyId, latitude, longitude, radiusM, self);
        }
        return new UpdatedParty(PartySummary.of(party, Instant.now()),
                locationInvites.current(partyId).map(LocationInvite::of).orElse(null));
    }

    @McpTool(name = "remove_participant", description = """
            Takes a participant, a user or a guest, out of a party the signed-in ryyppy.net user is a \
            member of. Any member can remove any participant, the signed-in user included. Removing a \
            user only takes them out of this party; their account and drinks stay. Ids come from \
            get_party_status.""",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true, idempotentHint = true))
    public Removed removeParticipant(
            @McpToolParam(description = "The party's id, from list_parties") int partyId,
            @McpToolParam(description = "The participant's user id, from get_party_status") int userId) {
        requireMember(partyId);
        if (!partyAccess.isMember(partyId, userId)) {
            throw new IllegalArgumentException("User " + userId + " is not in party " + partyId);
        }
        String name = userAccounts.get(userId).getName();
        partyRoster.leave(partyId, userId);
        return new Removed(partyId, userId, name);
    }

    @McpTool(name = "delete_party", description = """
            Deletes a party for everyone in it. The signed-in ryyppy.net user can delete a party \
            they started, or one where they are the only member with an account (list_parties says \
            which). Users' drinks stay in their own logs; the party's guests who are in no other \
            party are deleted with their drinks. It cannot be undone. A party with no other users in \
            it is deleted at once. If other users are in it, a call without confirm deletes nothing \
            and returns who would lose the party and which guests would be deleted: tell the user, \
            and call again with confirm only once they have agreed.""",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true, idempotentHint = false))
    public Deletion deleteParty(
            @McpToolParam(description = "The party's id, from list_parties") int partyId,
            @McpToolParam(description = "true once the user has agreed to delete a party other users are in; default false", required = false) Boolean confirm) {
        int self = requireMember(partyId);
        PartyDeletions.Preview preview = partyDeletions.preview(partyId, self);
        if (!preview.allowed()) {
            throw new AccessDeniedException("Only the person who started party " + partyId
                    + " can delete it while other users are in it");
        }
        boolean deleted = preview.otherUsers().isEmpty() || Boolean.TRUE.equals(confirm);
        if (deleted) {
            partyDeletions.delete(partyId);
        }
        return new Deletion(deleted, PartySummary.of(preview.party(), Instant.now()),
                preview.otherUsers().stream().map(User::getName).toList(),
                preview.guests().stream().map(User::getName).toList());
    }

    private InviteReport invitePeople(int self, int partyId, List<Person> people) {
        Invitations invitations = new Invitations(self, partyId);
        for (Person person : people) {
            invitations.add(person);
        }
        return invitations.report();
    }

    /** One invite call: who the people given turned out to be. */
    private final class Invitations {

        private final int self;
        private final int partyId;
        private final List<Invited> invited = new ArrayList<>();
        private final List<Ambiguous> ambiguous = new ArrayList<>();
        private final List<Unmatched> unmatched = new ArrayList<>();
        private List<Friend> friendsOutside;
        private List<Friend> friendsInside;

        Invitations(int self, int partyId) {
            this.self = self;
            this.partyId = partyId;
        }

        InviteReport report() {
            return new InviteReport(invited, ambiguous, unmatched);
        }

        void add(Person person) {
            String label = labelOf(person);
            if (person == null) {
                unmatched.add(new Unmatched(label, "Give a person as userId, email, name, or name with sex and weight", List.of()));
            } else if (person.userId() != null) {
                addFriendById(label, person.userId());
            } else if (person.email() != null && !person.email().isBlank()) {
                addByEmail(label, person.email());
            } else if (person.name() == null || person.name().isBlank()) {
                unmatched.add(new Unmatched(label, "Give userId, email, name, or name with sex and weight", List.of()));
            } else if (person.sex() != null || person.weight() != null) {
                addGuest(label, person);
            } else {
                addFriendByName(label, person.name());
            }
        }

        private void addFriendById(String label, int userId) {
            User user = userId == self || partyAccess.isPartyMate(self, userId) ? userAccounts.get(userId) : null;
            if (user == null) {
                unmatched.add(new Unmatched(label, "You share no party with that user", List.of()));
                return;
            }
            addUser(user);
        }

        private void addByEmail(String label, String email) {
            User user = userAccounts.byEmail(email);
            if (user == null) {
                unmatched.add(new Unmatched(label, "No ryyppy.net user has that email address", List.of()));
                return;
            }
            addUser(user);
        }

        private void addUser(User user) {
            boolean already = partyAccess.isMember(partyId, user.getId());
            if (!already) {
                partyRoster.join(partyId, user.getId());
            }
            invited.add(new Invited(user.getId(), user.getName(), false, already));
        }

        private void addGuest(String label, Person person) {
            if (person.sex() == null || person.weight() == null || !(person.weight() > 0)) {
                unmatched.add(new Unmatched(label, "A guest needs a name, a sex and a weight above 0", List.of()));
                return;
            }
            User guest = new User();
            guest.setName(person.name());
            guest.setSex(person.sex());
            guest.setWeight(person.weight());
            guest.setGuest(true);
            userAccounts.add(guest);
            partyRoster.join(partyId, guest.getId());
            invited.add(new Invited(guest.getId(), guest.getName(), true, false));
        }

        private void addFriendByName(String label, String name) {
            List<Friend> outside = friendsOutside();
            List<Friend> found = FriendNames.matching(name, outside);
            if (found.size() == 1) {
                addUser(userAccounts.get(found.get(0).getId()));
            } else if (found.size() > 1) {
                ambiguous.add(new Ambiguous(label, found.stream().map(Candidate::of).toList()));
            } else if (FriendNames.matching(name, friendsInside()).size() == 1) {
                Friend inside = FriendNames.matching(name, friendsInside()).get(0);
                invited.add(new Invited(inside.getId(), inside.getName(), false, true));
            } else {
                unmatched.add(new Unmatched(label, "No friend from your earlier parties has that name",
                        FriendNames.closest(name, outside).stream().map(Candidate::of).toList()));
            }
        }

        private List<Friend> friendsOutside() {
            if (friendsOutside == null) {
                Map<Integer, Friend> distinct = new LinkedHashMap<>();
                for (Friend friend : invitationSuggestions.forParty(self, partyId, Integer.MAX_VALUE)) {
                    distinct.putIfAbsent(friend.getId(), friend);
                }
                friendsOutside = List.copyOf(distinct.values());
            }
            return friendsOutside;
        }

        private List<Friend> friendsInside() {
            if (friendsInside == null) {
                friendsInside = partyRoster.members(partyId).stream()
                        .filter(member -> !member.isGuest())
                        .map(member -> new Friend(member.getId(), member.getName(), null))
                        .toList();
            }
            return friendsInside;
        }
    }

    private static String labelOf(Person person) {
        if (person == null) {
            return "";
        }
        if (person.name() != null && !person.name().isBlank()) {
            return person.name();
        }
        if (person.email() != null && !person.email().isBlank()) {
            return person.email();
        }
        return person.userId() == null ? "" : "user " + person.userId();
    }

    private static boolean location(Double latitude, Double longitude, Integer radiusM) {
        if (latitude == null && longitude == null && radiusM == null) {
            return false;
        }
        if (latitude == null || longitude == null || radiusM == null) {
            throw new IllegalArgumentException("A place needs latitude, longitude and radiusM together");
        }
        PartyLocationInvites.requireValid(latitude, longitude, radiusM);
        return true;
    }

    private static Position positionOf(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Give a joinLink, or latitude and longitude");
        }
        return new Position(latitude, longitude);
    }

    private static String tokenOf(String joinLink) {
        String token = joinLink.trim();
        int cut = token.indexOf('#');
        token = cut < 0 ? token : token.substring(0, cut);
        cut = token.indexOf('?');
        token = cut < 0 ? token : token.substring(0, cut);
        while (token.endsWith("/")) {
            token = token.substring(0, token.length() - 1);
        }
        return token.substring(token.lastIndexOf('/') + 1);
    }

    private JoinOutcome joined(Party party) {
        return new JoinOutcome(PartySummary.of(party, Instant.now()), List.of());
    }

    private List<NearbyPartySummary> summaries(List<NearbyParty> found, int userId) {
        return found.stream()
                .map(nearby -> new NearbyPartySummary(nearby.party().getId(), nearby.party().getName(),
                        nearby.party().getStartTime(), nearby.party().getEndTime(), nearby.distance(),
                        partyAccess.isMember(nearby.party().getId(), userId)))
                .toList();
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
