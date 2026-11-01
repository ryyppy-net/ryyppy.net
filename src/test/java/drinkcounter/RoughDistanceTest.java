package drinkcounter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RoughDistanceTest {

    @ParameterizedTest
    @CsvSource({"0", "12.5", "99.99"})
    void underOneHundredMetresIsLessThanOneHundred(double metres) {
        assertEquals(new RoughDistance(100, true), RoughDistance.of(metres));
    }

    @ParameterizedTest
    @CsvSource({
            "100, 100", "149.9, 100", "150, 200", "349, 300", "949.9, 900",
            "950, 1000", "1000, 1000", "1249.9, 1000", "1250, 1500", "1600, 1500",
            "2749, 2500", "2750, 3000", "5000, 5000"})
    void roundsToOneHundredMetresBelowOneKilometreAndToHalfAKilometreFromThere(double metres, int rough) {
        assertEquals(new RoughDistance(rough, false), RoughDistance.of(metres));
    }
}
