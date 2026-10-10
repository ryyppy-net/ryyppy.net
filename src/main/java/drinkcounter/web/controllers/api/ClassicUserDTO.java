package drinkcounter.web.controllers.api;

import drinkcounter.model.Drink;
import drinkcounter.model.User;
import java.time.Clock;
import java.util.List;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * The user XML the classic UI reads from /API/users/{id} and inside /API/parties/{id}.
 */
@JacksonXmlRootElement(localName = "user")
public record ClassicUserDTO(int id, String name, float alcoholInPromilles, int totalDrinks, long idle) {

    public static ClassicUserDTO fromUser(User user, Clock clock) {
        List<Drink> drinks = user.getDrinks();
        long idleSeconds = 0;
        if (!drinks.isEmpty()) {
            Drink lastDrink = drinks.get(drinks.size() - 1);
            idleSeconds = (clock.millis() - lastDrink.getTimeStamp().toEpochMilli()) / 1000;
        }
        return new ClassicUserDTO(user.getId(), user.getName(), user.getPromilles(), user.getTotalDrinks(),
                idleSeconds);
    }
}
