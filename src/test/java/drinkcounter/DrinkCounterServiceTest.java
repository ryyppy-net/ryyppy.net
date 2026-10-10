package drinkcounter;

import drinkcounter.dao.DrinkDAO;
import drinkcounter.dao.UserDAO;
import drinkcounter.model.Drink;
import drinkcounter.model.User;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DrinkCounterServiceTest {

    private DrinkCounterService service;
    private UserDAO userDAO;
    private User user;
    private DrinkDAO drinkDAO;

    @BeforeEach
    public void setUp() {
        AlcoholService.getInstance().reset();

        service = new DrinkCounterService();
        userDAO = mock(UserDAO.class);
        drinkDAO = mock(DrinkDAO.class);
        AtomicInteger nextDrinkId = new AtomicInteger(1);
        when(drinkDAO.save(any(Drink.class))).thenAnswer(invocation -> {
            Drink drink = invocation.getArgument(0);
            drink.setId(nextDrinkId.getAndIncrement());
            return drink;
        });
        ReflectionTestUtils.setField(service, "userDAO", userDAO);
        ReflectionTestUtils.setField(service, "drinkDao", drinkDAO);

        user = new User();
        user.setId(1);
        user.setWeight(80);
        user.setSex(User.Sex.MALE);
        when(userDAO.findById(1)).thenReturn(Optional.of(user));
    }

    @Test
    public void addDrinkToDateParsesLocalTimeUsingClientTimezoneOffset() {
        // Client timezone offset is JS-style: UTC+02:00 is reported as -120.
        service.addDrinkToDate(1, "05.03.2024 13:37", -120);

        assertEquals(1, user.getDrinks().size());
        assertEquals(Instant.parse("2024-03-05T11:37:00Z"), user.getDrinks().get(0).getTimeStamp());
    }

    @Test
    public void addDrinkToDateRejectsFutureDates() {
        String farFuture = "01.01.2099 00:00";
        assertThrows(IllegalArgumentException.class, () -> service.addDrinkToDate(1, farFuture, 0));
    }

    @Test
    public void changeDrinkAlcoholUpdatesDrinkAndPromilles() {
        int drinkId = service.addDrink(1, new Date());
        Drink drink = user.getDrinks().get(0);
        when(drinkDAO.findById(drinkId)).thenReturn(Optional.of(drink));
        float promillesBefore = user.getPromilles();

        service.changeDrinkAlcohol(1, drinkId, drink.getAlcohol() * 2);

        assertTrue(user.getPromilles() > promillesBefore);
    }

    @Test
    public void changeDrinkAlcoholRejectsAnotherUsersDrink() {
        Drink othersDrink = othersDrink();

        assertThrows(EntityNotFoundException.class, () -> service.changeDrinkAlcohol(1, othersDrink.getId(), 1f));
    }

    @Test
    public void removeDrinkFromUserRejectsAnotherUsersDrink() {
        Drink othersDrink = othersDrink();

        assertThrows(EntityNotFoundException.class, () -> service.removeDrinkFromUser(1, othersDrink.getId()));
        verify(drinkDAO, never()).delete(any(Drink.class));
    }

    private Drink othersDrink() {
        User other = new User();
        other.setId(2);
        Drink drink = new Drink();
        drink.setId(99);
        drink.setDrinker(other);
        when(drinkDAO.findById(99)).thenReturn(Optional.of(drink));
        return drink;
    }
}
