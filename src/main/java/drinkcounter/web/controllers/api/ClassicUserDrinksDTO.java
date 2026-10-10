package drinkcounter.web.controllers.api;

import drinkcounter.model.Drink;
import drinkcounter.model.User;
import java.util.List;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * The XML the classic UI reads from /API/users/{id}/show-drinks: the user's latest drinks, newest first.
 */
@JacksonXmlRootElement(localName = "user")
public record ClassicUserDrinksDTO(int id, DrinksDTO drinks) {

    private static final int LATEST_DRINKS = 11;

    public record DrinksDTO(
            int count,
            @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "drink")
            List<DrinkDTO> drink) {
    }

    public record DrinkDTO(int id, long timestamp) {
    }

    public static ClassicUserDrinksDTO fromUser(User user) {
        List<Drink> drinks = user.getDrinks();
        List<DrinkDTO> latest = drinks.reversed().stream()
                .limit(LATEST_DRINKS)
                .map(drink -> new DrinkDTO(drink.getId(), drink.getTimeStamp().toEpochMilli()))
                .toList();
        return new ClassicUserDrinksDTO(user.getId(), new DrinksDTO(drinks.size(), latest));
    }
}
