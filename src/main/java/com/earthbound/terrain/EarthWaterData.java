package com.earthbound.water;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * ============================================================
 * EARTHBOUND WATER DATA
 *
 * GUEMES NOAA COASTLINE VERSION
 *
 * PURPOSE:
 *
 * Determine LAND versus OCEAN using NOAA ENC land polygons
 * instead of the old TIGERweb hydrography image.
 *
 * This removes:
 *
 * - 512x512 TIGER water mask
 * - 2048x2048 TIGER water mask
 * - alpha-channel water detection
 * - ferry rectangle corrections
 * - three-point ferry shoreline correction
 *
 * NOAA land polygons determine whether a coordinate is land.
 *
 * If a coordinate is inside NOAA land:
 *      LAND
 *
 * If it is outside NOAA land:
 *      WATER
 *
 * This first version is deliberately limited to the
 * Guemes Island geographic tile.
 *
 * ============================================================
 */
public class EarthWaterData {

    public static final int SEA_LEVEL = 63;

    /*
     * ========================================================
     * GUEMES TEST BOUNDS
     * ========================================================
     */

    private static final double WEST = -122.70;
    private static final double SOUTH = 48.47;
    private static final double EAST = -122.55;
    private static final double NORTH = 48.60;

    /*
     * ========================================================
     * NOAA ENC LAND AREA
     * ========================================================
     *
     * NOAA ENC Direct
     * Coastal Land_Area polygon layer.
     *
     * Layer 171:
     * Coastal.Land_Area
     *
     * Geometry:
     * Polygon
     *
     * Spatial reference:
     * EPSG:4326
     * ========================================================
     */

    private static final String NOAA_LAND_QUERY =
            "https://encdirect.noaa.gov/"
                    + "arcgis/rest/services/"
                    + "encdirect/enc_coastal/MapServer/171/query";

    /*
     * ========================================================
     * LOADED POLYGONS
     * ========================================================
     */

    private static final List<LandPolygon> landPolygons =
            new ArrayList<>();

    private static boolean loaded = false;

    /*
     * ========================================================
     * PUBLIC LOAD ENTRY POINT
     * ========================================================
     */

    public static void load() {

        loadGuemesWaterMask();

    }

    /*
     * ========================================================
     * COMPATIBILITY METHOD
     *
     * EarthBound.java already calls this method.
     *
     * We keep the method name so no other EarthBound files
     * need to change.
     * ========================================================
     */

    public static boolean loadGuemesWaterMask() {

        System.out.println(
                "[EarthBound] Loading NOAA Guemes land/coastline data..."
        );

        landPolygons.clear();
        loaded = false;

        try {

            String geoJson =
                    downloadNoaaLandPolygons();

            parseGeoJson(
                    geoJson
            );

            if (landPolygons.isEmpty()) {

                throw new IllegalStateException(
                        "NOAA returned no land polygons "
                                + "for the Guemes tile."
                );

            }

            loaded = true;

            System.out.println(
                    "[EarthBound] NOAA Guemes coastline loaded!"
            );

            System.out.println(
                    "[EarthBound] NOAA land polygons: "
                            + landPolygons.size()
            );

            System.out.println(
                    "[EarthBound] TIGER water mask: DISABLED"
            );

            System.out.println(
                    "[EarthBound] Ferry shoreline correction: DISABLED"
            );

            System.out.println(
                    "[EarthBound] NOAA LAND/OCEAN TEST ACTIVE"
            );

            return true;

        } catch (Exception exception) {

            loaded = false;

            System.err.println(
                    "[EarthBound] Failed to load NOAA coastline data!"
            );

            exception.printStackTrace();

            return false;

        }

    }

    /*
     * ========================================================
     * DOWNLOAD NOAA LAND POLYGONS
     * ========================================================
     */

    private static String downloadNoaaLandPolygons()
            throws Exception {

        System.out.println(
                "[EarthBound] Downloading NOAA ENC land polygons..."
        );

        /*
         * ArcGIS envelope:
         *
         * xmin,ymin,xmax,ymax
         *
         * longitude = X
         * latitude  = Y
         */

        String geometry =
                WEST
                        + ","
                        + SOUTH
                        + ","
                        + EAST
                        + ","
                        + NORTH;

        String request =
                NOAA_LAND_QUERY

                        + "?where="
                        + encode("1=1")

                        + "&geometry="
                        + encode(geometry)

                        + "&geometryType="
                        + encode("esriGeometryEnvelope")

                        + "&inSR=4326"

                        + "&spatialRel="
                        + encode(
                                "esriSpatialRelIntersects"
                        )

                        + "&outFields="
                        + encode("OBJECTID")

                        + "&returnGeometry=true"

                        + "&outSR=4326"

                        + "&f=geojson";

        URL url =
                new URL(
                        request
                );

        HttpURLConnection connection =
                (HttpURLConnection)
                        url.openConnection();

        connection.setConnectTimeout(
                30000
        );

        connection.setReadTimeout(
                60000
        );

        connection.setRequestProperty(
                "User-Agent",
                "EarthBound-Minecraft-Server/0.1"
        );

        connection.setRequestProperty(
                "Accept",
                "application/geo+json, application/json"
        );

        connection.connect();

        int responseCode =
                connection.getResponseCode();

        if (
                responseCode < 200
                        ||
                responseCode >= 300
        ) {

            String errorText =
                    readStream(
                            connection.getErrorStream()
                    );

            connection.disconnect();

            throw new IllegalStateException(
                    "NOAA returned HTTP "
                            + responseCode
                            + ": "
                            + errorText
            );

        }

        String response;

        try {

            response =
                    readStream(
                            connection.getInputStream()
                    );

        } finally {

            connection.disconnect();

        }

        if (
                response == null
                        ||
                response.trim().isEmpty()
        ) {

            throw new IllegalStateException(
                    "NOAA returned an empty response."
            );

        }

        System.out.println(
                "[EarthBound] NOAA coastline response received: "
                        + response.length()
                        + " characters"
        );

        return response;

    }

    /*
     * ========================================================
     * PARSE GEOJSON
     * ========================================================
     */

    private static void parseGeoJson(
            String geoJson
    ) {

        JsonObject root =
                JsonParser
                        .parseString(
                                geoJson
                        )
                        .getAsJsonObject();

        /*
         * ArcGIS may return an error object as JSON.
         */

        if (root.has("error")) {

            throw new IllegalStateException(
                    "NOAA ArcGIS error: "
                            + root.get("error").toString()
            );

        }

        if (!root.has("features")) {

            throw new IllegalStateException(
                    "NOAA GeoJSON response has no features array."
            );

        }

        JsonArray features =
                root.getAsJsonArray(
                        "features"
                );

        System.out.println(
                "[EarthBound] NOAA land features returned: "
                        + features.size()
        );

        for (
                JsonElement featureElement
                : features
        ) {

            if (
                    featureElement == null
                            ||
                    !featureElement.isJsonObject()
            ) {

                continue;

            }

            JsonObject feature =
                    featureElement.getAsJsonObject();

            if (
                    !feature.has("geometry")
                            ||
                    feature.get("geometry").isJsonNull()
            ) {

                continue;

            }

            JsonObject geometry =
                    feature
                            .getAsJsonObject(
                                    "geometry"
                            );

            if (
                    !geometry.has("type")
                            ||
                    !geometry.has("coordinates")
            ) {

                continue;

            }

            String type =
                    geometry
                            .get("type")
                            .getAsString();

            JsonArray coordinates =
                    geometry
                            .getAsJsonArray(
                                    "coordinates"
                            );

            if (
                    "Polygon".equalsIgnoreCase(type)
            ) {

                parsePolygon(
                        coordinates
                );

            } else if (
                    "MultiPolygon".equalsIgnoreCase(type)
            ) {

                parseMultiPolygon(
                        coordinates
                );

            }

        }

    }

    /*
     * ========================================================
     * PARSE POLYGON
     * ========================================================
     *
     * GeoJSON Polygon:
     *
     * [
     *   exterior ring,
     *   optional hole,
     *   optional hole...
     * ]
     * ========================================================
     */

    private static void parsePolygon(
            JsonArray polygonCoordinates
    ) {

        if (
                polygonCoordinates == null
                        ||
                polygonCoordinates.size() == 0
        ) {

            return;

        }

        List<Point> exterior =
                parseRing(
                        polygonCoordinates
                                .get(0)
                                .getAsJsonArray()
                );

        if (exterior.size() < 3) {

            return;

        }

        List<List<Point>> holes =
                new ArrayList<>();

        for (
                int ringIndex = 1;
                ringIndex < polygonCoordinates.size();
                ringIndex++
        ) {

            JsonElement ringElement =
                    polygonCoordinates.get(
                            ringIndex
                    );

            if (
                    ringElement == null
                            ||
                    !ringElement.isJsonArray()
            ) {

                continue;

            }

            List<Point> hole =
                    parseRing(
                            ringElement.getAsJsonArray()
                    );

            if (hole.size() >= 3) {

                holes.add(
                        hole
                );

            }

        }

        landPolygons.add(
                new LandPolygon(
                        exterior,
                        holes
                )
        );

    }

    /*
     * ========================================================
     * PARSE MULTIPOLYGON
     * ========================================================
     */

    private static void parseMultiPolygon(
            JsonArray multiPolygonCoordinates
    ) {

        for (
                JsonElement polygonElement
                : multiPolygonCoordinates
        ) {

            if (
                    polygonElement == null
                            ||
                    !polygonElement.isJsonArray()
            ) {

                continue;

            }

            parsePolygon(
                    polygonElement.getAsJsonArray()
            );

        }

    }

    /*
     * ========================================================
     * PARSE RING
     * ========================================================
     */

    private static List<Point> parseRing(
            JsonArray ringCoordinates
    ) {

        List<Point> points =
                new ArrayList<>();

        for (
                JsonElement coordinateElement
                : ringCoordinates
        ) {

            if (
                    coordinateElement == null
                            ||
                    !coordinateElement.isJsonArray()
            ) {

                continue;

            }

            JsonArray coordinate =
                    coordinateElement
                            .getAsJsonArray();

            if (coordinate.size() < 2) {

                continue;

            }

            double longitude =
                    coordinate
                            .get(0)
                            .getAsDouble();

            double latitude =
                    coordinate
                            .get(1)
                            .getAsDouble();

            points.add(
                    new Point(
                            longitude,
                            latitude
                    )
            );

        }

        return points;

    }

    /*
     * ========================================================
     * PUBLIC WATER LOOKUP
     * ========================================================
     */

    public static boolean isWater(
            double latitude,
            double longitude
    ) {

        /*
         * Fail-safe:
         *
         * If NOAA did not load, do NOT flood the world.
         */

        if (!isLoaded()) {

            return false;

        }

        /*
         * This first NOAA test only controls the Guemes tile.
         *
         * Outside the tile we return land for now.
         */

        if (
                longitude < WEST
                        ||
                longitude > EAST
                        ||
                latitude < SOUTH
                        ||
                latitude > NORTH
        ) {

            return false;

        }

        /*
         * NOAA gives us LAND polygons.
         *
         * Inside NOAA land:
         *      water = false
         *
         * Outside NOAA land:
         *      water = true
         */

        boolean land =
                isLand(
                        latitude,
                        longitude
                );

        return !land;

    }

    /*
     * ========================================================
     * LAND LOOKUP
     * ========================================================
     */

    private static boolean isLand(
            double latitude,
            double longitude
    ) {

        for (
                LandPolygon polygon
                : landPolygons
        ) {

            if (
                    polygon.contains(
                            longitude,
                            latitude
                    )
            ) {

                return true;

            }

        }

        return false;

    }

    /*
     * ========================================================
     * STATUS
     * ========================================================
     */

    public static boolean isLoaded() {

        return loaded
                &&
                !landPolygons.isEmpty();

    }

    /*
     * ========================================================
     * URL ENCODING
     * ========================================================
     */

    private static String encode(
            String value
    ) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );

    }

    /*
     * ========================================================
     * READ HTTP STREAM
     * ========================================================
     */

    private static String readStream(
            InputStream input
    )
            throws Exception {

        if (input == null) {

            return "";

        }

        StringBuilder builder =
                new StringBuilder();

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        input,
                                        StandardCharsets.UTF_8
                                )
                        )
        ) {

            String line;

            while (
                    (line =
                            reader.readLine())
                            != null
            ) {

                builder.append(
                        line
                );

            }

        }

        return builder.toString();

    }

    /*
     * ========================================================
     * POINT
     * ========================================================
     */

    private static class Point {

        private final double longitude;
        private final double latitude;

        private Point(
                double longitude,
                double latitude
        ) {

            this.longitude =
                    longitude;

            this.latitude =
                    latitude;

        }

    }

    /*
     * ========================================================
     * LAND POLYGON
     * ========================================================
     */

    private static class LandPolygon {

        private final List<Point> exterior;

        private final List<List<Point>> holes;

        /*
         * Bounding box.
         *
         * This prevents us from running the more expensive
         * point-in-polygon calculation against polygons that
         * are nowhere near the requested coordinate.
         */

        private final double minLongitude;
        private final double maxLongitude;
        private final double minLatitude;
        private final double maxLatitude;

        private LandPolygon(
                List<Point> exterior,
                List<List<Point>> holes
        ) {

            this.exterior =
                    exterior;

            this.holes =
                    holes;

            double minimumLongitude =
                    Double.POSITIVE_INFINITY;

            double maximumLongitude =
                    Double.NEGATIVE_INFINITY;

            double minimumLatitude =
                    Double.POSITIVE_INFINITY;

            double maximumLatitude =
                    Double.NEGATIVE_INFINITY;

            for (
                    Point point
                    : exterior
            ) {

                minimumLongitude =
                        Math.min(
                                minimumLongitude,
                                point.longitude
                        );

                maximumLongitude =
                        Math.max(
                                maximumLongitude,
                                point.longitude
                        );

                minimumLatitude =
                        Math.min(
                                minimumLatitude,
                                point.latitude
                        );

                maximumLatitude =
                        Math.max(
                                maximumLatitude,
                                point.latitude
                        );

            }

            minLongitude =
                    minimumLongitude;

            maxLongitude =
                    maximumLongitude;

            minLatitude =
                    minimumLatitude;

            maxLatitude =
                    maximumLatitude;

        }

        private boolean contains(
                double longitude,
                double latitude
        ) {

            /*
             * Bounding-box rejection.
             */

            if (
                    longitude < minLongitude
                            ||
                    longitude > maxLongitude
                            ||
                    latitude < minLatitude
                            ||
                    latitude > maxLatitude
            ) {

                return false;

            }

            /*
             * Must be inside exterior.
             */

            if (
                    !pointInRing(
                            longitude,
                            latitude,
                            exterior
                    )
            ) {

                return false;

            }

            /*
             * If inside one of the polygon's holes,
             * it is not land.
             */

            for (
                    List<Point> hole
                    : holes
            ) {

                if (
                        pointInRing(
                                longitude,
                                latitude,
                                hole
                        )
                ) {

                    return false;

                }

            }

            return true;

        }

    }

    /*
     * ========================================================
     * POINT-IN-POLYGON
     *
     * Standard ray-casting algorithm.
     * ========================================================
     */

    private static boolean pointInRing(
            double longitude,
            double latitude,
            List<Point> ring
    ) {

        boolean inside =
                false;

        int size =
                ring.size();

        if (size < 3) {

            return false;

        }

        for (
                int i = 0,
                    j = size - 1;

                i < size;

                j = i++
        ) {

            Point pointI =
                    ring.get(i);

            Point pointJ =
                    ring.get(j);

            boolean crosses =
                    (
                            (pointI.latitude > latitude)
                                    !=
                            (pointJ.latitude > latitude)
                    );

            if (!crosses) {

                continue;

            }

            double denominator =
                    pointJ.latitude
                            - pointI.latitude;

            if (
                    Math.abs(
                            denominator
                    )
                            < 0.000000000001
            ) {

                continue;

            }

            double intersectionLongitude =
                    (
                            (
                                    pointJ.longitude
                                            - pointI.longitude
                            )
                                    *
                                    (
                                            latitude
                                                    - pointI.latitude
                                    )
                                    /
                                    denominator
                    )
                            +
                            pointI.longitude;

            if (
                    longitude
                            < intersectionLongitude
            ) {

                inside =
                        !inside;

            }

        }

        return inside;

    }

}
