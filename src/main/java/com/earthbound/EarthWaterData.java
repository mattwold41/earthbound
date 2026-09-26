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

public class EarthWaterData {

    /*
     * Minecraft sea level.
     */
    public static final int SEA_LEVEL = 63;


    /*
     * Current EarthBound Guemes test area.
     */
    private static final double GUEMES_WEST =
            -122.70;

    private static final double GUEMES_SOUTH =
            48.47;

    private static final double GUEMES_EAST =
            -122.55;

    private static final double GUEMES_NORTH =
            48.60;


    /*
     * Census TIGERweb Hydro service.
     *
     * Layer 1 = Areal Hydrography.
     *
     * Unlike the old system, we are NOT
     * downloading a 512 x 512 rendered image.
     *
     * We download the actual water polygons.
     */
    private static final String HYDRO_QUERY =
            "https://tigerweb.geo.census.gov/"
                    + "arcgis/rest/services/"
                    + "TIGERweb/Hydro/MapServer/1/query";


    /*
     * All downloaded water polygons.
     */
    private static volatile List<WaterPolygon>
            waterPolygons = null;


    private EarthWaterData() {
        // Utility class
    }


    /*
     * Compatibility method.
     *
     * EarthBound.java already calls
     * loadGuemesWaterMask().
     *
     * We keep that method name so we do not
     * need to change EarthBound.java yet.
     *
     * Internally this now loads VECTOR
     * water polygons instead of a PNG mask.
     */
    public static boolean loadGuemesWaterMask() {

        HttpURLConnection connection =
                null;

        try {

            System.out.println(
                    "[EarthBound] Loading vector water polygons..."
            );


            /*
             * ArcGIS envelope:
             *
             * west,south,east,north
             */
            String geometry =
                    GUEMES_WEST
                            + ","
                            + GUEMES_SOUTH
                            + ","
                            + GUEMES_EAST
                            + ","
                            + GUEMES_NORTH;


            String address =
                    HYDRO_QUERY
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
                            + encode("OBJECTID,NAME")
                            + "&returnGeometry=true"
                            + "&outSR=4326"
                            + "&f=geojson";


            URL url =
                    URI.create(address)
                            .toURL();


            connection =
                    (HttpURLConnection)
                            url.openConnection();


            connection.setRequestMethod("GET");

            connection.setConnectTimeout(
                    20000
            );

            connection.setReadTimeout(
                    60000
            );


            int responseCode =
                    connection.getResponseCode();


            if (responseCode != 200) {

                System.out.println(
                        "[EarthBound] Vector water request failed. HTTP "
                                + responseCode
                );

                return false;
            }


            String json;


            try (InputStream input =
                         connection.getInputStream();

                 BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         input,
                                         StandardCharsets.UTF_8
                                 )
                         )) {


                StringBuilder builder =
                        new StringBuilder();


                String line;


                while ((line =
                        reader.readLine())
                        != null) {

                    builder.append(line);
                }


                json =
                        builder.toString();
            }


            JsonObject root =
                    JsonParser.parseString(json)
                            .getAsJsonObject();


            JsonArray features =
                    root.getAsJsonArray(
                            "features"
                    );


            if (features == null) {

                System.out.println(
                        "[EarthBound] No water features returned."
                );

                return false;
            }


            System.out.println(
                    "[EarthBound] Water feature count="
                            + features.size()
            );


            List<WaterPolygon> polygons =
                    new ArrayList<>();


            for (JsonElement featureElement
                    : features) {


                if (!featureElement
                        .isJsonObject()) {

                    continue;
                }


                JsonObject feature =
                        featureElement
                                .getAsJsonObject();


                JsonObject geometryObject =
                        feature.getAsJsonObject(
                                "geometry"
                        );


                if (geometryObject == null) {

                    continue;
                }


                JsonElement typeElement =
                        geometryObject.get(
                                "type"
                        );


                JsonArray coordinates =
                        geometryObject
                                .getAsJsonArray(
                                        "coordinates"
                                );


                if (typeElement == null
                        || coordinates == null) {

                    continue;
                }


                String type =
                        typeElement
                                .getAsString();


                if ("Polygon".equals(type)) {

                    addPolygon(
                            coordinates,
                            polygons
                    );

                } else if (
                        "MultiPolygon"
                                .equals(type)) {


                    for (JsonElement polygonElement
                            : coordinates) {


                        if (!polygonElement
                                .isJsonArray()) {

                            continue;
                        }


                        addPolygon(
                                polygonElement
                                        .getAsJsonArray(),
                                polygons
                        );
                    }
                }
            }


            waterPolygons =
                    polygons;


            System.out.println(
                    "[EarthBound] Vector water polygons loaded!"
            );


            System.out.println(
                    "[EarthBound] Water polygons: "
                            + polygons.size()
            );


            return true;


        } catch (Exception exception) {

            System.out.println(
                    "[EarthBound] Failed to load vector water polygons."
            );

            exception.printStackTrace();

            return false;


        } finally {

            if (connection != null) {

                connection.disconnect();
            }
        }
    }


    /*
     * Converts one GeoJSON Polygon
     * into our local polygon representation.
     *
     * GeoJSON polygon coordinates contain:
     *
     * ring 0 = outside boundary
     * ring 1+ = holes
     */
    private static void addPolygon(
            JsonArray polygonCoordinates,
            List<WaterPolygon> polygons) {


        if (polygonCoordinates.size()
                == 0) {

            return;
        }


        List<Ring> rings =
                new ArrayList<>();


        for (JsonElement ringElement
                : polygonCoordinates) {


            if (!ringElement
                    .isJsonArray()) {

                continue;
            }


            JsonArray ringCoordinates =
                    ringElement
                            .getAsJsonArray();


            List<Point> points =
                    new ArrayList<>();


            for (JsonElement pointElement
                    : ringCoordinates) {


                if (!pointElement
                        .isJsonArray()) {

                    continue;
                }


                JsonArray coordinate =
                        pointElement
                                .getAsJsonArray();


                if (coordinate.size()
                        < 2) {

                    continue;
                }


                /*
                 * GeoJSON order:
                 *
                 * [longitude, latitude]
                 */
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
                                latitude,
                                longitude
                        )
                );
            }


            if (points.size()
                    >= 3) {

                rings.add(
                        new Ring(points)
                );
            }
        }


        if (!rings.isEmpty()) {

            polygons.add(
                    new WaterPolygon(
                            rings
                    )
            );
        }
    }


    /*
     * Returns true after vector water
     * polygons have loaded.
     */
    public static boolean isLoaded() {

        return waterPolygons
                != null;
    }


    /*
     * Fast local lookup.
     *
     * There are NO Internet requests here.
     *
     * Minecraft passes the real-world
     * latitude and longitude for a block.
     * We check whether that point falls
     * inside one of the downloaded
     * hydrography polygons.
     */
    public static boolean isWater(
            double latitude,
            double longitude) {


        List<WaterPolygon> polygons =
                waterPolygons;


        if (polygons == null) {

            return false;
        }


        /*
         * Outside the current Guemes
         * test area.
         */
        if (latitude < GUEMES_SOUTH
                || latitude > GUEMES_NORTH
                || longitude < GUEMES_WEST
                || longitude > GUEMES_EAST) {

            return false;
        }


        for (WaterPolygon polygon
                : polygons) {


            if (polygon.contains(
                    latitude,
                    longitude)) {

                return true;
            }
        }


        return false;
    }


    /*
     * URL encoding helper.
     */
    private static String encode(
            String value) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }


    /*
     * One water polygon.
     *
     * First ring = outer boundary.
     * Remaining rings = holes/islands.
     */
    private static class WaterPolygon {

        private final List<Ring> rings;


        private WaterPolygon(
                List<Ring> rings) {

            this.rings =
                    rings;
        }


        private boolean contains(
                double latitude,
                double longitude) {


            if (rings.isEmpty()) {

                return false;
            }


            /*
             * Must be inside outer ring.
             */
            if (!rings.get(0)
                    .contains(
                            latitude,
                            longitude)) {

                return false;
            }


            /*
             * If inside any interior ring,
             * this point is a hole and
             * therefore not water.
             */
            for (int i = 1;
                 i < rings.size();
                 i++) {


                if (rings.get(i)
                        .contains(
                                latitude,
                                longitude)) {

                    return false;
                }
            }


            return true;
        }
    }


    /*
     * One polygon ring.
     */
    private static class Ring {

        private final List<Point> points;


        private final double minLatitude;
        private final double maxLatitude;

        private final double minLongitude;
        private final double maxLongitude;


        private Ring(
                List<Point> points) {

            this.points =
                    points;


            double minLat =
                    Double.POSITIVE_INFINITY;

            double maxLat =
                    Double.NEGATIVE_INFINITY;

            double minLon =
                    Double.POSITIVE_INFINITY;

            double maxLon =
                    Double.NEGATIVE_INFINITY;


            for (Point point
                    : points) {


                minLat =
                        Math.min(
                                minLat,
                                point.latitude
                        );


                maxLat =
                        Math.max(
                                maxLat,
                                point.latitude
                        );


                minLon =
                        Math.min(
                                minLon,
                                point.longitude
                        );


                maxLon =
                        Math.max(
                                maxLon,
                                point.longitude
                        );
            }


            minLatitude =
                    minLat;

            maxLatitude =
                    maxLat;

            minLongitude =
                    minLon;

            maxLongitude =
                    maxLon;
        }


        /*
         * Standard ray-casting
         * point-in-polygon test.
         */
        private boolean contains(
                double latitude,
                double longitude) {


            /*
             * Fast bounding-box rejection.
             */
            if (latitude < minLatitude
                    || latitude > maxLatitude
                    || longitude < minLongitude
                    || longitude > maxLongitude) {

                return false;
            }


            boolean inside =
                    false;


            int size =
                    points.size();


            for (int i = 0,
                 j = size - 1;
                 i < size;
                 j = i++) {


                Point pointI =
                        points.get(i);

                Point pointJ =
                        points.get(j);


                boolean crosses =
                        ((pointI.latitude
                                > latitude)
                                !=
                                (pointJ.latitude
                                        > latitude))
                                &&
                                (longitude
                                        <
                                        (pointJ.longitude
                                                - pointI.longitude)
                                                *
                                                (latitude
                                                        - pointI.latitude)
                                                /
                                                (pointJ.latitude
                                                        - pointI.latitude)
                                                +
                                                pointI.longitude);


                if (crosses) {

                    inside =
                            !inside;
                }
            }


            return inside;
        }
    }


    /*
     * Real-world geographic point.
     */
    private static class Point {

        private final double latitude;
        private final double longitude;


        private Point(
                double latitude,
                double longitude) {

            this.latitude =
                    latitude;

            this.longitude =
                    longitude;
        }
    }
}
