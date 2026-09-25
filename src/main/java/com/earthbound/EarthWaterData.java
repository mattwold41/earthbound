package com.earthbound;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;

import java.io.InputStream;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class EarthWaterData {

    /*
     * Minecraft sea level.
     */
    public static final int SEA_LEVEL = 63;


    /*
     * Same Guemes test bounds used by
     * EarthTerrainLoader.
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
     */
    private static final int MASK_WIDTH =
            512;

    private static final int MASK_HEIGHT =
            512;


    /*
     * Census TIGERweb Hydro MapServer.
     *
     * Layer 1 = Areal Hydrography.
     */
    private static final String HYDRO_SERVICE =
            "https://tigerweb.geo.census.gov/"
                    + "arcgis/rest/services/"
                    + "TIGERweb/Hydro/MapServer";


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
     * Downloads ONE rendered hydrography image
     * covering the Guemes test area.
     *
     * No per-block Internet requests.
     */
    public static boolean loadGuemesWaterMask() {

        HttpURLConnection connection =
                null;

        try {

            System.out.println(
                    "[EarthBound] Downloading Guemes water mask..."
            );


            String address =
                    HYDRO_SERVICE
                            + "/export"
                            + "?bbox="
                            + GUEMES_WEST
                            + ","
                            + GUEMES_SOUTH
                            + ","
                            + GUEMES_EAST
                            + ","
                            + GUEMES_NORTH
                            + "&bboxSR=4326"
                            + "&imageSR=4326"
                            + "&size="
                            + MASK_WIDTH
                            + ","
                            + MASK_HEIGHT

                            /*
                             * Only draw layer 1:
                             * Areal Hydrography.
                             */
                            + "&layers=show:1"

                            /*
                             * Transparent background.
                             * Water polygons remain visible.
                             */
                            + "&transparent=true"
                            + "&format=png32"
                            + "&f=image";


            URL url =
                    URI.create(address)
                            .toURL();


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
                        "[EarthBound] Water mask request failed. HTTP "
                                + responseCode
                );

                return false;
            }


            BufferedImage image;


            try (InputStream input =
                         connection.getInputStream()) {

                image =
                        ImageIO.read(input);
            }


            if (image == null) {

                System.out.println(
                        "[EarthBound] Water mask PNG could not be decoded."
                );

                return false;
            }


            int width =
                    image.getWidth();

            int height =
                    image.getHeight();


            System.out.println(
                    "[EarthBound] Water mask image: "
                            + width
                            + " x "
                            + height
            );


            boolean[][] newMask =
                    new boolean[height][width];


            int waterCells =
                    0;


            for (int y = 0;
                 y < height;
                 y++) {

                for (int x = 0;
                     x < width;
                     x++) {


                    int argb =
                            image.getRGB(
                                    x,
                                    y
                            );


                    /*
                     * Transparent pixels have
                     * alpha = 0.
                     *
                     * Hydrography polygons have
                     * visible pixels.
                     */
                    int alpha =
                            (argb >>> 24)
                                    & 0xFF;


                    boolean water =
                            alpha > 0;


                    newMask[y][x] =
                            water;


                    if (water) {
                        waterCells++;
                    }
                }
            }


            waterMask =
                    newMask;


            System.out.println(
                    "[EarthBound] Guemes water mask loaded!"
            );


            System.out.println(
                    "[EarthBound] Water pixels: "
                            + waterCells
                            + " / "
                            + (width * height)
            );


            return true;


        } catch (Exception exception) {

            System.out.println(
                    "[EarthBound] Failed to load Guemes water mask."
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
     * Returns true after the mask
     * has successfully loaded.
     */
    public static boolean isLoaded() {

        return waterMask != null;
    }


    /*
     * Fast local lookup.
     *
     * Minecraft can call this thousands
     * of times without making any
     * Internet requests.
     */
    public static boolean isWater(
            double latitude,
            double longitude) {


        boolean[][] mask =
                waterMask;


        if (mask == null) {

            return false;
        }


        /*
         * Outside our current Guemes
         * test area.
         */
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


        /*
         * Image row zero is north.
         */
        double yPercent =
                (GUEMES_NORTH - latitude)
                        / (GUEMES_NORTH
                        - GUEMES_SOUTH);


        int width =
                mask[0].length;

        int height =
                mask.length;


        int x =
                (int) Math.round(
                        xPercent
                                * (width - 1)
                );


        int y =
                (int) Math.round(
                        yPercent
                                * (height - 1)
                );


        x =
                Math.max(
                        0,
                        Math.min(
                                width - 1,
                                x
                        )
                );


        y =
                Math.max(
                        0,
                        Math.min(
                                height - 1,
                                y
                        )
                );


        return mask[y][x];
    }
}
