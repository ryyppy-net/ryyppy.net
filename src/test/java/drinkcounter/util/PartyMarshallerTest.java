package drinkcounter.util;

import drinkcounter.UserService;
import drinkcounter.model.Drink;
import drinkcounter.model.User;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PartyMarshallerTest {

    @Test
    public void marshallDrinksWritesDrinkTimestampAsEpochMillis() throws Exception {
        Instant timeStamp = Instant.parse("2024-03-05T13:37:42.123Z");

        Drink drink = new Drink();
        drink.setId(7);
        drink.setTimeStamp(timeStamp);

        User user = new User();
        user.setId(1);
        user.drink(drink);

        UserService userService = mock(UserService.class);
        when(userService.getUser(1)).thenReturn(user);

        PartyMarshaller marshaller = new PartyMarshaller();
        ReflectionTestUtils.setField(marshaller, "userService", userService);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshaller.marshallDrinks(1, out);

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(out.toByteArray()));

        NodeList timestamps = doc.getElementsByTagName("timestamp");
        assertEquals(1, timestamps.getLength());
        assertEquals(Long.toString(timeStamp.toEpochMilli()), timestamps.item(0).getTextContent());
    }
}
