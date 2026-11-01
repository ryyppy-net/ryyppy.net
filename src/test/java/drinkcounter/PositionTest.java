package drinkcounter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PositionTest {

    @Test
    void oneDegreeOfLatitudeIsAbout111Kilometres() {
        assertEquals(111_195, new Position(60, 25).distanceTo(new Position(61, 25)), 1);
    }

    @Test
    void oneDegreeOfLongitudeShrinksWithLatitude() {
        assertEquals(111_195, new Position(0, 25).distanceTo(new Position(0, 26)), 1);
        assertEquals(55_597, new Position(60, 25).distanceTo(new Position(60, 26)), 1);
    }

    @Test
    void helsinkiToTampereIsAbout160Kilometres() {
        Position helsinkiCentralStation = new Position(60.1719, 24.9414);
        Position tampereStation = new Position(61.4985, 23.7730);

        assertEquals(160_000, helsinkiCentralStation.distanceTo(tampereStation), 2_000);
    }

    @Test
    void distanceCrossesTheAntimeridian() {
        assertEquals(22_239, new Position(0, 179.9).distanceTo(new Position(0, -179.9)), 1);
    }

    @Test
    void samePlaceIsZeroMetresAway() {
        assertEquals(0, new Position(60.1699, 24.9384).distanceTo(new Position(60.1699, 24.9384)));
    }

    @ParameterizedTest
    @CsvSource({"90.0001, 0", "-90.0001, 0", "0, 180.0001", "0, -180.0001", "NaN, 0", "0, NaN", "Infinity, 0"})
    void coordinatesOffEarthAreInvalid(double latitude, double longitude) {
        assertThrows(InvalidPositionException.class, () -> new Position(latitude, longitude));
    }

    @Test
    void invalidPositionMessageDoesNotRepeatTheCoordinates() {
        InvalidPositionException e = assertThrows(InvalidPositionException.class, () -> new Position(91.2345, 0));

        assertEquals(false, e.getMessage().contains("91"));
    }
}
