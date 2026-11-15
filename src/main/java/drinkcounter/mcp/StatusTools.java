package drinkcounter.mcp;

import drinkcounter.DrinkLog;
import drinkcounter.PartyRoster;
import drinkcounter.Promilles;
import drinkcounter.UserAccounts;
import drinkcounter.alcoholcalculator.BloodAlcohol;
import drinkcounter.authentication.PartyAccess;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import drinkcounter.web.controllers.api.v2.DrinkDTO;
import java.time.Instant;
import java.util.List;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** MCP tools that read the signed-in user's own status and drinks, and the status of their parties. */
@Component
public class StatusTools {

    private final UserAccounts userAccounts;
    private final PartyRoster partyRoster;
    private final PartyAccess partyAccess;
    private final Promilles promilles;
    private final DrinkLog drinkLog;

    public StatusTools(UserAccounts userAccounts, PartyRoster partyRoster, PartyAccess partyAccess,
            Promilles promilles, DrinkLog drinkLog) {
        this.userAccounts = userAccounts;
        this.partyRoster = partyRoster;
        this.partyAccess = partyAccess;
        this.promilles = promilles;
        this.drinkLog = drinkLog;
    }

    public record MyStatus(String name, float promilles, int drinksSinceSober) {
    }

    public record Participant(int id, String name, float promilles, int standardDrinks) {

        static Participant of(BloodAlcohol bloodAlcohol) {
            User user = bloodAlcohol.user();
            return new Participant(user.getId(), user.getName(), bloodAlcohol.promilles(), bloodAlcohol.standardDrinks());
        }
    }

    public record PartyStatus(int id, String name, Instant startTime, Instant endTime, boolean ended,
            Instant asOf, List<Participant> participants) {
    }

    @McpTool(name = "get_my_status", description = """
            The signed-in ryyppy.net user's current promilles and the number of standard drinks \
            they have had since they were last sober.""",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false))
    public MyStatus getMyStatus() {
        BloodAlcohol bloodAlcohol = promilles.of(signedInUser());
        return new MyStatus(bloodAlcohol.user().getName(), bloodAlcohol.promilles(), bloodAlcohol.drinksSinceSober());
    }

    @McpTool(name = "list_my_drinks", description = """
            Every drink the signed-in ryyppy.net user has logged, oldest first, with its id, \
            timestamp and size in standard drinks.""",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false))
    public List<DrinkDTO> listMyDrinks() {
        return drinkLog.drinksOf(signedInUser().getId()).stream().map(DrinkDTO::fromDrink).toList();
    }

    @McpTool(name = "get_party_status", description = """
            A party the signed-in ryyppy.net user is a member of: its name, start and end time and \
            each participant's promilles and standard drinks. Counts only drinks logged during the \
            party, as of now or the party's end, whichever is first. Use list_parties to find the id.""",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false))
    public PartyStatus getPartyStatus(@McpToolParam(description = "The party's id, from list_parties") int partyId) {
        User user = signedInUser();
        if (!partyAccess.isMember(partyId, user.getId())) {
            throw new IllegalArgumentException("No party " + partyId + " among your parties");
        }
        Party party = partyRoster.get(partyId);
        List<BloodAlcohol> readings = promilles.during(party, partyRoster.members(partyId));
        return new PartyStatus(party.getId(), party.getName(), party.getStartTime(), party.getEndTime(),
                party.hasEnded(Instant.now()), readings.isEmpty() ? party.getStartTime() : readings.get(0).now(),
                readings.stream().map(Participant::of).toList());
    }

    private User signedInUser() {
        // The token's subject is the email the user signed in with.
        return userAccounts.byEmail(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
