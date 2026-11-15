package drinkcounter.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import drinkcounter.model.Friend;
import java.util.List;
import org.junit.jupiter.api.Test;

class FriendNamesTest {

    private final List<Friend> pool = List.of(friend(1, "Matti Virtanen"), friend(2, "Matias"),
            friend(3, "Liisa Korhonen"), friend(4, "Matti"));

    @Test
    void theWholeNameBeatsAWordOfIt() {
        assertEquals(List.of(4), ids(FriendNames.matching("matti", pool)));
    }

    @Test
    void aWordOfANameMatchesBeforeTheStartOfOne() {
        assertEquals(List.of(3), ids(FriendNames.matching("KORHONEN", pool)));
    }

    @Test
    void theStartOfAWordMatchesWhenNothingStricterDoes() {
        assertEquals(List.of(2), ids(FriendNames.matching("Matia", pool)));
        assertEquals(List.of(1, 2, 4), ids(FriendNames.matching("Mat", pool)));
    }

    @Test
    void extraSpacesAreIgnored() {
        assertEquals(List.of(3), ids(FriendNames.matching("  liisa   korhonen ", pool)));
    }

    @Test
    void aBlankOrUnknownNameMatchesNoOne() {
        assertEquals(List.of(), FriendNames.matching(" ", pool));
        assertEquals(List.of(), FriendNames.matching("Kalle", pool));
    }

    @Test
    void theClosestNamesComeFirst() {
        assertEquals(List.of(3), ids(FriendNames.closest("Lisa", pool)));
        assertEquals(List.of(2, 1, 4), ids(FriendNames.closest("Matia", pool)));
    }

    @Test
    void aNameNothingResemblesHasNoClosestFriends() {
        assertEquals(List.of(), FriendNames.closest("Zzzzz", pool));
    }

    private static List<Integer> ids(List<Friend> friends) {
        return friends.stream().map(Friend::getId).toList();
    }

    private static Friend friend(int id, String name) {
        return new Friend(id, name, null);
    }
}
