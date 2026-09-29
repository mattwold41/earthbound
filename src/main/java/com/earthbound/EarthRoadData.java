package com.earthbound;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class EarthRoadData {

    /*
     * ============================================================
     * EARTHBOUND ROAD DATA
     * ============================================================
     *
     * Real Census / TIGER road centerlines determine WHERE
     * roads are located.
     *
     * EarthBound determines how wide each road class is.
     *
     * FINAL ROAD WIDTH STANDARD:
     *
     * LOCAL   = 5 blocks
     * HIGHWAY = 8 blocks
     * FREEWAY = 10 blocks
     *
     * The Stone Brick border is generated separately by
     * EarthGenerator and is NOT included in these widths.
     * ============================================================
     */


    /*
     * Current Guemes / Anacortes test area.
     */

    private static final double WEST =
            -122.70;

    private static final double SOUTH =
            48.47;

    private static final double EAST =
            -122.55;

    private static final double NORTH =
            48.60;


    /*
     * ============================================================
     * EARTHBOUND ROAD WIDTHS
     * ============================================================
     */

    public static final int FREEWAY_WIDTH =
            10;

    public static final int HIGHWAY_WIDTH =
            8;

    public static final int LOCAL_WIDTH =
            5;


    /*
     * Half-width from centerline.
     *
     * Example:
     *
     * Local road:
     *
     * 5 blocks wide
     * radius = 2.5 blocks/meters
     *
     * Highway:
     *
     * 8 blocks wide
     * radius = 4.0
     *
     * Freeway:
     *
     * 10 blocks wide
     * radius = 5.0
     */

    private static final double FREEWAY_RADIUS =
            FREEWAY_WIDTH / 2.0;

    private static final double HIGHWAY_RADIUS =
            HIGHWAY_WIDTH / 2.0;

    private static final double LOCAL_RADIUS =
            LOCAL_WIDTH / 2.0;


    /*
     * ============================================================
     * DOWNLOAD RELIABILITY
     * ============================================================
     */

    private static final int MAX_DOWNLOAD_ATTEMPTS =
            3;

    private static final int CONNECT_TIMEOUT_MS =
            20000;

    private static final int READ_TIMEOUT_MS =
            60000;

    private static final long RETRY_DELAY_MS =
            3000L;


    /*
     * ============================================================
     * TIGERWEB TRANSPORTATION SERVICE
     * ============================================================
     */

    private static final String ROAD_SERVICE =
            "https://tigerweb.geo.census.gov/arcgis/rest/services/"
                    + "TIGERweb/Transportation/MapServer";


    /*
     * TIGERweb feature layers.
     *
     * 2 = Primary Roads
     * 6 = Secondary Roads
     * 8 = Local Roads
     */

    private static final int PRIMARY_LAYER =
            2;

    private static final int SECONDARY_LAYER =
            6;

    private static final int LOCAL_LAYER =
            8;


    /*
     * ============================================================
     * ROAD CENTERLINE STORAGE
     * ============================================================
     */

    private static final List<RoadSegment>
            primaryRoads =
            new ArrayList<>();

    private static final List<RoadSegment>
            secondaryRoads =
            new ArrayList<>();

    private static final List<RoadSegment>
            localRoads =
            new ArrayList<>();


    private static boolean loaded =
            false;


    private EarthRoadData() {
    }


    /*
     * ============================================================
     * ROAD TYPES
     * ============================================================
     */

    public enum RoadType {

        NONE(0),

        LOCAL(LOCAL_WIDTH),

        HIGHWAY(HIGHWAY_WIDTH),

        FREEWAY(FREEWAY_WIDTH);


        private final int width;


        RoadType(int width) {

            this.width =
                    width;
        }


        public int getWidth() {

            return width;
        }


        public double getRadius() {

            return width / 2.0;
        }
    }


    /*
     * ============================================================
     * ROAD SEGMENT
     * ============================================================
     *
     * One piece of a real-world road centerline.
     */

    private static final class RoadSegment {

        private final double latitude1;

        private final double longitude1;

        private final double latitude2;

        private final double longitude2;


        private RoadSegment(
                double latitude1,
                double longitude1,
                double latitude2,
                double longitude2
        ) {

            this.latitude1 =
                    latitude1;

            this.longitude1 =
                    longitude1;

            this.latitude2 =
                    latitude2;

            this.longitude2 =
                    longitude2;
        }
    }


    /*
     * ============================================================
     * LOAD ROAD DATA
     * ============================================================
     *
     * Keep the existing method name because EarthBound.java
     * already calls this method.
     */

    public static synchronized boolean loadGuemesRoadMask() {

        if (loaded) {

            return true;
        }


        System.out.println(
                "[EarthBound] Loading vector road centerlines..."
        );


        primaryRoads.clear();

        secondaryRoads.clear();

        localRoads.clear();


        boolean primaryLoaded =
                loadLayerWithRetry(
                        PRIMARY_LAYER,
                        "primary roads",
                        primaryRoads
                );


        boolean secondaryLoaded =
                loadLayerWithRetry(
                        SECONDARY_LAYER,
                        "secondary roads",
                        secondaryRoads
                );


        boolean localLoaded =
                loadLayerWithRetry(
                        LOCAL_LAYER,
                        "local roads",
                        localRoads
                );


        if (!primaryLoaded
                || !secondaryLoaded
                || !localLoaded) {

            loaded =
                    false;


            System.out.println(
                    "[EarthBound] Vector road data "
                            + "could not be completely loaded."
            );


            return false;
        }


        loaded =
                true;


        System.out.println(
                "[EarthBound] Vector road centerlines loaded!"
        );


        System.out.println(
                "[EarthBound] Road segments: primary="
                        + primaryRoads.size()
                        + ", secondary="
                        + secondaryRoads.size()
                        + ", local="
                        + localRoads.size()
        );


        System.out.println(
                "[EarthBound] Road widths: freeway="
                        + FREEWAY_WIDTH
                        + ", highway="
                        + HIGHWAY_WIDTH
                        + ", local="
                        + LOCAL_WIDTH
        );


        return true;
    }


    /*
     * ============================================================
     * DOWNLOAD WITH RETRIES
     * ============================================================
     */

    private static boolean loadLayerWithRetry(
            int layer,
            String layerName,
            List<RoadSegment> destination
    ) {

        for (int attempt = 1;
             attempt <= MAX_DOWNLOAD_ATTEMPTS;
             attempt++) {


            System.out.println(
                    "[EarthBound] Downloading "
                            + layerName
                            + " vectors - attempt "
                            + attempt
                            + " of "
                            + MAX_DOWNLOAD_ATTEMPTS
            );


            try {

                List<RoadSegment> downloaded =
                        downloadLayer(
                                layer,
                                layerName
                        );


                if (downloaded != null) {

                    destination.clear();

                    destination.addAll(
                            downloaded
                    );


                    System.out.println(
                            "[EarthBound] "
                                    + layerName
                                    + " loaded with "
                                    + destination.size()
                                    + " centerline segments."
                    );


                    return true;
                }


            } catch (Exception exception) {

                System.out.println(
                        "[EarthBound] "
                                + layerName
                                + " attempt "
                                + attempt
                                + " failed: "
                                + exception
                                .getClass()
                                .getSimpleName()
                                + ": "
                                + exception
                                .getMessage()
                );
            }


            if (attempt
                    < MAX_DOWNLOAD_ATTEMPTS) {

                System.out.println(
                        "[EarthBound] Waiting "
                                + (RETRY_DELAY_MS / 1000)
                                + " seconds before retrying "
                                + layerName
                                + "..."
                );


                try {

                    Thread.sleep(
                            RETRY_DELAY_MS
                    );


                } catch (InterruptedException exception) {

                    Thread.currentThread()
                            .interrupt();


                    return false;
                }
            }
        }


        return false;
    }


    /*
     * ============================================================
     * DOWNLOAD TIGERWEB LAYER
     * ============================================================
     */

    private static List<RoadSegment> downloadLayer(
            int layer,
            String layerName
    ) throws Exception {


        String envelope =
                WEST
                        + ","
                        + SOUTH
                        + ","
                        + EAST
                        + ","
                        + NORTH;


        String request =
                ROAD_SERVICE
                        + "/"
                        + layer
                        + "/query"
                        + "?where="
                        + encode("1=1")
                        + "&geometry="
                        + encode(envelope)
                        + "&geometryType="
                        + encode(
                                "esriGeometryEnvelope"
                        )
                        + "&inSR=4326"
                        + "&spatialRel="
                        + encode(
                                "esriSpatialRelIntersects"
                        )
                        + "&outFields="
                        + encode(
                                "OBJECTID,NAME,BASENAME,MTFCC,RTTYP"
                        )
                        + "&returnGeometry=true"
                        + "&outSR=4326"
                        + "&f=geojson";


        URL url =
                URI.create(
                        request
                ).toURL();


        HttpURLConnection connection =
                (HttpURLConnection)
                        url.openConnection();


        try {

            connection.setConnectTimeout(
                    CONNECT_TIMEOUT_MS
            );


            connection.setReadTimeout(
                    READ_TIMEOUT_MS
            );


            connection.setRequestProperty(
                    "User-Agent",
                    "EarthBound-Minecraft/0.1.0"
            );


            connection.setRequestProperty(
                    "Accept",
                    "application/geo+json, application/json"
            );


            int responseCode =
                    connection
                            .getResponseCode();


            if (responseCode != 200) {

                System.out.println(
                        "[EarthBound] "
                                + layerName
                                + " query returned HTTP "
                                + responseCode
                );


                return null;
            }


            String json;


            try (
                    InputStream input =
                            connection.getInputStream();

                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            input,
                                            StandardCharsets.UTF_8
                                    )
                            )
            ) {

                StringBuilder builder =
                        new StringBuilder();


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


                json =
                        builder.toString();
            }


            return parseGeoJson(
                    json,
                    layerName
            );


        } finally {

            connection.disconnect();
        }
    }


    /*
     * ============================================================
     * PARSE GEOJSON
     * ============================================================
     */

    private static List<RoadSegment> parseGeoJson(
            String json,
            String layerName
    ) {

        List<RoadSegment> segments =
                new ArrayList<>();


        JsonObject root =
                JsonParser.parseString(
                        json
                ).getAsJsonObject();


        if (!root.has(
                "features"
        )) {

            System.out.println(
                    "[EarthBound] "
                            + layerName
                            + " GeoJSON contained no features array."
            );


            return segments;
        }


        JsonArray features =
                root.getAsJsonArray(
                        "features"
                );


        System.out.println(
                "[EarthBound] "
                        + layerName
                        + " feature count="
                        + features.size()
        );


        for (
                JsonElement featureElement
                : features
        ) {

            if (!featureElement
                    .isJsonObject()) {

                continue;
            }


            JsonObject feature =
                    featureElement
                            .getAsJsonObject();


            if (!feature.has(
                    "geometry"
            )
                    || feature
                    .get("geometry")
                    .isJsonNull()) {

                continue;
            }


            JsonObject geometry =
                    feature
                            .getAsJsonObject(
                                    "geometry"
                            );


            if (!geometry.has(
                    "type"
            )
                    || !geometry.has(
                    "coordinates"
            )) {

                continue;
            }


            String geometryType =
                    geometry
                            .get("type")
                            .getAsString();


            JsonArray coordinates =
                    geometry
                            .getAsJsonArray(
                                    "coordinates"
                            );


            if ("LineString".equals(
                    geometryType
            )) {

                addLineString(
                        coordinates,
                        segments
                );


            } else if (
                    "MultiLineString".equals(
                            geometryType
                    )
            ) {

                for (
                        JsonElement lineElement
                        : coordinates
                ) {

                    if (lineElement
                            .isJsonArray()) {

                        addLineString(
                                lineElement
                                        .getAsJsonArray(),
                                segments
                        );
                    }
                }
            }
        }


        return segments;
    }


    /*
     * ============================================================
     * CONVERT LINESTRING TO SEGMENTS
     * ============================================================
     *
     * GeoJSON coordinate order:
     *
     * [longitude, latitude]
     */

    private static void addLineString(
            JsonArray coordinates,
            List<RoadSegment> segments
    ) {

        if (coordinates.size()
                < 2) {

            return;
        }


        for (int i = 0;
             i < coordinates.size() - 1;
             i++) {


            JsonArray first =
                    coordinates
                            .get(i)
                            .getAsJsonArray();


            JsonArray second =
                    coordinates
                            .get(i + 1)
                            .getAsJsonArray();


            if (first.size() < 2
                    || second.size() < 2) {

                continue;
            }


            double longitude1 =
                    first
                            .get(0)
                            .getAsDouble();


            double latitude1 =
                    first
                            .get(1)
                            .getAsDouble();


            double longitude2 =
                    second
                            .get(0)
                            .getAsDouble();


            double latitude2 =
                    second
                            .get(1)
                            .getAsDouble();


            segments.add(
                    new RoadSegment(
                            latitude1,
                            longitude1,
                            latitude2,
                            longitude2
                    )
            );
        }
    }


    /*
     * ============================================================
     * ROAD CLASSIFICATION
     * ============================================================
     *
     * Higher road classes take priority.
     */

    public static RoadType getRoadType(
            double latitude,
            double longitude
    ) {

        if (!loaded) {

            return RoadType.NONE;
        }


        if (isNearRoad(
                primaryRoads,
                latitude,
                longitude,
                FREEWAY_RADIUS
        )) {

            return RoadType.FREEWAY;
        }


        if (isNearRoad(
                secondaryRoads,
                latitude,
                longitude,
                HIGHWAY_RADIUS
        )) {

            return RoadType.HIGHWAY;
        }


        if (isNearRoad(
                localRoads,
                latitude,
                longitude,
                LOCAL_RADIUS
        )) {

            return RoadType.LOCAL;
        }


        return RoadType.NONE;
    }


    /*
     * ============================================================
     * IS ROAD
     * ============================================================
     */

    public static boolean isRoad(
            double latitude,
            double longitude
    ) {

        return getRoadType(
                latitude,
                longitude
        ) != RoadType.NONE;
    }


    /*
     * ============================================================
     * ROAD WIDTH
     * ============================================================
     */

    public static int getRoadWidth(
            double latitude,
            double longitude
    ) {

        return getRoadType(
                latitude,
                longitude
        ).getWidth();
    }


    /*
     * ============================================================
     * CLASS-SPECIFIC ROAD DISTANCES
     * ============================================================
     *
     * These are important for the new EarthGenerator.
     *
     * The old nearest-road helper combined all three road
     * classes into one number.
     *
     * These methods allow EarthGenerator to know exactly how
     * far a block is from:
     *
     * - a freeway
     * - a highway
     * - a local road
     *
     * That lets each road class receive the correct width.
     */


    public static double getDistanceToNearestFreewayMeters(
            double latitude,
            double longitude
    ) {

        if (!loaded) {

            return Double.POSITIVE_INFINITY;
        }


        return getNearestDistance(
                primaryRoads,
                latitude,
                longitude
        );
    }


    public static double getDistanceToNearestHighwayMeters(
            double latitude,
            double longitude
    ) {

        if (!loaded) {

            return Double.POSITIVE_INFINITY;
        }


        return getNearestDistance(
                secondaryRoads,
                latitude,
                longitude
        );
    }


    public static double getDistanceToNearestLocalRoadMeters(
            double latitude,
            double longitude
    ) {

        if (!loaded) {

            return Double.POSITIVE_INFINITY;
        }


        return getNearestDistance(
                localRoads,
                latitude,
                longitude
        );
    }


    /*
     * ============================================================
     * NEAREST ROAD TYPE
     * ============================================================
     *
     * This checks which centerline is physically nearest.
     *
     * EarthGenerator can use this when deciding which road
     * template should be drawn.
     */

    public static RoadType getNearestRoadType(
            double latitude,
            double longitude
    ) {

        if (!loaded) {

            return RoadType.NONE;
        }


        double freewayDistance =
                getDistanceToNearestFreewayMeters(
                        latitude,
                        longitude
                );


        double highwayDistance =
                getDistanceToNearestHighwayMeters(
                        latitude,
                        longitude
                );


        double localDistance =
                getDistanceToNearestLocalRoadMeters(
                        latitude,
                        longitude
                );


        /*
         * Higher road class wins ties.
         */

        if (freewayDistance
                <= highwayDistance
                && freewayDistance
                <= localDistance) {

            return RoadType.FREEWAY;
        }


        if (highwayDistance
                <= localDistance) {

            return RoadType.HIGHWAY;
        }


        if (Double.isFinite(
                localDistance
        )) {

            return RoadType.LOCAL;
        }


        return RoadType.NONE;
    }


    /*
     * ============================================================
     * DISTANCE TO SPECIFIC ROAD TYPE
     * ============================================================
     */

    public static double getDistanceToRoadTypeMeters(
            RoadType roadType,
            double latitude,
            double longitude
    ) {

        if (roadType == null) {

            return Double.POSITIVE_INFINITY;
        }


        switch (roadType) {

            case FREEWAY:

                return getDistanceToNearestFreewayMeters(
                        latitude,
                        longitude
                );


            case HIGHWAY:

                return getDistanceToNearestHighwayMeters(
                        latitude,
                        longitude
                );


            case LOCAL:

                return getDistanceToNearestLocalRoadMeters(
                        latitude,
                        longitude
                );


            case NONE:
            default:

                return Double.POSITIVE_INFINITY;
        }
    }


    /*
     * ============================================================
     * DISTANCE TO NEAREST ROAD
     * ============================================================
     *
     * Keep this original public helper so existing EarthBound
     * code continues to work.
     */

    public static double getDistanceToNearestRoadMeters(
            double latitude,
            double longitude
    ) {

        if (!loaded) {

            return Double.POSITIVE_INFINITY;
        }


        double nearestDistance =
                Double.POSITIVE_INFINITY;


        nearestDistance =
                Math.min(
                        nearestDistance,
                        getNearestDistance(
                                primaryRoads,
                                latitude,
                                longitude
                        )
                );


        nearestDistance =
                Math.min(
                        nearestDistance,
                        getNearestDistance(
                                secondaryRoads,
                                latitude,
                                longitude
                        )
                );


        nearestDistance =
                Math.min(
                        nearestDistance,
                        getNearestDistance(
                                localRoads,
                                latitude,
                                longitude
                        )
                );


        return nearestDistance;
    }


    /*
     * ============================================================
     * ROAD CENTERLINE DISTANCE CHECK
     * ============================================================
     */

    private static boolean isNearRoad(
            List<RoadSegment> roads,
            double latitude,
            double longitude,
            double radiusMeters
    ) {

        for (
                RoadSegment segment
                : roads
        ) {

            double distance =
                    distanceToSegmentMeters(
                            latitude,
                            longitude,
                            segment
                    );


            if (distance
                    <= radiusMeters) {

                return true;
            }
        }


        return false;
    }


    /*
     * ============================================================
     * NEAREST DISTANCE IN A ROAD LIST
     * ============================================================
     */

    private static double getNearestDistance(
            List<RoadSegment> roads,
            double latitude,
            double longitude
    ) {

        double nearestDistance =
                Double.POSITIVE_INFINITY;


        for (
                RoadSegment segment
                : roads
        ) {

            double distance =
                    distanceToSegmentMeters(
                            latitude,
                            longitude,
                            segment
                    );


            if (distance
                    < nearestDistance) {

                nearestDistance =
                        distance;
            }
        }


        return nearestDistance;
    }


    /*
     * ============================================================
     * DISTANCE FROM POINT TO ROAD SEGMENT
     * ============================================================
     *
     * Approximate local latitude / longitude as meter coordinates.
     *
     * This remains appropriate for the Guemes / Anacortes
     * development area.
     */

    private static double distanceToSegmentMeters(
            double latitude,
            double longitude,
            RoadSegment segment
    ) {

        double referenceLatitude =
                Math.toRadians(
                        latitude
                );


        double metersPerLongitudeDegree =
                111320.0
                        * Math.cos(
                                referenceLatitude
                        );


        double px =
                longitude
                        * metersPerLongitudeDegree;


        double py =
                latitude
                        * 111320.0;


        double ax =
                segment.longitude1
                        * metersPerLongitudeDegree;


        double ay =
                segment.latitude1
                        * 111320.0;


        double bx =
                segment.longitude2
                        * metersPerLongitudeDegree;


        double by =
                segment.latitude2
                        * 111320.0;


        double dx =
                bx - ax;


        double dy =
                by - ay;


        double lengthSquared =
                dx * dx
                        + dy * dy;


        /*
         * Zero-length segment safety.
         */

        if (lengthSquared
                == 0.0) {


            double xDifference =
                    px - ax;


            double yDifference =
                    py - ay;


            return Math.sqrt(
                    xDifference
                            * xDifference
                            + yDifference
                            * yDifference
            );
        }


        /*
         * Find nearest point along the line segment.
         */

        double t =
                (
                        (px - ax)
                                * dx

                                + (py - ay)
                                * dy
                )
                        / lengthSquared;


        t =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                t
                        )
                );


        double nearestX =
                ax
                        + t
                        * dx;


        double nearestY =
                ay
                        + t
                        * dy;


        double xDifference =
                px - nearestX;


        double yDifference =
                py - nearestY;


        return Math.sqrt(
                xDifference
                        * xDifference
                        + yDifference
                        * yDifference
        );
    }


    /*
     * ============================================================
     * STATUS
     * ============================================================
     */

    public static boolean isLoaded() {

        return loaded;
    }


    /*
     * ============================================================
     * URL ENCODING
     * ============================================================
     */

    private static String encode(
            String value
    ) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }
}
