package drinkcounter.web.controllers.api.v2;

import drinkcounter.PartyDeletion;
import drinkcounter.model.User;
import java.util.List;

/**
 * Whether the signed-in member may delete the party, and whom deleting it would affect: the other
 * users it disappears from and the guests deleted with it.
 */
public record PartyDeletionDTO(boolean deletable, List<Person> otherUsers, List<Person> deletedGuests) {

    public record Person(int id, String name) {

        static Person of(User user) {
            return new Person(user.getId(), user.getName());
        }
    }

    public static PartyDeletionDTO of(PartyDeletion.Impact impact) {
        return new PartyDeletionDTO(impact.deletable(),
                impact.otherUsers().stream().map(Person::of).toList(),
                impact.deletedGuests().stream().map(Person::of).toList());
    }
}
