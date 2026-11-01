package drinkcounter;

/** Coordinates that are missing or not on Earth. The message never repeats them. */
public class InvalidPositionException extends IllegalArgumentException {

    public InvalidPositionException() {
        super("Latitude must be between -90 and 90 and longitude between -180 and 180");
    }
}
