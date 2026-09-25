package com.earthbound;

public class EarthWaterData {

    /*
     * Minecraft sea level.
     */
    public static final int SEA_LEVEL = 63;


    /*
     * Guemes test-area bounds.
     *
     * These match our USGS elevation raster.
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
     * Water-mask resolution.
     *
     * This is deliberately much smaller than
     * Minecraft's block grid.
     *
     * We query the geographic water service once
     * for each mask cell during startup.
     *
     * Minecraft terrain generation itself makes
     * ZERO Internet requests.
     */
    private static final int MASK_WIDTH =
            128;

    private static final int MASK_HEIGHT =
            128;


    /*
     * true  = water
     * false = land
     */
    private static volatile boolean[][] waterMask =
            null;


    private EarthWaterData() {
        // Utility class
    }


    /*
     * Loads the Guemes water mask.
     *
     * IMPORTANT:
     * This is only called during startup,
     * never once per Minecraft block.
     */
    public static boolean loadGuemesWaterMask() {

        System.out.println(
                "[EarthBound] Loading Guemes water mask..."
        );


        boolean[][] newMask =
                new boolean[MASK_HEIGHT][MASK_WIDTH];


        int waterCells = 0;


        try {

            for (int y = 0;
                 y < MASK_HEIGHT;
                 y++) {

                double yPercent =
                        (double) y
                                / (MASK_HEIGHT - 1);


                double latitude =
                        GUEMES_NORTH
                                - yPercent
                                * (GUEMES_NORTH
                                - GUEMES_SOUTH);


                for (int x = 0;
                     x < MASK_WIDTH;
                     x++) {

                    double xPercent =
                            (double) x
                                    / (MASK_WIDTH - 1);


                    double longitude =
                            GUEMES_WEST
                                    + xPercent
                                    * (GUEMES_EAST
                                    - GUEMES_WEST);


                    boolean water =
                            queryWater(
                                    latitude,
                                    longitude
                            );


                    newMask[y][x] =
                            water;


                    if (water) {
                        waterCells++;
                    }
                }


                /*
                 * Progress message every 16 rows.
                 */
                if (y % 16 == 0) {

                    System.out.println(
                            "[EarthBound] Water mask progress: "
                                    + y
                                    + "/"
                                    + MASK_HEIGHT
                    );
                }
            }


            waterMask =
                    newMask;


            System.out.println(
                    "[EarthBound] Guemes water mask ready."
            );

            System.out.println(
                    "[EarthBound] Water cells: "
                            + waterCells
                            + " / "
                            + (MASK_WIDTH * MASK_HEIGHT)
            );


            return true;


        } catch (Exception exception) {

            System.out.println(
                    "[EarthBound] Failed to build Guemes water mask."
            );

            exception.printStackTrace();

            return false;
        }
    }


    /*
     * Queries Census TIGERweb hydrography.
     *
     * This is ONLY used while building
     * the startup water mask.
     */
    public static boolean queryWater(
            double latitude,
            double longitude) {

        java.net.HttpURLConnection connection =
                null;

        try {

            String service =
                    "https://tigerweb.geo.census.gov/"
                            + "arcgis/rest/services/"
                            + "TIGERweb/Hydro/MapServer/"
                            + "1/query";


            String address =
                    service
                            + "?geometry="
                            + longitude
                            + ","
                            + latitude
                            + "&geometryType=esriGeometryPoint"
                            + "&inSR=4326"
                            + "&spatialRel=esriSpatialRelIntersects"
                            + "&returnGeometry=false"
                            + "&returnCountOnly=true"
                            + "&f=json";


            java.net.URL url =
                    java.net.URI
                            .create(address)
                            .toURL();


            connection =
                    (java.net.HttpURLConnection)
                            url.openConnection();


            connection.setRequestMethod("GET");

            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);


            int responseCode =
                    connection.getResponseCode();


            if (responseCode != 200) {

                return false;
            }


            StringBuilder response =
                    new StringBuilder();


            try (java.io.BufferedReader reader =
                         new java.io.BufferedReader(
                                 new java.io.InputStreamReader(
                                         connection.getInputStream()
                                 )
                         )) {

                String line;

                while ((line =
                                reader.readLine()) != null) {

                    response.append(line);
                }
            }


            String json =
                    response.toString();


            int marker =
                    json.indexOf("\"count\":");


            if (marker < 0) {
                return false;
            }


            int start =
                    marker
                            + "\"count\":".length();


            int end =
                    start;


            while (end < json.length()
                    && Character.isDigit(
                            json.charAt(end))) {

                end++;
            }


            if (end <= start) {
                return false;
            }


            int count =
                    Integer.parseInt(
                            json.substring(
                                    start,
                                    end
                            )
                    );


            return count > 0;


        } catch (Exception exception) {

            return false;


        } finally {

            if (connection != null) {

                connection.disconnect();
            }
        }
    }


    /*
     * Returns whether the water mask
     * has finished loading.
     */
    public static boolean isLoaded() {

        return waterMask != null;
    }


    /*
     * Fast lookup used by Minecraft terrain generation.
     *
     * NO Internet request happens here.
     */
    public static boolean isWater(
            double latitude,
            double longitude) {

        boolean[][] mask =
                waterMask;


        if (mask == null) {
            return false;
        }


        if (latitude < GUEMES_SOUTH
                || latitude > GUEMES_NORTH
                || longitude < GUEMES_WEST
                || longitude > GUEMES_EAST) {

            return false;
        }


        double xPercent =
                (longitude - GUEMES_WEST)
                        / (GUEMES_EAST
                        - GUEMES_WEST);


        double yPercent =
                (GUEMES_NORTH - latitude)
                        / (GUEMES_NORTH
                        - GUEMES_SOUTH);


        int x =
                (int) Math.round(
                        xPercent
                                * (MASK_WIDTH - 1)
                );


        int y =
                (int) Math.round(
                        yPercent
                                * (MASK_HEIGHT - 1)
                );


        x =
                Math.max(
                        0,
                        Math.min(
                                MASK_WIDTH - 1,
                                x
                        )
                );


        y =
                Math.max(
                        0,
                        Math.min(
                                MASK_HEIGHT - 1,
                                y
                        )
                );


        return mask[y][x];
    }
}
