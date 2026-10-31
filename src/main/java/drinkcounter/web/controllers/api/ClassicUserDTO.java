package drinkcounter.web.controllers.api;

import drinkcounter.alcoholcalculator.BloodAlcohol;
import drinkcounter.model.User;
import java.time.Clock;
import java.time.Instant;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * The user XML the classic UI reads from /API/users/{id} and inside /API/parties/{id}.
 */
@JacksonXmlRootElement(localName = "user")
public record ClassicUserDTO(int id, String name, float alcoholInPromilles, int totalDrinks, long idle) {

    /** @param lastDrinkAt when the user last drank, or null if never */
    public static ClassicUserDTO fromUser(BloodAlcohol bloodAlcohol, Instant lastDrinkAt, Clock clock) {
        User user = bloodAlcohol.user();
        long idleSeconds = 0;
        if (lastDrinkAt != null) {
            idleSeconds = (clock.millis() - lastDrinkAt.toEpochMilli()) / 1000;
        }
        return new ClassicUserDTO(user.getId(), user.getName(), bloodAlcohol.promilles(), bloodAlcohol.drinksSinceSober(),
                idleSeconds);
    }
}
