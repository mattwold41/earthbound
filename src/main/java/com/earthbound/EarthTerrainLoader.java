package com.earthbound;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class EarthTerrainLoader {

    /*
     * Official USGS 3DEP Elevation ImageServer.
     */
    private static final String USGS_3DEP_SERVICE =
            "https://elevation.nationalmap.gov/arcgis/rest/services/"
            + "3DEPElevation/ImageServer";


    /*
     * First EarthBound terrain test area.
     *
     * This box covers the Guemes Island area.
     * It is deliberately a test tile before we connect
     * terrain downloading directly to world generation.
     */
    private static final double GUEMES_WEST = -122.70;
    private static final double GUEMES_SOUTH = 48.47;
    private static final double GUEMES_EAST = -122.55;
    private static final double GUEMES_NORTH = 48.60;

    private static final int GUEMES_TILE_WIDTH = 512;
    private static final int GUEMES_TILE_HEIGHT = 512;


    private EarthTerrainLoader() {
        // Utility class
    }


    /*
     * Loads one elevation point.
     *
     * This is still useful for /earth locate.
     * World generation will eventually use terrain tiles
     * instead of making one web request per block.
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


    /*
     * Converts real elevation into EarthBound's
     * progressive Minecraft vertical scale.
     */
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
     * Tests basic access to the USGS 3DEP service.
     */
    public static boolean test3DEPConnection() {

        HttpURLConnection connection = null;

        try {

            String address =
                    USGS_3DEP_SERVICE
                    + "?f=pjson";

            URL url =
                    URI.create(address).toURL();

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode =
                    connection.getResponseCode();

            if (responseCode != 200) {

                System.out.println(
                        "[EarthBound] USGS HTTP response: "
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

                if (response.length() > 0) {

                    System.out.println(
                            "[EarthBound] USGS 3DEP connection successful."
                    );

                    return true;
                }

                System.out.println(
                        "[EarthBound] USGS returned empty data."
                );

                return false;
            }

        } catch (Exception exception) {

            System.out.println(
                    "[EarthBound] Could not connect to USGS 3DEP."
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
     * Creates a USGS terrain tile URL.
     *
     * The result is a floating-point TIFF elevation raster.
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


    /*
     * Creates the URL for our first Guemes Island
     * elevation tile.
     */
    public static String createGuemesTerrainTileURL() {

        return createTerrainTileURL(
                GUEMES_WEST,
                GUEMES_SOUTH,
                GUEMES_EAST,
                GUEMES_NORTH,
                GUEMES_TILE_WIDTH,
                GUEMES_TILE_HEIGHT
        );
    }


    /*
     * Downloads the first Guemes Island terrain tile
     * as a test.
     *
     * For now we only verify that real raster bytes
     * arrive from USGS. We do NOT generate Minecraft
     * terrain from the TIFF yet.
     */
    public static boolean testGuemesTerrainTile() {

        HttpURLConnection connection = null;

        try {

            String address =
                    createGuemesTerrainTileURL();

            System.out.println(
                    "[EarthBound] Requesting Guemes Island terrain tile..."
            );

            URL url =
                    URI.create(address).toURL();

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);

            int responseCode =
                    connection.getResponseCode();

            if (responseCode != 200) {

                System.out.println(
                        "[EarthBound] Guemes terrain request failed. HTTP "
                                + responseCode
                );

                return false;
            }

            long totalBytes = 0;

            try (InputStream input =
                         connection.getInputStream()) {

                byte[] buffer =
                        new byte[8192];

                int bytesRead;

                while ((bytesRead =
                                input.read(buffer)) != -1) {

                    totalBytes += bytesRead;
                }
            }

            if (totalBytes > 0) {

                System.out.println(
                        "[EarthBound] Guemes terrain tile downloaded successfully."
                );

                System.out.println(
                        "[EarthBound] Guemes terrain tile size: "
                                + totalBytes
                                + " bytes"
                );

                return true;
            }

            System.out.println(
                    "[EarthBound] Guemes terrain tile was empty."
            );

            return false;

        } catch (Exception exception) {

            System.out.println(
                    "[EarthBound] Could not download Guemes terrain tile."
            );

            exception.printStackTrace();

            return false;

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
