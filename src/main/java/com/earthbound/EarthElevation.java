package com.earthbound;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EarthElevation {

    // EarthBound scale:
    // 1 Minecraft block = 2 real-world meters
    private static final double METERS_PER_BLOCK = 2.0;

    // Minecraft ocean surface
    private static final int SEA_LEVEL = 63;

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

                System.out.println(
                        "[EarthBound] USGS response: "
                        + response
                );

                double elevation =
                        parseElevation(
                                response.toString()
                        );

                System.out.println(
                        "[EarthBound] Parsed elevation: "
                        + elevation
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
     * Converts real-world elevation into
     * an EarthBound Minecraft Y coordinate.
     *
     * EarthBound scale:
     * 2 real meters = 1 Minecraft block.
     *
     * Real sea level = Minecraft Y 63.
     */
    public static int getMinecraftHeight(
            double elevationMeters) {

        double blocksAboveSeaLevel =
                elevationMeters / METERS_PER_BLOCK;

        return SEA_LEVEL +
                (int) Math.round(blocksAboveSeaLevel);
    }


    private static double parseElevation(
            String json) {

        /*
         * USGS currently returns elevation like:
         *
         * "value":"53.009941101"
         */

        String marker =
                "\"value\":\"";

        int start =
                json.indexOf(marker);

        if (start == -1) {

            System.out.println(
                    "[EarthBound] Could not find elevation value in response."
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
