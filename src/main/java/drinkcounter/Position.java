package drinkcounter;

/** A point on Earth in WGS 84 degrees, as a browser's Geolocation API reports it. */
public record Position(double latitude, double longitude) {

    static final double EARTH_RADIUS_M = 6_371_000;

    public Position {
        if (!(latitude >= -90 && latitude <= 90) || !(longitude >= -180 && longitude <= 180)) {
            throw new InvalidPositionException();
        }
    }

    /** Great-circle (haversine) distance in metres. */
    public double distanceTo(Position other) {
        double dLat = Math.toRadians(other.latitude - latitude);
        double dLon = Math.toRadians(other.longitude - longitude);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(other.latitude))
                * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.min(1, Math.sqrt(a)));
    }
}
