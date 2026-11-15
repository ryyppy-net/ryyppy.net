package drinkcounter.mcp;

import drinkcounter.DrinkLog;
import drinkcounter.UserAccounts;
import drinkcounter.alcoholcalculator.AlcoholCalculator;
import drinkcounter.authentication.PartyAccess;
import drinkcounter.model.Drink;
import java.time.Instant;
import java.util.Date;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * MCP tools that log, correct and undo drinks. Without a party they act on the signed-in user's own
 * drinks; with one they act on a member of it, as the party page does.
 */
@Component
public class DrinkTools {

    private final UserAccounts userAccounts;
    private final DrinkLog drinkLog;
    private final PartyAccess partyAccess;

    public DrinkTools(UserAccounts userAccounts, DrinkLog drinkLog, PartyAccess partyAccess) {
        this.userAccounts = userAccounts;
        this.drinkLog = drinkLog;
        this.partyAccess = partyAccess;
    }

    public record DrinkSummary(int id, int userId, Instant timestamp, float alcoholGrams) {

        static DrinkSummary of(Drink drink, int userId) {
            return new DrinkSummary(drink.getId(), userId, drink.getTimeStamp(), drink.getAlcohol());
        }
    }

    public record UndoneDrink(int drinkId, int userId) {
    }

    @McpTool(name = "log_drink", description = """
            Logs a drink. Without partyId it is logged for the signed-in user and may have any past time. \
            With partyId it is logged for a member of that party (userId, default the signed-in user), \
            who must also be a member; a party that has ended accepts no drinks. \
            A drink without volumeMl and alcoholPercent is a standard drink of 12 g of alcohol.""",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false))
    public DrinkSummary logDrink(
            @McpToolParam(description = "Party to log the drink through; omit to log for yourself", required = false) Integer partyId,
            @McpToolParam(description = "Id of the party member the drink is for; default is yourself", required = false) Integer userId,
            @McpToolParam(description = "Volume of the drink in millilitres; give together with alcoholPercent", required = false) Float volumeMl,
            @McpToolParam(description = "Alcohol by volume in percent; give together with volumeMl", required = false) Float alcoholPercent,
            @McpToolParam(description = "ISO-8601 instant when the drink was taken, such as 2026-11-15T20:30:00Z; default now", required = false) String timestamp) {
        int self = signedInUserId();
        int drinker = authorize(self, partyId, userId);
        float grams = volumeMl == null && alcoholPercent == null
                ? (float) AlcoholCalculator.STANDARD_DRINK_ALCOHOL_GRAMS
                : alcoholGrams(volumeMl, alcoholPercent);
        Date at = timestamp == null ? null : Date.from(Instant.parse(timestamp));
        Drink drink = partyId == null
                ? drinkLog.record(drinker, at, grams)
                : drinkLog.recordInParty(partyId, drinker, at, grams);
        return DrinkSummary.of(drink, drinker);
    }

    @McpTool(name = "correct_drink", description = """
            Changes the amount of alcohol in a drink already logged. Without partyId it is one of the \
            signed-in user's own drinks; with partyId it is a drink of a member of that party (userId, \
            default the signed-in user), who must also be a member.""",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = true))
    public DrinkSummary correctDrink(
            @McpToolParam(description = "Id of the drink to change") int drinkId,
            @McpToolParam(description = "Volume of the drink in millilitres") float volumeMl,
            @McpToolParam(description = "Alcohol by volume in percent") float alcoholPercent,
            @McpToolParam(description = "Party the drink's owner is a member of; omit for your own drink", required = false) Integer partyId,
            @McpToolParam(description = "Id of the party member the drink belongs to; default is yourself", required = false) Integer userId) {
        int drinker = authorize(signedInUserId(), partyId, userId);
        Drink drink = drinkLog.correctAlcohol(drinker, drinkId, alcoholGrams(volumeMl, alcoholPercent));
        return DrinkSummary.of(drink, drinker);
    }

    @McpTool(name = "undo_drink", description = """
            Deletes a drink that was logged. Without partyId it is one of the signed-in user's own \
            drinks; with partyId it is a drink of a member of that party (userId, default the signed-in \
            user), who must also be a member.""",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true, idempotentHint = false))
    public UndoneDrink undoDrink(
            @McpToolParam(description = "Id of the drink to delete") int drinkId,
            @McpToolParam(description = "Party the drink's owner is a member of; omit for your own drink", required = false) Integer partyId,
            @McpToolParam(description = "Id of the party member the drink belongs to; default is yourself", required = false) Integer userId) {
        int drinker = authorize(signedInUserId(), partyId, userId);
        drinkLog.undo(drinker, drinkId);
        return new UndoneDrink(drinkId, drinker);
    }

    /**
     * The user whose drink the call is about. Another user's drinks are reachable only through a
     * party that both the signed-in user and that user belong to.
     */
    private int authorize(int self, Integer partyId, Integer userId) {
        int drinker = userId == null ? self : userId;
        if (partyId == null) {
            if (drinker != self) {
                throw new AccessDeniedException("Another user's drinks can only be reached through a shared party");
            }
            return self;
        }
        if (!partyAccess.isMember(partyId, self) || !partyAccess.isMember(partyId, drinker)) {
            throw new AccessDeniedException("Both you and the drinker must be members of the party");
        }
        return drinker;
    }

    private static float alcoholGrams(Float volumeMl, Float alcoholPercent) {
        if (volumeMl == null || alcoholPercent == null) {
            throw new IllegalArgumentException("Give both volumeMl and alcoholPercent");
        }
        return AlcoholCalculator.getAlcoholAmount(volumeMl / 1000f, alcoholPercent / 100f);
    }

    private int signedInUserId() {
        // The token's subject is the email the user signed in with.
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userAccounts.byEmail(email).getId();
    }
}
