package com.earthbound;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;

public class EarthTerrainDownloader {

    /*
     * ============================================================
     * EARTHBOUND USGS TERRAIN DOWNLOADER
     * ============================================================
     *
     * Downloads and stores real USGS 3DEP elevation data for
     * Guemes Island.
     *
     * IMPORTANT:
     *
     * The downloaded raster is 512 x 512.
     *
     * Minecraft blocks do NOT simply snap to the nearest raster
     * pixel anymore.
     *
     * Elevation is calculated using bilinear interpolation between
     * the four surrounding USGS raster pixels.
     *
     * This prevents large square elevation terraces caused by
     * nearest-pixel sampling.
     * ============================================================
     */


    /*
     * ============================================================
     * USGS SERVICE
     * ============================================================
     */

    public static final String USGS_3DEP_SERVICE =
            "https://elevation.nationalmap.gov/"
                    + "arcgis/rest/services/"
                    + "3DEPElevation/ImageServer";


    /*
     * ============================================================
     * GUEMES ISLAND DATA BOUNDS
     * ============================================================
     */

    public static final double WEST =
            -122.70;

    public static final double SOUTH =
            48.47;

    public static final double EAST =
            -122.55;

    public static final double NORTH =
            48.60;


    /*
     * ============================================================
     * RASTER SIZE
     * ============================================================
     */

    public static final int TILE_WIDTH =
            512;

    public static final int TILE_HEIGHT =
            512;


    /*
     * ============================================================
     * ELEVATION DATA
     * ============================================================
     */

    private static double[][] elevationData;

    private static boolean loaded =
            false;


    private EarthTerrainDownloader() {
    }


    /*
     * ============================================================
     * LOAD GUEMES ISLAND
     * ============================================================
     */

    public static void loadGuemesIsland() {

        System.out.println(
                "=== Loading real USGS Guemes elevation ==="
        );


        try {

            File terrainFolder =
                    new File(
                            "plugins/EarthBound/terrain"
                    );


            if (!terrainFolder.exists()) {

                terrainFolder.mkdirs();

            }


            File elevationFile =
                    new File(
                            terrainFolder,
                            "guemes-elevation.tif"
                    );


            /*
             * --------------------------------------------------------
             * DOWNLOAD IF NECESSARY
             * --------------------------------------------------------
             */

            if (!elevationFile.exists()) {

                downloadElevationTile(
                        elevationFile
                );

            } else {

                System.out.println(
                        "Using cached Guemes elevation tile."
                );

            }


            /*
             * --------------------------------------------------------
             * READ RASTER
             * --------------------------------------------------------
             */

            readElevationTile(
                    elevationFile
            );


            loaded =
                    true;


            System.out.println(
                    "=== Guemes USGS elevation loaded ==="
            );


            System.out.println(
                    "Raster size: "
                            + elevationData[0].length
                            + "x"
                            + elevationData.length
            );


            System.out.println(
                    "Center elevation sample: "
                            + elevationData[
                            elevationData.length / 2
                            ][
                            elevationData[0].length / 2
                            ]
                            + " meters"
            );


            System.out.println(
                    "[EarthBound] Bilinear USGS terrain interpolation enabled."
            );


        } catch (Exception exception) {

            loaded =
                    false;


            System.err.println(
                    "Failed to load Guemes USGS elevation."
            );


            exception.printStackTrace();

        }

    }


    /*
     * ============================================================
     * DOWNLOAD USGS ELEVATION TILE
     * ============================================================
     */

    private static void downloadElevationTile(
            File destination
    ) throws Exception {


        String url =
                USGS_3DEP_SERVICE
                        + "/exportImage"
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
                        + TILE_WIDTH
                        + ","
                        + TILE_HEIGHT
                        + "&format=tiff"
                        + "&pixelType=F32"
                        + "&interpolation=RSP_BilinearInterpolation"
                        + "&f=image";


        System.out.println(
                "Downloading Guemes elevation from USGS..."
        );


        HttpURLConnection connection =
                (HttpURLConnection)
                        URI.create(
                                        url
                                )
                                .toURL()
                                .openConnection();


        connection.setRequestMethod(
                "GET"
        );


        connection.setConnectTimeout(
                30000
        );


        connection.setReadTimeout(
                60000
        );


        connection.setRequestProperty(
                "User-Agent",
                "EarthBound-Minecraft/0.1.0"
        );


        int responseCode =
                connection.getResponseCode();


        if (responseCode != 200) {

            throw new IllegalStateException(
                    "USGS returned HTTP "
                            + responseCode
            );

        }


        try (
                InputStream input =
                        new BufferedInputStream(
                                connection.getInputStream()
                        );

                FileOutputStream output =
                        new FileOutputStream(
                                destination
                        )
        ) {

            input.transferTo(
                    output
            );

        } finally {

            connection.disconnect();

        }


        System.out.println(
                "USGS elevation download complete."
        );


        System.out.println(
                "Downloaded "
                        + Files.size(
                        destination.toPath()
                )
                        + " bytes."
        );

    }


    /*
     * ============================================================
     * READ USGS TIFF
     * ============================================================
     */

    private static void readElevationTile(
            File elevationFile
    ) throws Exception {


        BufferedImage image =
                ImageIO.read(
                        elevationFile
                );


        if (image == null) {

            throw new IllegalStateException(
                    "TIFF elevation image could not be decoded."
            );

        }


        Raster raster =
                image.getRaster();


        int width =
                raster.getWidth();

        int height =
                raster.getHeight();


        elevationData =
                new double[
                        height
                        ][
                        width
                        ];


        for (int y = 0;
             y < height;
             y++) {


            for (int x = 0;
                 x < width;
                 x++) {


                elevationData[y][x] =
                        raster.getSampleDouble(
                                x,
                                y,
                                0
                        );

            }
        }

    }


    /*
     * ============================================================
     * TERRAIN STATUS
     * ============================================================
     */

    public static boolean isLoaded() {

        return loaded
                && elevationData != null
                && elevationData.length > 0
                && elevationData[0].length > 0;

    }


    /*
     * ============================================================
     * GET REAL USGS ELEVATION
     * ============================================================
     *
     * OLD SYSTEM:
     *
     * Minecraft coordinate
     *       |
     *       v
     * nearest raster pixel
     *       |
     *       v
     * elevation
     *
     *
     * NEW SYSTEM:
     *
     * Minecraft coordinate
     *       |
     *       v
     * exact decimal raster position
     *       |
     *       v
     * four surrounding raster pixels
     *       |
     *       v
     * bilinear interpolation
     *       |
     *       v
     * smooth elevation value
     *
     *
     * Example:
     *
     * P00 ---------------- P10
     *  |                    |
     *  |        X           |
     *  |                    |
     * P01 ---------------- P11
     *
     * X receives a weighted elevation based on its exact
     * position between P00, P10, P01 and P11.
     * ============================================================
     */

    public static double getElevation(
            double latitude,
            double longitude
    ) {


        /*
         * --------------------------------------------------------
         * SAFETY CHECK
         * --------------------------------------------------------
         */

        if (!isLoaded()) {

            return 0.0;

        }


        /*
         * --------------------------------------------------------
         * CONVERT LATITUDE / LONGITUDE TO RASTER PERCENTAGE
         * --------------------------------------------------------
         */

        double xPercent =
                (longitude - WEST)
                        / (EAST - WEST);


        double yPercent =
                (NORTH - latitude)
                        / (NORTH - SOUTH);


        /*
         * Keep coordinates safely inside the raster.
         */

        xPercent =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                xPercent
                        )
                );


        yPercent =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                yPercent
                        )
                );


        /*
         * --------------------------------------------------------
         * EXACT DECIMAL RASTER POSITION
         * --------------------------------------------------------
         *
         * IMPORTANT:
         *
         * Do NOT round these values.
         */

        int rasterWidth =
                elevationData[0].length;


        int rasterHeight =
                elevationData.length;


        double exactX =
                xPercent
                        * (rasterWidth - 1);


        double exactY =
                yPercent
                        * (rasterHeight - 1);


        /*
         * --------------------------------------------------------
         * FOUR SURROUNDING PIXELS
         * --------------------------------------------------------
         */

        int x0 =
                (int) Math.floor(
                        exactX
                );


        int y0 =
                (int) Math.floor(
                        exactY
                );


        int x1 =
                Math.min(
                        x0 + 1,
                        rasterWidth - 1
                );


        int y1 =
                Math.min(
                        y0 + 1,
                        rasterHeight - 1
                );


        /*
         * --------------------------------------------------------
         * POSITION INSIDE THE FOUR-PIXEL CELL
         * --------------------------------------------------------
         */

        double fractionX =
                exactX - x0;


        double fractionY =
                exactY - y0;


        /*
         * --------------------------------------------------------
         * READ FOUR USGS ELEVATIONS
         * --------------------------------------------------------
         *
         * top-left     = elevation00
         * top-right    = elevation10
         * bottom-left  = elevation01
         * bottom-right = elevation11
         */

        double elevation00 =
                elevationData[
                        y0
                        ][
                        x0
                        ];


        double elevation10 =
                elevationData[
                        y0
                        ][
                        x1
                        ];


        double elevation01 =
                elevationData[
                        y1
                        ][
                        x0
                        ];


        double elevation11 =
                elevationData[
                        y1
                        ][
                        x1
                        ];


        /*
         * --------------------------------------------------------
         * SAFETY FOR INVALID RASTER VALUES
         * --------------------------------------------------------
         *
         * If the TIFF ever contains a non-finite value,
         * fall back to the nearest valid sample instead of
         * allowing NaN or Infinity into world generation.
         */

        if (!Double.isFinite(
                elevation00
        )) {

            elevation00 =
                    findSafeElevation(
                            x0,
                            y0
                    );
        }


        if (!Double.isFinite(
                elevation10
        )) {

            elevation10 =
                    findSafeElevation(
                            x1,
                            y0
                    );
        }


        if (!Double.isFinite(
                elevation01
        )) {

            elevation01 =
                    findSafeElevation(
                            x0,
                            y1
                    );
        }


        if (!Double.isFinite(
                elevation11
        )) {

            elevation11 =
                    findSafeElevation(
                            x1,
                            y1
                    );
        }


        /*
         * --------------------------------------------------------
         * HORIZONTAL INTERPOLATION
         * --------------------------------------------------------
         *
         * Interpolate across the top pair.
         */

        double topElevation =
                interpolate(
                        elevation00,
                        elevation10,
                        fractionX
                );


        /*
         * Interpolate across the bottom pair.
         */

        double bottomElevation =
                interpolate(
                        elevation01,
                        elevation11,
                        fractionX
                );


        /*
         * --------------------------------------------------------
         * VERTICAL INTERPOLATION
         * --------------------------------------------------------
         *
         * Interpolate between the two horizontal results.
         */

        return interpolate(
                topElevation,
                bottomElevation,
                fractionY
        );

    }


    /*
     * ============================================================
     * LINEAR INTERPOLATION
     * ============================================================
     *
     * fraction = 0.0
     * returns start
     *
     * fraction = 1.0
     * returns end
     *
     * fraction = 0.5
     * returns halfway between the two.
     */

    private static double interpolate(
            double start,
            double end,
            double fraction
    ) {

        return start
                + (end - start)
                * fraction;

    }


    /*
     * ============================================================
     * SAFE ELEVATION FALLBACK
     * ============================================================
     *
     * Normally the USGS raster contains valid values everywhere
     * we use it.
     *
     * This is only a protection against an unexpected NaN or
     * Infinity value.
     */

    private static double findSafeElevation(
            int centerX,
            int centerY
    ) {


        int rasterWidth =
                elevationData[0].length;


        int rasterHeight =
                elevationData.length;


        /*
         * Search a small area around the bad pixel.
         */

        for (int radius = 0;
             radius <= 3;
             radius++) {


            for (int offsetY = -radius;
                 offsetY <= radius;
                 offsetY++) {


                for (int offsetX = -radius;
                     offsetX <= radius;
                     offsetX++) {


                    int sampleX =
                            centerX
                                    + offsetX;


                    int sampleY =
                            centerY
                                    + offsetY;


                    if (sampleX < 0
                            || sampleX >= rasterWidth
                            || sampleY < 0
                            || sampleY >= rasterHeight) {

                        continue;

                    }


                    double value =
                            elevationData[
                                    sampleY
                                    ][
                                    sampleX
                                    ];


                    if (Double.isFinite(
                            value
                    )) {

                        return value;

                    }

                }
            }
        }


        /*
         * Last-resort fallback.
         *
         * Sea level / zero real-world elevation is safer than
         * passing an invalid number into the terrain generator.
         */

        return 0.0;

    }

}
