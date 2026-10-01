package com.earthbound.water;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.imageio.ImageIO;

/**
 * ============================================================
 * EARTHBOUND WATER DATA
 *
 * Loads the real Guemes Island water mask.
 *
 * Data source:
 * U.S. Census Bureau TIGERweb
 * Hydro / Areal Hydrography
 *
 * TEST VERSION:
 * - Uses the original 512 x 512 water mask
 * - NO manual ferry shoreline correction
 * - NO GPS shoreline interpolation
 * - NO rectangular shoreline override
 *
 * This lets us test the older EarthBound water system cleanly.
 * ============================================================
 */
public class EarthWaterData {

    public static final int SEA_LEVEL = 63;

    /*
     * ========================================================
     * GUEMES GEOGRAPHIC BOUNDS
     * ========================================================
     */

    private static final double WEST = -122.70;
    private static final double SOUTH = 48.47;
    private static final double EAST = -122.55;
    private static final double NORTH = 48.60;

    /*
     * ========================================================
     * WATER MASK SIZE
     *
     * ORIGINAL TEST RESOLUTION
     * ========================================================
     */

    private static final int MASK_WIDTH = 512;
    private static final int MASK_HEIGHT = 512;

    /*
     * ========================================================
     * TIGERWEB HYDROGRAPHY SERVICE
     *
     * Layer 1 = Areal Hydrography
     * ========================================================
     */

    private static final String HYDRO_SERVICE =
            "https://tigerweb.geo.census.gov/"
                    + "arcgis/rest/services/"
                    + "TIGERweb/Hydro/MapServer/export";

    /*
     * ========================================================
     * WATER MASK
     * ========================================================
     */

    private static boolean[][] waterMask;

    private static boolean loaded = false;

    /*
     * ========================================================
     * STANDARD LOADER
     * ========================================================
     */

    public static void load() {

        loadGuemesWaterMask();

    }

    /*
     * ========================================================
     * LOAD GUEMES WATER MASK
     * ========================================================
     */

    public static boolean loadGuemesWaterMask() {

        System.out.println(
                "[EarthBound] Loading original 512x512 Guemes water mask..."
        );

        try {

            File imageFile =
                    downloadWaterMask();

            decodeWaterMask(
                    imageFile
            );

            loaded = true;

            System.out.println(
                    "[EarthBound] Guemes water mask loaded!"
            );

            System.out.println(
                    "[EarthBound] Water mask: "
                            + MASK_WIDTH
                            + "x"
                            + MASK_HEIGHT
            );

            int waterPixels =
                    countWaterPixels();

            System.out.println(
                    "[EarthBound] Water pixels: "
                            + waterPixels
                            + " / "
                            + (MASK_WIDTH * MASK_HEIGHT)
            );

            System.out.println(
                    "[EarthBound] ORIGINAL 512 WATER TEST ACTIVE"
            );

            System.out.println(
                    "[EarthBound] Ferry shoreline correction: DISABLED"
            );

            System.out.println(
                    "[EarthBound] GPS shoreline correction: DISABLED"
            );

            if (imageFile != null) {

                if (!imageFile.delete()) {

                    imageFile.deleteOnExit();

                }

            }

            return true;

        } catch (Exception exception) {

            loaded = false;

            System.err.println(
                    "[EarthBound] Failed to load Guemes water mask!"
            );

            exception.printStackTrace();

            return false;

        }

    }

    /*
     * ========================================================
     * DOWNLOAD TIGERWEB WATER IMAGE
     * ========================================================
     */

    private static File downloadWaterMask()
            throws Exception {

        System.out.println(
                "[EarthBound] Downloading TIGERweb Guemes hydrography..."
        );

        /*
         * Request:
         *
         * Geographic bounds:
         * WEST, SOUTH, EAST, NORTH
         *
         * Coordinate system:
         * WGS84 / EPSG:4326
         *
         * Layer:
         * 1 = Areal Hydrography
         *
         * Transparent background:
         * land / no water polygon
         *
         * Visible pixels:
         * mapped water polygon
         */

        String request =
                HYDRO_SERVICE
                        + "?bbox="
                        + WEST
                        + ","
                        + SOUTH
                        + ","
                        + EAST
                        + ","
                        + NORTH

                        + "&bboxSR=4326"

                        + "&imageSR=4326"

                        + "&size="
                        + MASK_WIDTH
                        + ","
                        + MASK_HEIGHT

                        + "&format=png32"

                        + "&transparent=true"

                        + "&layers=show:1"

                        + "&f=image";

        URL url =
                new URL(
                        request
                );

        HttpURLConnection connection =
                (HttpURLConnection)
                        url.openConnection();

        connection.setConnectTimeout(
                30000
        );

        connection.setReadTimeout(
                60000
        );

        connection.setRequestProperty(
                "User-Agent",
                "EarthBound-Minecraft-Server/0.1"
        );

        connection.connect();

        int responseCode =
                connection.getResponseCode();

        if (
                responseCode < 200
                        ||
                responseCode >= 300
        ) {

            connection.disconnect();

            throw new IllegalStateException(
                    "TIGERweb returned HTTP "
                            + responseCode
            );

        }

        File tempFile =
                File.createTempFile(
                        "earthbound-guemes-water-",
                        ".png"
                );

        long totalBytes = 0;

        try (
                InputStream input =
                        connection.getInputStream();

                FileOutputStream output =
                        new FileOutputStream(
                                tempFile
                        )
        ) {

            byte[] buffer =
                    new byte[8192];

            int read;

            while (
                    (read =
                            input.read(
                                    buffer
                            ))
                            != -1
            ) {

                output.write(
                        buffer,
                        0,
                        read
                );

                totalBytes += read;

            }

        } finally {

            connection.disconnect();

        }

        System.out.println(
                "[EarthBound] Water mask downloaded: "
                        + totalBytes
                        + " bytes"
        );

        return tempFile;

    }

    /*
     * ========================================================
     * DECODE WATER MASK
     * ========================================================
     */

    private static void decodeWaterMask(
            File file
    )
            throws Exception {

        System.out.println(
                "[EarthBound] Decoding Guemes water mask..."
        );

        BufferedImage image =
                ImageIO.read(
                        file
                );

        if (image == null) {

            throw new IllegalStateException(
                    "Java could not decode the TIGERweb water image."
            );

        }

        if (
                image.getWidth() != MASK_WIDTH
                        ||
                image.getHeight() != MASK_HEIGHT
        ) {

            throw new IllegalStateException(
                    "Unexpected water mask size: "
                            + image.getWidth()
                            + "x"
                            + image.getHeight()
            );

        }

        waterMask =
                new boolean[
                        MASK_WIDTH
                ][
                        MASK_HEIGHT
                ];

        for (
                int x = 0;
                x < MASK_WIDTH;
                x++
        ) {

            for (
                    int z = 0;
                    z < MASK_HEIGHT;
                    z++
            ) {

                int argb =
                        image.getRGB(
                                x,
                                z
                        );

                /*
                 * PNG32 alpha channel.
                 *
                 * Alpha 0:
                 * transparent / no mapped water polygon
                 *
                 * Visible alpha:
                 * mapped hydrography polygon
                 */

                int alpha =
                        (argb >>> 24)
                                & 0xFF;

                waterMask[x][z] =
                        alpha > 20;

            }

        }

        System.out.println(
                "[EarthBound] Water mask decoded: "
                        + image.getWidth()
                        + "x"
                        + image.getHeight()
        );

    }

    /*
     * ========================================================
     * DATA STATUS
     * ========================================================
     */

    public static boolean isLoaded() {

        return loaded
                &&
                waterMask != null;

    }

    /*
     * ========================================================
     * REAL-WORLD WATER LOOKUP
     * ========================================================
     */

    public static boolean isWater(
            double latitude,
            double longitude
    ) {

        if (!isLoaded()) {

            return false;

        }

        /*
         * Outside the currently loaded Guemes geographic tile.
         *
         * This test version only knows the Guemes tile.
         */

        if (
                longitude < WEST
                        ||
                longitude > EAST
                        ||
                latitude < SOUTH
                        ||
                latitude > NORTH
        ) {

            return false;

        }

        /*
         * ====================================================
         * LONGITUDE -> IMAGE X
         *
         * WEST = left side of image
         * EAST = right side of image
         * ====================================================
         */

        double normalizedX =
                (longitude - WEST)
                        /
                        (EAST - WEST);

        int maskX =
                (int) Math.round(
                        normalizedX
                                *
                                (MASK_WIDTH - 1)
                );

        /*
         * ====================================================
         * LATITUDE -> IMAGE Z
         *
         * NORTH = top of image
         * SOUTH = bottom of image
         * ====================================================
         */

        double normalizedZ =
                (NORTH - latitude)
                        /
                        (NORTH - SOUTH);

        int maskZ =
                (int) Math.round(
                        normalizedZ
                                *
                                (MASK_HEIGHT - 1)
                );

        maskX =
                clamp(
                        maskX,
                        0,
                        MASK_WIDTH - 1
                );

        maskZ =
                clamp(
                        maskZ,
                        0,
                        MASK_HEIGHT - 1
                );

        /*
         * IMPORTANT:
         *
         * There is deliberately NO ferry correction here.
         *
         * We are returning the original TIGERweb mask directly.
         */

        return waterMask[
                maskX
        ][
                maskZ
        ];

    }

    /*
     * ========================================================
     * DEBUG / VALIDATION
     * ========================================================
     */

    private static int countWaterPixels() {

        if (waterMask == null) {

            return 0;

        }

        int count = 0;

        for (
                int x = 0;
                x < MASK_WIDTH;
                x++
        ) {

            for (
                    int z = 0;
                    z < MASK_HEIGHT;
                    z++
            ) {

                if (
                        waterMask[x][z]
                ) {

                    count++;

                }

            }

        }

        return count;

    }

    /*
     * ========================================================
     * CLAMP
     * ========================================================
     */

    private static int clamp(
            int value,
            int minimum,
            int maximum
    ) {

        if (
                value < minimum
        ) {

            return minimum;

        }

        if (
                value > maximum
        ) {

            return maximum;

        }

        return value;

    }

}
