package drinkcounter.mcp;

import drinkcounter.model.Friend;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Matches the name a person was given by against a pool of friends. A name matches in the first
 * of these ways that matches anyone: the whole name, one of its words, or the start of one of
 * its words. Case is ignored.
 */
final class FriendNames {

    private static final int SUGGESTIONS = 3;

    private FriendNames() {
    }

    /** The friends the query matches in its strictest matching way; empty if it matches none. */
    static List<Friend> matching(String query, List<Friend> pool) {
        String wanted = normalize(query);
        if (wanted.isEmpty()) {
            return List.of();
        }
        List<Predicate<String>> ways = List.of(
                name -> name.equals(wanted),
                name -> Arrays.asList(name.split(" ")).contains(wanted),
                name -> Arrays.stream(name.split(" ")).anyMatch(word -> word.startsWith(wanted)));
        for (Predicate<String> way : ways) {
            List<Friend> found = pool.stream().filter(friend -> way.test(normalize(friend.getName()))).toList();
            if (!found.isEmpty()) {
                return found;
            }
        }
        return List.of();
    }

    /**
     * Up to three friends whose name, or one word of it, is a few edits from the query, the
     * fewest first. A name nothing resembles gets no suggestions.
     */
    static List<Friend> closest(String query, List<Friend> pool) {
        String wanted = normalize(query);
        int tolerance = Math.max(2, wanted.length() / 3);
        return pool.stream()
                .filter(friend -> distance(wanted, normalize(friend.getName())) <= tolerance)
                .sorted(Comparator.comparingInt((Friend friend) -> distance(wanted, normalize(friend.getName()))))
                .limit(SUGGESTIONS)
                .toList();
    }

    private static int distance(String wanted, String name) {
        int best = editDistance(wanted, name);
        for (String word : name.split(" ")) {
            best = Math.min(best, editDistance(wanted, word));
        }
        return best;
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static int editDistance(String a, String b) {
        int[] previous = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            int[] current = new int[b.length() + 1];
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int substitution = previous[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                current[j] = Math.min(substitution, Math.min(previous[j] + 1, current[j - 1] + 1));
            }
            previous = current;
        }
        return previous[b.length()];
    }
}
