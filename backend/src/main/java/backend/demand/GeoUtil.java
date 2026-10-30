package backend.demand;

import java.util.Locale;

public final class GeoUtil {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double KM_PER_DEGREE_LAT = 111.0;

    private GeoUtil() {
    }

    /** ~1.1 km grid cell used to aggregate demand by locality. */
    public static String areaKey(double lat, double lng) {
        return String.format(Locale.ROOT, "%.2f,%.2f", lat, lng);
    }

    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /** {minLat, maxLat, minLng, maxLng} around a point; cheap pre-filter before haversine. */
    public static double[] boundingBox(double lat, double lng, double radiusKm) {
        double dLat = radiusKm / KM_PER_DEGREE_LAT;
        double cos = Math.max(0.01, Math.cos(Math.toRadians(lat)));
        double dLng = radiusKm / (KM_PER_DEGREE_LAT * cos);
        return new double[]{lat - dLat, lat + dLat, lng - dLng, lng + dLng};
    }

    public static boolean validLatLng(Double lat, Double lng) {
        return lat != null && lng != null
                && lat >= -90 && lat <= 90
                && lng >= -180 && lng <= 180
                && !(lat == 0.0 && lng == 0.0);
    }
}
