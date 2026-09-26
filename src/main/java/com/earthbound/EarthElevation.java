package com.earthbound;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EarthElevation {

    /*
     * Minecraft sea level.
     */
    private static final int SEA_LEVEL =
            63;

    /*
     * Highest terrain block EarthBound
     * will generate.
     *
     * This leaves a small amount of room
     * below Minecraft's upper build limit.
     */
    private static final int MAX_TERRAIN_Y =
            315;

    /*
     * Real-world elevation of
     * Mount Everest in meters.
     */
    private static final double EVEREST_METERS =
            8848.86;

    /*
     * Cache used by /earth locate
     * elevation lookups.
     */
    private static final Map<String, Double> CACHE =
            new ConcurrentHashMap<>();


    private EarthElevation() {
        // Utility class
    }


    /*
     * Downloads a real-world elevation
     * from the USGS Elevation Point
     * Query Service.
     *
     * This is primarily used by
     * /earth locate.
     *
     * World generation itself currently
     * uses EarthTerrainLoader's raster.
     */
    public static double getElevation(
            double latitude,
            double longitude) {

        double roundedLat =
                Math.round(
                        latitude * 100000.0
                ) / 100000.0;

        double roundedLon =
                Math.round(
                        longitude * 100000.0
                ) / 100000.0;

        String key =
                roundedLat
                        + ","
                        + roundedLon;

        Double cached =
                CACHE.get(key);

        if (cached != null) {

            return cached;
        }

        HttpURLConnection connection =
                null;

        try {

            String address =
                    "https://epqs.nationalmap.gov/v1/json"
                            + "?x="
                            + roundedLon
                            + "&y="
                            + roundedLat
                            + "&units=Meters"
                            + "&wkid=4326"
                            + "&includeDate=false";

            System.out.println(
                    "[EarthBound] Requesting elevation: "
                            + address
            );

            URL url =
                    URI.create(address)
                            .toURL();

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod(
                    "GET"
            );

            connection.setConnectTimeout(
                    5000
            );

            connection.setReadTimeout(
                    5000
            );

            int responseCode =
                    connection.getResponseCode();

            System.out.println(
                    "[EarthBound] USGS response code: "
                            + responseCode
            );

            if (responseCode != 200) {

                return 0.0;
            }

            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         connection
                                                 .getInputStream()
                                 )
                         )) {

                StringBuilder response =
                        new StringBuilder();

                String line;

                while ((line =
                        reader.readLine())
                        != null) {

                    response.append(line);
                }

                double elevation =
                        parseElevation(
                                response.toString()
                        );

                System.out.println(
                        "[EarthBound] Real elevation: "
                                + elevation
                                + " meters"
                );

                CACHE.put(
                        key,
                        elevation
                );

                return elevation;
            }

        } catch (Exception exception) {

            System.out.println(
                    "[EarthBound] Elevation download failed for "
                            + latitude
                            + ", "
                            + longitude
            );

            exception.printStackTrace();

            return 0.0;

        } finally {

            if (connection != null) {

                connection.disconnect();
            }
        }
    }


    /*
     * EarthBound progressive vertical scale.
     *
     * Horizontal scale remains:
     *
     * 1 Minecraft block =
     * 1 real-world meter.
     *
     * Vertical scaling is separate.
     *
     * Low terrain is preserved much more
     * strongly than tall mountains.
     *
     * Approximate reference points:
     *
     * Real elevation      Minecraft Y
     *
     * 0 m                 63
     * 50 m                113
     * 100 m               153
     * 250 m               180
     * 500 m               200
     * 1000 m              220
     * 2000 m              240
     * 3286 m              258
     * 4392 m              270
     * 6000 m              288
     * 8848.86 m           315
     *
     * This intentionally compresses
     * taller terrain progressively so
     * Mount Baker, Mount Rainier,
     * Everest, and other high mountains
     * can fit within Minecraft.
     */
    public static int getMinecraftHeight(
            double elevationMeters) {

        /*
         * Ocean / sea-level terrain.
         */
        if (elevationMeters <= 0.0) {

            return SEA_LEVEL;
        }

        double minecraftHeight;


        /*
         * 0 - 50 meters
         *
         * Keep very low terrain at
         * approximately 1:1 vertically.
         *
         * This is important for islands,
         * shorelines, valleys, and cities.
         */
        if (elevationMeters <= 50.0) {

            minecraftHeight =
                    SEA_LEVEL
                            + elevationMeters;


        /*
         * 50 - 100 meters
         *
         * Begin gentle compression.
         */
        } else if (
                elevationMeters <= 100.0) {

            minecraftHeight =
                    interpolate(
                            elevationMeters,
                            50.0,
                            100.0,
                            113.0,
                            153.0
                    );


        /*
         * 100 - 250 meters
         */
        } else if (
                elevationMeters <= 250.0) {

            minecraftHeight =
                    interpolate(
                            elevationMeters,
                            100.0,
                            250.0,
                            153.0,
                            180.0
                    );


        /*
         * 250 - 500 meters
         */
        } else if (
                elevationMeters <= 500.0) {

            minecraftHeight =
                    interpolate(
                            elevationMeters,
                            250.0,
                            500.0,
                            180.0,
                            200.0
                    );


        /*
         * 500 - 1000 meters
         */
        } else if (
                elevationMeters <= 1000.0) {

            minecraftHeight =
                    interpolate(
                            elevationMeters,
                            500.0,
                            1000.0,
                            200.0,
                            220.0
                    );


        /*
         * 1000 - 2000 meters
         */
        } else if (
                elevationMeters <= 2000.0) {

            minecraftHeight =
                    interpolate(
                            elevationMeters,
                            1000.0,
                            2000.0,
                            220.0,
                            240.0
                    );


        /*
         * 2000 - Mount Baker summit
         *
         * Mount Baker is approximately
         * 3286 meters.
         */
        } else if (
                elevationMeters <= 3286.0) {

            minecraftHeight =
                    interpolate(
                            elevationMeters,
                            2000.0,
                            3286.0,
                            240.0,
                            258.0
                    );


        /*
         * Mount Baker - Mount Rainier.
         *
         * Rainier is approximately
         * 4392 meters.
         */
        } else if (
                elevationMeters <= 4392.0) {

            minecraftHeight =
                    interpolate(
                            elevationMeters,
                            3286.0,
                            4392.0,
                            258.0,
                            270.0
                    );


        /*
         * Rainier - 6000 meters.
         */
        } else if (
                elevationMeters <= 6000.0) {

            minecraftHeight =
                    interpolate(
                            elevationMeters,
                            4392.0,
                            6000.0,
                            270.0,
                            288.0
                    );


        /*
         * 6000 meters - Everest.
         */
        } else {

            double limitedElevation =
                    Math.min(
                            elevationMeters,
                            EVEREST_METERS
                    );

            minecraftHeight =
                    interpolate(
                            limitedElevation,
                            6000.0,
                            EVEREST_METERS,
                            288.0,
                            MAX_TERRAIN_Y
                    );
        }


        int finalHeight =
                (int) Math.round(
                        minecraftHeight
                );


        /*
         * Safety limit.
         */
        return Math.min(
                finalHeight,
                MAX_TERRAIN_Y
        );
    }


    /*
     * Linear interpolation between
     * two vertical-scale control points.
     */
    private static double interpolate(
            double value,
            double inputMin,
            double inputMax,
            double outputMin,
            double outputMax) {

        double percentage =
                (value - inputMin)
                        / (inputMax
                        - inputMin);

        return outputMin
                + percentage
                * (outputMax
                - outputMin);
    }


    /*
     * Reads the elevation value returned
     * by the USGS JSON response.
     */
    private static double parseElevation(
            String json) {

        String marker =
                "\"value\":\"";

        int start =
                json.indexOf(marker);

        if (start == -1) {

            System.out.println(
                    "[EarthBound] Could not find elevation value."
            );

            return 0.0;
        }

        start +=
                marker.length();

        int end =
                start;

        while (end
                < json.length()) {

            char character =
                    json.charAt(end);

            if ((character >= '0'
                    && character <= '9')
                    || character == '.'
                    || character == '-') {

                end++;

            } else {

                break;
            }
        }

        try {

            return Double.parseDouble(
                    json.substring(
                            start,
                            end
                    )
            );

        } catch (Exception exception) {

            System.out.println(
                    "[EarthBound] Could not parse elevation number."
            );

            return 0.0;
        }
    }
}
