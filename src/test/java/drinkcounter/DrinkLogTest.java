package drinkcounter;

import drinkcounter.repository.DrinkRepository;
import drinkcounter.repository.UserRepository;
import drinkcounter.model.Drink;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import jakarta.persistence.EntityNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DrinkLogTest {

    private DrinkLog drinkLog;
    private UserRepository userRepository;
    private User user;
    private DrinkRepository drinkRepository;
    private PartyRoster partyRoster;
    private MutableClock clock;

    @BeforeEach
    public void setUp() {
        userRepository = mock(UserRepository.class);
        drinkRepository = mock(DrinkRepository.class);
        AtomicInteger nextDrinkId = new AtomicInteger(1);
        when(drinkRepository.save(any(Drink.class))).thenAnswer(invocation -> {
            Drink drink = invocation.getArgument(0);
            drink.setId(nextDrinkId.getAndIncrement());
            return drink;
        });
        partyRoster = mock(PartyRoster.class);
        clock = new MutableClock(Instant.parse("2024-03-05T12:00:00Z"));
        drinkLog = new DrinkLog(drinkRepository, userRepository, partyRoster, clock);

        user = new User();
        user.setId(1);
        user.setWeight(80);
        user.setSex(User.Sex.MALE);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
    }

    @Test
    public void recordAtParsesLocalTimeUsingClientTimezoneOffset() {
        // Client timezone offset is JS-style: UTC+02:00 is reported as -120.
        drinkLog.recordAt(1, "05.03.2024 13:37", -120);

        assertEquals(1, user.getDrinks().size());
        assertEquals(Instant.parse("2024-03-05T11:37:00Z"), user.getDrinks().get(0).getTimeStamp());
    }

    @Test
    public void recordAtRejectsFutureDates() {
        String farFuture = "01.01.2099 00:00";
        assertThrows(IllegalArgumentException.class, () -> drinkLog.recordAt(1, farFuture, 0));
    }

    @Test
    public void correctAlcoholUpdatesTheUsersDrink() {
        Drink drink = usersDrink();

        drinkLog.correctAlcohol(1, drink.getId(), 30f);

        assertEquals(30f, drink.getAlcohol());
    }

    @Test
    public void correctAlcoholRejectsAnotherUsersDrink() {
        Drink othersDrink = othersDrink();

        assertThrows(EntityNotFoundException.class, () -> drinkLog.correctAlcohol(1, othersDrink.getId(), 1f));
    }

    @Test
    public void undoDeletesTheUsersDrinkFoundInTheirDrinkList() {
        Drink drink = usersDrink();

        drinkLog.undo(1, drink.getId());

        assertTrue(user.getDrinks().isEmpty());
        verify(drinkRepository).delete(drink);
        verify(drinkRepository, never()).findById(any());
    }

    @Test
    public void undoRejectsAnotherUsersDrink() {
        Drink othersDrink = othersDrink();

        assertThrows(EntityNotFoundException.class, () -> drinkLog.undo(1, othersDrink.getId()));
        verify(drinkRepository, never()).delete(any(Drink.class));
    }

    private Drink usersDrink() {
        drinkLog.record(1, new Date());
        return user.getDrinks().get(0);
    }

    private Drink othersDrink() {
        User other = new User();
        other.setId(2);
        Drink drink = new Drink();
        drink.setId(99);
        drink.setDrinker(other);
        when(drinkRepository.findById(99)).thenReturn(Optional.of(drink));
        return drink;
    }

    @Test
    public void totalIsCountedAtMostOnceAMinute() {
        Instant start = Instant.parse("2024-03-05T12:00:00Z");
        clock.set(start);
        when(drinkRepository.count()).thenReturn(10L, 11L);

        assertEquals(10L, drinkLog.total());
        clock.set(start.plusSeconds(59));
        assertEquals(10L, drinkLog.total());
        verify(drinkRepository, times(1)).count();

        clock.set(start.plusSeconds(60));
        assertEquals(11L, drinkLog.total());
    }

    private static final Instant PARTY_END = Instant.parse("2024-03-05T14:00:00Z");

    private Party partyEndingAt(Instant end) {
        Party party = new Party();
        party.setId(5);
        party.setStartTime(end.minusSeconds(7200));
        party.setEndTime(end);
        when(partyRoster.get(5)).thenReturn(party);
        return party;
    }

    @Test
    public void recordInPartySavesTheDrinkJustBeforeTheEnd() {
        partyEndingAt(PARTY_END);
        clock.set(PARTY_END.minusMillis(1));

        Drink drink = drinkLog.recordInParty(5, 1, null, 12f);

        assertEquals(PARTY_END.minusMillis(1), drink.getTimeStamp());
        assertEquals(1, user.getDrinks().size());
        verify(drinkRepository).save(any(Drink.class));
    }

    @Test
    public void recordInPartyIsRefusedAtTheEnd() {
        partyEndingAt(PARTY_END);
        clock.set(PARTY_END);

        assertThrows(PartyEndedException.class, () -> drinkLog.recordInParty(5, 1, null, 12f));

        assertTrue(user.getDrinks().isEmpty());
        verify(drinkRepository, never()).save(any(Drink.class));
    }

    @Test
    public void recordInPartyIsRefusedAfterTheEnd() {
        partyEndingAt(PARTY_END);
        clock.set(PARTY_END.plusSeconds(3600));

        assertThrows(PartyEndedException.class, () -> drinkLog.recordInParty(5, 1, Date.from(PARTY_END.minusSeconds(60)), 12f));

        verify(drinkRepository, never()).save(any(Drink.class));
    }

    @Test
    public void undoAndCorrectionStayAllowedAfterThePartyEnded() {
        partyEndingAt(PARTY_END);
        clock.set(PARTY_END.minusSeconds(1));
        Drink drink = drinkLog.recordInParty(5, 1, null, 12f);
        clock.set(PARTY_END.plusSeconds(1));
        drink.setId(3);

        drinkLog.correctAlcohol(1, 3, 6f);
        drinkLog.undo(1, 3);

        assertTrue(user.getDrinks().isEmpty());
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void set(Instant now) {
            this.now = now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
