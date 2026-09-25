package com.earthbound;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class EarthTerrainLoader {

    /*
     * USGS 3DEP Elevation ImageServer.
     *
     * EarthBound will eventually request rectangular
     * terrain tiles from this service and cache them
     * before Minecraft generates the corresponding chunks.
     */
    private static final String USGS_3DEP_SERVICE =
            "https://elevation.nationalmap.gov/arcgis/rest/services/"
            + "3DEPElevation/ImageServer";


    private EarthTerrainLoader() {
        // Utility class.
    }


    /*
     * Existing point loader.
     *
     * We keep this for testing and fallback use.
     */
    public static double loadElevation(
            double latitude,
            double longitude) {

        Double cachedElevation =
                EarthTerrainData.getElevation(
                        latitude,
                        longitude
                );

        if (cachedElevation != null) {
            return cachedElevation;
        }

        double elevation =
                EarthElevation.getElevation(
                        latitude,
                        longitude
                );

        EarthTerrainData.storeElevation(
                latitude,
                longitude,
                elevation
        );

        return elevation;
    }


    public static int loadMinecraftHeight(
            double latitude,
            double longitude) {

        double elevation =
                loadElevation(
                        latitude,
                        longitude
                );

        return EarthElevation.getMinecraftHeight(
                elevation
        );
    }


    /*
     * Tests communication with the USGS 3DEP
     * ImageServer before we begin downloading
     * Guemes terrain tiles.
     */
    public static boolean test3DEPConnection() {

        try {

            String address =
                    USGS_3DEP_SERVICE
                    + "?f=pjson";

            URL url =
                    URI.create(address).toURL();

            HttpURLConnection connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod("GET");

            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode =
                    connection.getResponseCode();

            if (responseCode != 200) {

                System.out.println(
                        "[EarthBound] 3DEP connection failed. HTTP "
                        + responseCode
                );

                return false;
            }

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

                boolean valid =
                        response.toString()
                                .contains(
                                        "\"name\":\"3DEPElevation\""
                                );

                if (valid) {

                    System.out.println(
                            "[EarthBound] USGS 3DEP connection successful."
                    );

                } else {

                    System.out.println(
                            "[EarthBound] USGS responded, "
                            + "but the 3DEP service was not recognized."
                    );
                }

                return valid;
            }

        } catch (Exception exception) {

            System.out.println(
                    "[EarthBound] Could not connect to USGS 3DEP."
            );

            exception.printStackTrace();

            return false;
        }
    }


    /*
     * Creates the URL that EarthBound will use
     * to request one elevation raster tile.
     *
     * west/south/east/north are longitude/latitude.
     */
    public static String createTerrainTileURL(
            double west,
            double south,
            double east,
            double north,
            int width,
            int height) {

        return USGS_3DEP_SERVICE
                + "/exportImage"
                + "?bbox="
                + west + ","
                + south + ","
                + east + ","
                + north
                + "&bboxSR=4326"
                + "&imageSR=4326"
                + "&size="
                + width + ","
                + height
                + "&format=tiff"
                + "&pixelType=F32"
                + "&interpolation=RSP_BilinearInterpolation"
                + "&f=image";
    }
}
