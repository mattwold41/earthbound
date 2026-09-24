package com.earthbound;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EarthElevation {

    // Keeps elevations we've already downloaded in memory.
    private static final Map<String, Double> CACHE =
            new ConcurrentHashMap<>();

    /**
     * Returns real-world ground elevation in meters.
     *
     * If USGS cannot be reached, this returns 0.
     */
    public static double getElevation(
            double latitude,
            double longitude) {

        // Round the location before caching.
        // This prevents nearly identical coordinates
        // from creating unnecessary requests.
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

            URL url = URI.create(address).toURL();

            HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);

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

            return 0.0;
        }
    }


    private static double parseElevation(String json) {

        String marker = "\"value\":";

        int start = json.indexOf(marker);

        if (start == -1) {
            return 0.0;
        }

        start += marker.length();

        int end = start;

        while (end < json.length()) {

            char character =
                    json.charAt(end);

            if ((character >= '0' && character <= '9')
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

            return 0.0;
        }
    }
}
