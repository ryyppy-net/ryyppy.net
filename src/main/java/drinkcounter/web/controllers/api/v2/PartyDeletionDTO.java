package drinkcounter.web.controllers.api.v2;

import drinkcounter.PartyDeletions;
import drinkcounter.model.User;
import java.util.List;

/**
 * What deleting a party would mean for the signed-in member: whether they may delete it, the
 * other users who would lose it and the guests deleted with it.
 */
public record PartyDeletionDTO(boolean allowed, List<Person> otherUsers, List<Person> guests) {

    public record Person(int id, String name) {

        static Person of(User user) {
            return new Person(user.getId(), user.getName());
        }
    }

    public static PartyDeletionDTO of(PartyDeletions.Preview preview) {
        return new PartyDeletionDTO(preview.allowed(),
                preview.otherUsers().stream().map(Person::of).toList(),
                preview.guests().stream().map(Person::of).toList());
    }
}
