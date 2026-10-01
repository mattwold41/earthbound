package com.earthbound.water;

import com.earthbound.EarthCoordinates;

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
 * Downloads and stores the real Guemes Island hydrography mask.
 *
 * Data source:
 * U.S. Census Bureau TIGERweb
 * Hydro / Areal Hydrography
 *
 * The TIGERweb mask remains the authoritative water source.
 *
 * A small local correction is applied around the south Guemes
 * ferry area where the hydrography shoreline currently reaches
 * too far inland compared with the road/ferry layout.
 *
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
     * ========================================================
     */

    private static final int MASK_WIDTH = 2048;
    private static final int MASK_HEIGHT = 2048;

    /*
     * ========================================================
     * SOUTH GUEMES FERRY TEST CORRECTION
     *
     * Known Minecraft reference:
     *
     * S Shore Dr / Guemes Island Rd:
     * approximately X=-665, Z=-539
     *
     * Current first water:
     * approximately X=-665, Z=-515
     *
     * For the first controlled test we extend dry land
     * another 25 blocks south.
     *
     * That moves the protected test edge from roughly
     * Z=-515 to Z=-490.
     *
     * This is intentionally LOCAL and TEMPORARY while we
     * compare the generated shoreline against the real map.
     *
     * It does NOT shift the whole island or modify roads.
     * ========================================================
     */

    private static final int FERRY_CORRECTION_WEST_X = -730;
    private static final int FERRY_CORRECTION_EAST_X = -600;

    private static final int FERRY_CORRECTION_NORTH_Z = -550;
    private static final int FERRY_CORRECTION_SOUTH_Z = -490;

    /*
     * ========================================================
     * TIGERWEB HYDROGRAPHY SERVICE
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
     * LOADER
     * ========================================================
     */

    public static void load() {

        loadGuemesWaterMask();

    }

    /*
     * ========================================================
     * LOAD REAL GUEMES WATER MASK
     * ========================================================
     */

    public static boolean loadGuemesWaterMask() {

        System.out.println(
                "[EarthBound] Loading real Guemes water mask..."
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
                    "[EarthBound] South Guemes ferry shoreline "
                            + "test correction enabled."
            );

            System.out.println(
                    "[EarthBound] Ferry correction X="
                            + FERRY_CORRECTION_WEST_X
                            + " to "
                            + FERRY_CORRECTION_EAST_X
                            + ", Z="
                            + FERRY_CORRECTION_NORTH_Z
                            + " to "
                            + FERRY_CORRECTION_SOUTH_Z
            );

            if (imageFile != null) {

                imageFile.delete();

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
                image.getWidth()
                        != MASK_WIDTH
                        ||
                image.getHeight()
                        != MASK_HEIGHT
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
     * CHECK WHETHER DATA IS READY
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
         * Outside the currently loaded Guemes tile.
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
         * LOCAL SOUTH GUEMES FERRY CORRECTION
         *
         * Convert the requested real-world position back into
         * the same corrected Minecraft coordinate system used
         * by terrain, roads and future structures.
         *
         * Inside this small test zone, TIGER water is overridden
         * to land.
         *
         * Everything outside the zone continues using the
         * untouched TIGERweb mask.
         * ====================================================
         */

        int minecraftX =
                EarthCoordinates.longitudeToMinecraftX(
                        longitude
                );

        int minecraftZ =
                EarthCoordinates.latitudeToMinecraftZ(
                        latitude
                );

        if (
                minecraftX >= FERRY_CORRECTION_WEST_X
                        &&
                minecraftX <= FERRY_CORRECTION_EAST_X
                        &&
                minecraftZ >= FERRY_CORRECTION_NORTH_Z
                        &&
                minecraftZ <= FERRY_CORRECTION_SOUTH_Z
        ) {

            return false;

        }

        /*
         * Longitude:
         *
         * WEST -> left edge
         * EAST -> right edge
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
         * Latitude:
         *
         * NORTH -> top of image
         * SOUTH -> bottom of image
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
