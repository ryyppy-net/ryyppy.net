package drinkcounter.web.controllers.api;

import drinkcounter.model.Drink;
import drinkcounter.model.User;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.dataformat.xml.XmlMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClassicUserDrinksDTOTest {

    private final XmlMapper xml = new XmlMapper();

    @Test
    public void drinkTimestampIsEpochMillis() throws Exception {
        Instant timeStamp = Instant.parse("2024-03-05T13:37:42.123Z");
        Drink drink = new Drink();
        drink.setId(7);
        drink.setTimeStamp(timeStamp);
        User user = new User();
        user.setId(1);
        user.drink(drink);

        String out = xml.writeValueAsString(ClassicUserDrinksDTO.fromUser(user));

        assertEquals("<user><id>1</id><drinks><count>1</count><drink><id>7</id><timestamp>"
                + timeStamp.toEpochMilli() + "</timestamp></drink></drinks></user>", out);
    }

    @Test
    public void listsOnlyTheLatestDrinksNewestFirst() throws Exception {
        User user = new User();
        user.setId(1);
        for (int i = 1; i <= 15; i++) {
            Drink drink = new Drink();
            drink.setId(i);
            drink.setTimeStamp(Instant.ofEpochSecond(1_000_000 + i));
            user.drink(drink);
        }

        ClassicUserDrinksDTO dto = ClassicUserDrinksDTO.fromUser(user);

        assertEquals(15, dto.drinks().count());
        assertEquals(11, dto.drinks().drink().size());
        assertEquals(15, dto.drinks().drink().get(0).id());
        assertEquals(5, dto.drinks().drink().get(10).id());
    }
}
