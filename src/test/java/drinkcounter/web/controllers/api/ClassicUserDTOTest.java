package drinkcounter.web.controllers.api;

import drinkcounter.model.Drink;
import drinkcounter.model.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClassicUserDTOTest {

    private static final Instant NOW = Instant.parse("2024-03-05T14:00:00Z");

    @Test
    public void idleIsSecondsSinceTheLatestDrink() {
        User user = new User();
        user.setId(1);
        user.drink(drinkAt(1, NOW.minusSeconds(3600)));
        user.drink(drinkAt(2, NOW.minusSeconds(90)));

        ClassicUserDTO dto = ClassicUserDTO.fromUser(user, Clock.fixed(NOW, ZoneOffset.UTC));

        assertEquals(90, dto.idle());
    }

    @Test
    public void idleIsZeroWithoutDrinks() {
        User user = new User();
        user.setId(1);

        assertEquals(0, ClassicUserDTO.fromUser(user, Clock.fixed(NOW, ZoneOffset.UTC)).idle());
    }

    private static Drink drinkAt(int id, Instant timeStamp) {
        Drink drink = new Drink();
        drink.setId(id);
        drink.setTimeStamp(timeStamp);
        return drink;
    }
}
