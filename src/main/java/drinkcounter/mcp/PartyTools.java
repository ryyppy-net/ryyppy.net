package drinkcounter.mcp;

import drinkcounter.PartyRoster;
import drinkcounter.UserAccounts;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import java.time.Instant;
import java.util.List;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** MCP tools about the signed-in user's parties. */
@Component
public class PartyTools {

    private final UserAccounts userAccounts;
    private final PartyRoster partyRoster;

    public PartyTools(UserAccounts userAccounts, PartyRoster partyRoster) {
        this.userAccounts = userAccounts;
        this.partyRoster = partyRoster;
    }

    public record PartySummary(int id, String name, Instant startTime, Instant endTime, boolean ended) {

        static PartySummary of(Party party, Instant now) {
            return new PartySummary(party.getId(), party.getName(), party.getStartTime(), party.getEndTime(),
                    party.hasEnded(now));
        }
    }

    @McpTool(name = "list_parties", description = """
            Lists the parties the signed-in ryyppy.net user is a member of, newest first, \
            with each party's id, name, start and end time, and whether it has ended.""")
    public List<PartySummary> listParties() {
        // The token's subject is the email the user signed in with.
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userAccounts.byEmail(email);
        Instant now = Instant.now();
        return partyRoster.newestOf(user.getId(), Integer.MAX_VALUE).stream()
                .map(party -> PartySummary.of(party, now))
                .toList();
    }
}
