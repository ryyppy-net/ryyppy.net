package drinkcounter;

/**
 * A distance as shown to someone looking for a party: never exact, so a party's place cannot be
 * pinpointed. Below 100 m it is {@code lessThan} 100; below 1 km rounded to 100 m; from 1 km up
 * rounded to 500 m.
 */
public record RoughDistance(int metres, boolean lessThan) {

    public static RoughDistance of(double metres) {
        if (metres < 100) {
            return new RoughDistance(100, true);
        }
        long rounded = Math.round(metres / 100) * 100;
        if (rounded >= 1000) {
            rounded = Math.round(metres / 500) * 500;
        }
        return new RoughDistance((int) rounded, false);
    }
}
