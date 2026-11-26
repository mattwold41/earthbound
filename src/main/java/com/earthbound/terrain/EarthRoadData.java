package com.earthbound.roads;

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

    private static final double WEST = -122.70;
    private static final double SOUTH = 48.47;
    private static final double EAST = -122.55;
    private static final double NORTH = 48.60;

    public static final int FREEWAY_WIDTH = 10;
    public static final int HIGHWAY_WIDTH = 8;
    public static final int LOCAL_WIDTH = 5;

    private static final double METERS_PER_BLOCK = 2.0;

    private static final int MAX_DOWNLOAD_ATTEMPTS = 3;
    private static final int CONNECT_TIMEOUT_MS = 20000;
    private static final int READ_TIMEOUT_MS = 60000;
    private static final long RETRY_DELAY_MS = 3000L;

    private static final String ROAD_SERVICE =
            "https://tigerweb.geo.census.gov/arcgis/rest/services/"
                    + "TIGERweb/Transportation/MapServer";

    private static final int PRIMARY_LAYER = 2;
    private static final int SECONDARY_LAYER = 6;
    private static final int LOCAL_LAYER = 8;

    private static final List<RoadSegment> primaryRoads =
            new ArrayList<>();

    private static final List<RoadSegment> secondaryRoads =
            new ArrayList<>();

    private static final List<RoadSegment> localRoads =
            new ArrayList<>();

    private static final List<List<RoadPoint>> primaryCenterlines =
            new ArrayList<>();

    private static final List<List<RoadPoint>> secondaryCenterlines =
            new ArrayList<>();

    private static final List<List<RoadPoint>> localCenterlines =
            new ArrayList<>();

    private static volatile boolean loaded = false;

    private EarthRoadData() {
    }


    /*
     * ========================================================
     * ROAD TYPES
     * ========================================================
     */

    public enum RoadType {

        NONE(0),
        LOCAL(LOCAL_WIDTH),
        HIGHWAY(HIGHWAY_WIDTH),
        FREEWAY(FREEWAY_WIDTH);

        private final int width;

        RoadType(int width) {

            this.width = width;

        }

        public int getWidth() {

            return width;

        }

        public double getRadius() {

            return width / 2.0;

        }

    }


    /*
     * ========================================================
     * GEOGRAPHIC ROAD POINT
     * ========================================================
     */

    public static final class RoadPoint {

        private final double latitude;

        private final double longitude;

        public RoadPoint(
                double latitude,
                double longitude
        ) {

            if (!Double.isFinite(latitude)
                    || !Double.isFinite(longitude)
                    || latitude < -90.0
                    || latitude > 90.0
                    || longitude < -180.0
                    || longitude > 180.0) {

                throw new IllegalArgumentException(
                        "Invalid geographic road point."
                );

            }

            this.latitude = latitude;

            this.longitude = longitude;

        }

        public double getLatitude() {

            return latitude;

        }

        public double getLongitude() {

            return longitude;

        }

    }


    /*
     * ========================================================
     * ROAD SEGMENT
     * ========================================================
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

            this.latitude1 = latitude1;
            this.longitude1 = longitude1;

            this.latitude2 = latitude2;
            this.longitude2 = longitude2;

        }

    }


    /*
     * ========================================================
     * DOWNLOADED ROAD LAYER
     * ========================================================
     */

    private static final class RoadLayer {

        private final List<RoadSegment> segments =
                new ArrayList<>();

        private final List<List<RoadPoint>> centerlines =
                new ArrayList<>();

    }


    /*
     * ========================================================
     * ROAD CENTERLINE ACCESS
     * ========================================================
     *
     * Used by the EarthBound road elevation smoothing system.
     *
     * Original geographic road points remain in their
     * downloaded order.
     */

    public static List<List<RoadPoint>> getRoadCenterlines(
            RoadType type
    ) {

        if (!loaded || type == null) {

            return List.of();

        }

        switch (type) {

            case FREEWAY:

                return List.copyOf(primaryCenterlines);

            case HIGHWAY:

                return List.copyOf(secondaryCenterlines);

            case LOCAL:

                return List.copyOf(localCenterlines);

            case NONE:
            default:

                return List.of();

        }

    }


    /*
     * ========================================================
     * LOAD ROAD DATA
     * ========================================================
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

        primaryCenterlines.clear();
        secondaryCenterlines.clear();
        localCenterlines.clear();

        boolean primaryLoaded =
                loadLayerWithRetry(
                        PRIMARY_LAYER,
                        "primary roads",
                        primaryRoads,
                        primaryCenterlines
                );

        boolean secondaryLoaded =
                loadLayerWithRetry(
                        SECONDARY_LAYER,
                        "secondary roads",
                        secondaryRoads,
                        secondaryCenterlines
                );

        boolean localLoaded =
                loadLayerWithRetry(
                        LOCAL_LAYER,
                        "local roads",
                        localRoads,
                        localCenterlines
                );

        if (!primaryLoaded
                || !secondaryLoaded
                || !localLoaded) {

            primaryRoads.clear();
            secondaryRoads.clear();
            localRoads.clear();

            primaryCenterlines.clear();
            secondaryCenterlines.clear();
            localCenterlines.clear();

            loaded = false;

            System.out.println(
                    "[EarthBound] Vector road data "
                            + "could not be completely loaded."
            );

            return false;

        }

        loaded = true;

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
                "[EarthBound] Road centerlines: primary="
                        + primaryCenterlines.size()
                        + ", secondary="
                        + secondaryCenterlines.size()
                        + ", local="
                        + localCenterlines.size()
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
     * Compatibility method.
     */

    public static void load() {

        loadGuemesRoadMask();

    }


    /*
     * ========================================================
     * DOWNLOAD WITH RETRIES
     * ========================================================
     */

    private static boolean loadLayerWithRetry(
            int layer,
            String layerName,
            List<RoadSegment> destination,
            List<List<RoadPoint>> centerlineDestination
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

                RoadLayer downloaded =
                        downloadLayer(
                                layer,
                                layerName
                        );

                if (downloaded != null) {

                    destination.clear();

                    destination.addAll(
                            downloaded.segments
                    );

                    centerlineDestination.clear();

                    centerlineDestination.addAll(
                            downloaded.centerlines
                    );

                    System.out.println(
                            "[EarthBound] "
                                    + layerName
                                    + " loaded with "
                                    + destination.size()
                                    + " centerline segments and "
                                    + centerlineDestination.size()
                                    + " ordered centerlines."
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
                                + exception.getClass().getSimpleName()
                                + ": "
                                + exception.getMessage()
                );

            }

            if (attempt < MAX_DOWNLOAD_ATTEMPTS) {

                System.out.println(
                        "[EarthBound] Waiting "
                                + (RETRY_DELAY_MS / 1000)
                                + " seconds before retrying "
                                + layerName
                                + "..."
                );

                try {

                    Thread.sleep(RETRY_DELAY_MS);

                } catch (InterruptedException exception) {

                    Thread.currentThread().interrupt();

                    return false;

                }

            }

        }

        return false;

    }


    /*
     * ========================================================
     * DOWNLOAD TIGERWEB LAYER
     * ========================================================
     */

    private static RoadLayer downloadLayer(
            int layer,
            String layerName
    ) throws Exception {

        String envelope =
                WEST + ","
                        + SOUTH + ","
                        + EAST + ","
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
                        + encode("esriGeometryEnvelope")
                        + "&inSR=4326"
                        + "&spatialRel="
                        + encode("esriSpatialRelIntersects")
                        + "&outFields="
                        + encode("OBJECTID,NAME,BASENAME,MTFCC,RTTYP")
                        + "&returnGeometry=true"
                        + "&outSR=4326"
                        + "&f=geojson";

        URL url =
                URI.create(request).toURL();

        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

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
                    connection.getResponseCode();

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

                while ((line = reader.readLine()) != null) {

                    builder.append(line);

                }

                json = builder.toString();

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
     * ========================================================
     * PARSE GEOJSON
     * ========================================================
     */

    private static RoadLayer parseGeoJson(
            String json,
            String layerName
    ) {

        RoadLayer result =
                new RoadLayer();

        JsonObject root =
                JsonParser.parseString(json)
                        .getAsJsonObject();

        if (root.has("error")
                || !root.has("features")
                || !root.get("features").isJsonArray()) {

            throw new IllegalStateException(
                    layerName
                            + " returned invalid GeoJSON."
            );

        }

        if (root.has("exceededTransferLimit")
                && root.get("exceededTransferLimit")
                .getAsBoolean()) {

            throw new IllegalStateException(
                    layerName
                            + " exceeded the download transfer limit."
            );

        }

        JsonArray features =
                root.getAsJsonArray("features");

        System.out.println(
                "[EarthBound] "
                        + layerName
                        + " feature count="
                        + features.size()
        );

        for (JsonElement featureElement : features) {

            if (!featureElement.isJsonObject()) {

                continue;

            }

            JsonObject feature =
                    featureElement.getAsJsonObject();

            if (!feature.has("geometry")
                    || feature.get("geometry").isJsonNull()) {

                continue;

            }

            JsonObject geometry =
                    feature.getAsJsonObject("geometry");

            if (!geometry.has("type")
                    || !geometry.has("coordinates")) {

                continue;

            }

            String geometryType =
                    geometry.get("type").getAsString();

            JsonArray coordinates =
                    geometry.getAsJsonArray("coordinates");

            if ("LineString".equals(geometryType)) {

                addLineString(
                        coordinates,
                        result
                );

            } else if (
                    "MultiLineString".equals(geometryType)
            ) {

                for (JsonElement lineElement : coordinates) {

                    if (lineElement.isJsonArray()) {

                        addLineString(
                                lineElement.getAsJsonArray(),
                                result
                        );

                    }

                }

            }

        }

        return result;

    }


    /*
     * ========================================================
     * PRESERVE ORDERED ROAD CENTERLINES
     * ========================================================
     *
     * GeoJSON coordinates:
     *
     * [longitude, latitude]
     */

    private static void addLineString(
            JsonArray coordinates,
            RoadLayer result
    ) {

        if (coordinates.size() < 2) {

            return;

        }

        List<RoadPoint> points =
                new ArrayList<>();

        for (JsonElement element : coordinates) {

            if (!element.isJsonArray()) {

                storeCenterline(points, result);

                points.clear();

                continue;

            }

            JsonArray coordinate =
                    element.getAsJsonArray();

            if (coordinate.size() < 2) {

                storeCenterline(points, result);

                points.clear();

                continue;

            }

            double longitude =
                    coordinate.get(0).getAsDouble();

            double latitude =
                    coordinate.get(1).getAsDouble();

            RoadPoint point =
                    new RoadPoint(
                            latitude,
                            longitude
                    );

            if (!points.isEmpty()) {

                RoadPoint previous =
                        points.get(points.size() - 1);

                if (previous.getLatitude() == latitude
                        && previous.getLongitude() == longitude) {

                    continue;

                }

            }

            points.add(point);

        }

        storeCenterline(
                points,
                result
        );

    }


    /*
     * ========================================================
     * STORE CENTERLINE AND COMPATIBLE ROAD SEGMENTS
     * ========================================================
     */

    private static void storeCenterline(
            List<RoadPoint> points,
            RoadLayer result
    ) {

        if (points.size() < 2) {

            return;

        }

        result.centerlines.add(
                List.copyOf(points)
        );

        for (int i = 0; i < points.size() - 1; i++) {

            RoadPoint first =
                    points.get(i);

            RoadPoint second =
                    points.get(i + 1);

            result.segments.add(
                    new RoadSegment(
                            first.getLatitude(),
                            first.getLongitude(),
                            second.getLatitude(),
                            second.getLongitude()
                    )
            );

        }

    }


    /*
     * ========================================================
     * ROAD CLASSIFICATION
     * ========================================================
     *
     * Higher road classes take priority.
     *
     * Road widths are measured in Minecraft blocks.
     * Geographic distances are measured in meters.
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
                RoadType.FREEWAY.getRadius()
                        * METERS_PER_BLOCK
        )) {

            return RoadType.FREEWAY;

        }

        if (isNearRoad(
                secondaryRoads,
                latitude,
                longitude,
                RoadType.HIGHWAY.getRadius()
                        * METERS_PER_BLOCK
        )) {

            return RoadType.HIGHWAY;

        }

        if (isNearRoad(
                localRoads,
                latitude,
                longitude,
                RoadType.LOCAL.getRadius()
                        * METERS_PER_BLOCK
        )) {

            return RoadType.LOCAL;

        }

        return RoadType.NONE;

    }


    /*
     * ========================================================
     * IS ROAD
     * ========================================================
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
     * Legacy compatibility helper.
     */

    public static boolean isRoad(
            int x,
            int z
    ) {

        return false;

    }


    /*
     * ========================================================
     * ROAD WIDTH
     * ========================================================
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
     * ========================================================
     * CLASS-SPECIFIC ROAD DISTANCES
     * ========================================================
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
     * ========================================================
     * NEAREST ROAD TYPE
     * ========================================================
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

        if (Double.isFinite(freewayDistance)
                && freewayDistance <= highwayDistance
                && freewayDistance <= localDistance) {

            return RoadType.FREEWAY;

        }

        if (Double.isFinite(highwayDistance)
                && highwayDistance <= localDistance) {

            return RoadType.HIGHWAY;

        }

        if (Double.isFinite(localDistance)) {

            return RoadType.LOCAL;

        }

        return RoadType.NONE;

    }


    /*
     * ========================================================
     * DISTANCE TO SPECIFIC ROAD TYPE
     * ========================================================
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
     * ========================================================
     * DISTANCE TO NEAREST ROAD
     * ========================================================
     */

    public static double getDistanceToNearestRoadMeters(
            double latitude,
            double longitude
    ) {

        if (!loaded) {

            return Double.POSITIVE_INFINITY;

        }

        return Math.min(
                getDistanceToNearestFreewayMeters(
                        latitude,
                        longitude
                ),
                Math.min(
                        getDistanceToNearestHighwayMeters(
                                latitude,
                                longitude
                        ),
                        getDistanceToNearestLocalRoadMeters(
                                latitude,
                                longitude
                        )
                )
        );

    }


    /*
     * ========================================================
     * ROAD CENTERLINE DISTANCE CHECK
     * ========================================================
     */

    private static boolean isNearRoad(
            List<RoadSegment> roads,
            double latitude,
            double longitude,
            double radiusMeters
    ) {

        for (RoadSegment segment : roads) {

            double distance =
                    distanceToSegmentMeters(
                            latitude,
                            longitude,
                            segment
                    );

            if (distance <= radiusMeters) {

                return true;

            }

        }

        return false;

    }


    /*
     * ========================================================
     * NEAREST DISTANCE IN A ROAD LIST
     * ========================================================
     */

    private static double getNearestDistance(
            List<RoadSegment> roads,
            double latitude,
            double longitude
    ) {

        double nearestDistance =
                Double.POSITIVE_INFINITY;

        for (RoadSegment segment : roads) {

            double distance =
                    distanceToSegmentMeters(
                            latitude,
                            longitude,
                            segment
                    );

            if (distance < nearestDistance) {

                nearestDistance = distance;

            }

        }

        return nearestDistance;

    }


    /*
     * ========================================================
     * DISTANCE FROM POINT TO ROAD SEGMENT
     * ========================================================
     */

    private static double distanceToSegmentMeters(
            double latitude,
            double longitude,
            RoadSegment segment
    ) {

        double metersPerLongitudeDegree =
                111320.0
                        * Math.cos(
                                Math.toRadians(latitude)
                        );

        double ax =
                (segment.longitude1 - longitude)
                        * metersPerLongitudeDegree;

        double ay =
                (segment.latitude1 - latitude)
                        * 111320.0;

        double bx =
                (segment.longitude2 - longitude)
                        * metersPerLongitudeDegree;

        double by =
                (segment.latitude2 - latitude)
                        * 111320.0;

        double dx = bx - ax;

        double dy = by - ay;

        double lengthSquared =
                dx * dx + dy * dy;

        if (lengthSquared == 0.0) {

            return Math.hypot(ax, ay);

        }

        double t =
                (-ax * dx - ay * dy)
                        / lengthSquared;

        t = Math.max(
                0.0,
                Math.min(1.0, t)
        );

        double nearestX =
                ax + t * dx;

        double nearestY =
                ay + t * dy;

        return Math.hypot(
                nearestX,
                nearestY
        );

    }


    /*
     * ========================================================
     * STATUS
     * ========================================================
     */

    public static boolean isLoaded() {

        return loaded;

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

}
