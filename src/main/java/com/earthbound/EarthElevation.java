package com.earthbound;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EarthElevation {

    private static final int SEA_LEVEL = 63;
    private static final int MAX_TERRAIN_Y = 315;
    private static final double EVEREST_METERS = 8848.86;

    private static final Map<String, Double> CACHE =
            new ConcurrentHashMap<>();

    public static double getElevation(
            double latitude,
            double longitude) {

        double roundedLat =
                Math.round(latitude * 100000.0) / 100000.0;

        double roundedLon =
                Math.round(longitude * 100000.0) / 100000.0;

        String key =
                roundedLat + "," + roundedLon;

        Double cached = CACHE.get(key);

        if (cached != null) {
            return cached;
        }

        try {

            String address =
                    "https://epqs.nationalmap.gov/v1/json"
                    + "?x=" + roundedLon
                    + "&y=" + roundedLat
                    + "&units=Meters"
                    + "&wkid=4326"
                    + "&includeDate=false";

            System.out.println(
                    "[EarthBound] Requesting elevation: "
                    + address
            );

            URL url =
                    URI.create(address).toURL();

            HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode =
                    connection.getResponseCode();

            System.out.println(
                    "[EarthBound] USGS response code: "
                    + responseCode
            );

            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         connection.getInputStream()))) {

                StringBuilder response =
                        new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                double elevation =
                        parseElevation(response.toString());

                System.out.println(
                        "[EarthBound] Real elevation: "
                        + elevation
                        + " meters"
                );

                CACHE.put(key, elevation);

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
        }
    }

    /*
     * Progressive EarthBound vertical scale:
     *
     * 0 m       -> Y 63
     * 500 m     -> Y 130
     * 1000 m    -> Y 160
     * 3286 m    -> Y 225   (Mount Baker)
     * 4392 m    -> Y 245   (Mount Rainier)
     * 8848.86 m -> Y 315   (Mount Everest)
     */
    public static int getMinecraftHeight(
            double elevationMeters) {

        if (elevationMeters <= 0.0) {
            return SEA_LEVEL;
        }

        double minecraftHeight;

        if (elevationMeters <= 500.0) {

            minecraftHeight =
                    interpolate(
                            elevationMeters,
                            0.0,
                            500.0,
                            63.0,
                            130.0
                    );

        } else if (elevationMeters <= 1000.0) {

            minecraftHeight =
                    interpolate(
                            elevationMeters,
                            500.0,
                            1000.0,
                            130.0,
                            160.0
                    );

        } else if (elevationMeters <= 3286.0) {

            minecraftHeight =
                    interpolate(
                            elevationMeters,
                            1000.0,
                            3286.0,
                            160.0,
                            225.0
                    );

        } else if (elevationMeters <= 4392.0) {

            minecraftHeight =
                    interpolate(
                            elevationMeters,
                            3286.0,
                            4392.0,
                            225.0,
                            245.0
                    );

        } else {

            double limitedElevation =
                    Math.min(
                            elevationMeters,
                            EVEREST_METERS
                    );

            minecraftHeight =
                    interpolate(
                            limitedElevation,
                            4392.0,
                            EVEREST_METERS,
                            245.0,
                            MAX_TERRAIN_Y
                    );
        }

        int finalHeight =
                (int) Math.round(minecraftHeight);

        return Math.min(
                finalHeight,
                MAX_TERRAIN_Y
        );
    }

    private static double interpolate(
            double value,
            double inputMin,
            double inputMax,
            double outputMin,
            double outputMax) {

        double percentage =
                (value - inputMin)
                        / (inputMax - inputMin);

        return outputMin
                + percentage
                * (outputMax - outputMin);
    }

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

        start += marker.length();

        int end = start;

        while (end < json.length()) {

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
                    json.substring(start, end)
            );

        } catch (Exception exception) {

            System.out.println(
                    "[EarthBound] Could not parse elevation number."
            );

            return 0.0;
        }
    }
}
