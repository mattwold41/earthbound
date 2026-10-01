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
 * Downloads and stores the real Guemes Island hydrography mask.
 *
 * Primary data source:
 * U.S. Census Bureau TIGERweb
 * Hydro / Areal Hydrography
 *
 * A small measured shoreline correction is applied around the
 * South Guemes ferry landing.
 *
 * The correction is based on real shoreline GPS measurements
 * taken from satellite imagery and verified against the
 * generated Minecraft shoreline.
 *
 * IMPORTANT:
 * This correction does NOT move roads.
 * It does NOT shift the entire Guemes coastline.
 * It only corrects the measured South Guemes ferry section.
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
     * TIGERWEB HYDROGRAPHY SERVICE
     * ========================================================
     */

    private static final String HYDRO_SERVICE =
            "https://tigerweb.geo.census.gov/"
                    + "arcgis/rest/services/"
                    + "TIGERweb/Hydro/MapServer/export";

    /*
     * ========================================================
     * SOUTH GUEMES MEASURED SHORELINE CONTROL POINTS
     * ========================================================
     *
     * These are ordered WEST -> EAST.
     *
     * West shoreline:
     * 48.528440, -122.625163
     *
     * Ferry shoreline:
     * 48.528507, -122.624792
     *
     * East shoreline:
     * 48.528460, -122.624121
     *
     * Minecraft measurements showed the untouched TIGER mask
     * placing first land roughly 51-57 blocks too far north
     * through this specific section.
     *
     * We interpolate between the measured real shoreline
     * points instead of creating a rectangular override.
     * ========================================================
     */

    private static final double SOUTH_SHORE_WEST_LON =
            -122.625163;

    private static final double SOUTH_SHORE_WEST_LAT =
            48.528440;

    private static final double SOUTH_SHORE_CENTER_LON =
            -122.624792;

    private static final double SOUTH_SHORE_CENTER_LAT =
            48.528507;

    private static final double SOUTH_SHORE_EAST_LON =
            -122.624121;

    private static final double SOUTH_SHORE_EAST_LAT =
            48.528460;

    /*
     * Small tolerance north of the measured shoreline.
     *
     * The actual land correction extends northward from the
     * measured shoreline until it naturally reconnects with
     * land already present in the TIGER mask.
     *
     * This latitude is deliberately local to the ferry area.
     */

    private static final double SOUTH_SHORE_CORRECTION_NORTH =
            48.529600;

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
                            + "correction ENABLED."
            );

            System.out.println(
                    "[EarthBound] Shoreline correction uses "
                            + "3 measured GPS control points."
            );

            System.out.println(
                    "[EarthBound] Roads remain unchanged."
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
         * SOUTH GUEMES FERRY SHORELINE CORRECTION
         * ====================================================
         *
         * If the point falls inside the measured ferry
         * correction corridor and is north of the interpolated
         * real shoreline, it is land.
         *
         * This is intentionally evaluated BEFORE the TIGER
         * water mask.
         *
         * The southern boundary is not rectangular. It follows
         * the three measured shoreline GPS control points.
         * ====================================================
         */

        if (
                isSouthGuemesCorrectedLand(
                        latitude,
                        longitude
                )
        ) {

            return false;

        }

        /*
         * ====================================================
         * RAW TIGER WATER LOOKUP
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
     * SOUTH GUEMES CORRECTED LAND TEST
     * ========================================================
     */

    private static boolean isSouthGuemesCorrectedLand(
            double latitude,
            double longitude
    ) {

        /*
         * Stay strictly inside the measured west/east corridor.
         *
         * We are deliberately NOT extrapolating beyond the
         * shoreline locations that were actually measured.
         */

        if (
                longitude < SOUTH_SHORE_WEST_LON
                        ||
                        longitude > SOUTH_SHORE_EAST_LON
        ) {

            return false;

        }

        /*
         * Keep correction local to the southern ferry area.
         */

        if (
                latitude > SOUTH_SHORE_CORRECTION_NORTH
        ) {

            return false;

        }

        double shorelineLatitude =
                getMeasuredSouthShoreLatitude(
                        longitude
                );

        /*
         * North of the measured shoreline = land.
         * South of the measured shoreline = water.
         */

        return latitude >= shorelineLatitude;

    }

    /*
     * ========================================================
     * INTERPOLATE MEASURED SOUTH SHORELINE
     * ========================================================
     *
     * WEST -> CENTER uses the first two measurements.
     * CENTER -> EAST uses the second and third measurements.
     *
     * This creates a shaped shoreline rather than a rectangle.
     * ========================================================
     */

    private static double getMeasuredSouthShoreLatitude(
            double longitude
    ) {

        if (
                longitude <= SOUTH_SHORE_CENTER_LON
        ) {

            return interpolate(
                    SOUTH_SHORE_WEST_LON,
                    SOUTH_SHORE_WEST_LAT,
                    SOUTH_SHORE_CENTER_LON,
                    SOUTH_SHORE_CENTER_LAT,
                    longitude
            );

        }

        return interpolate(
                SOUTH_SHORE_CENTER_LON,
                SOUTH_SHORE_CENTER_LAT,
                SOUTH_SHORE_EAST_LON,
                SOUTH_SHORE_EAST_LAT,
                longitude
        );

    }

    /*
     * ========================================================
     * LINEAR INTERPOLATION
     * ========================================================
     */

    private static double interpolate(
            double x1,
            double y1,
            double x2,
            double y2,
            double x
    ) {

        if (
                Math.abs(
                        x2 - x1
                )
                        < 0.0000000001
        ) {

            return y1;

        }

        double progress =
                (x - x1)
                        /
                        (x2 - x1);

        return y1
                +
                (
                        (y2 - y1)
                                *
                                progress
                );

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
