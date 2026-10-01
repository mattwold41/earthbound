package com.earthbound.terrain;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.net.HttpURLConnection;
import java.net.URL;

import javax.imageio.ImageIO;


/*
 * ============================================================
 * EARTHBOUND TERRAIN DOWNLOADER
 *
 * Downloads real USGS 3DEP elevation data for Guemes Island.
 *
 * Data source:
 * U.S. Geological Survey
 * 3D Elevation Program (3DEP)
 *
 * Raster size:
 * 512 x 512
 *
 * ============================================================
 */


public class EarthTerrainDownloader {


    /*
     * Guemes Island geographic bounds
     */

    public static final double WEST =
            -122.70;

    public static final double SOUTH =
            48.47;

    public static final double EAST =
            -122.55;

    public static final double NORTH =
            48.60;


    public static final int RASTER_WIDTH =
            512;

    public static final int RASTER_HEIGHT =
            512;


    /*
     * USGS 3DEP elevation service
     */

    private static final String USGS_SERVICE =
            "https://elevation.nationalmap.gov/"
            + "arcgis/rest/services/"
            + "3DEPElevation/ImageServer/exportImage";


    private static boolean loaded =
            false;



    /*
     * ========================================================
     * LOAD GUEMES ISLAND
     * ========================================================
     */

    public static void loadGuemesIsland() {


        System.out.println(
                "[EarthBound] Loading real Guemes terrain..."
        );


        try {


            File terrainFile =
                    downloadGuemesElevation();


            EarthTerrainData terrainData =
                    decodeElevationRaster(
                            terrainFile
                    );


            EarthTerrainLoader.load(
                    terrainData
            );


            loaded = true;


            System.out.println(
                    "[EarthBound] Guemes terrain loaded!"
            );


            /*
             * Diagnostic sample.
             *
             * This should NOT be exactly the same
             * everywhere once real elevation is loaded.
             */

            int centerX =
                    RASTER_WIDTH / 2;

            int centerZ =
                    RASTER_HEIGHT / 2;


            double centerElevation =
                    terrainData.getElevation(
                            centerX,
                            centerZ
                    );


            System.out.println(
                    "[EarthBound] Terrain raster: "
                            + RASTER_WIDTH
                            + "x"
                            + RASTER_HEIGHT
            );


            System.out.println(
                    "[EarthBound] Center elevation sample: "
                            + centerElevation
                            + " meters"
            );


        } catch (Exception exception) {


            loaded = false;


            System.err.println(
                    "[EarthBound] ERROR loading Guemes terrain!"
            );


            exception.printStackTrace();

        }


    }



    /*
     * ========================================================
     * DOWNLOAD USGS ELEVATION TIFF
     * ========================================================
     */

    private static File downloadGuemesElevation()
            throws Exception {


        String requestUrl =
                USGS_SERVICE
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
                        + RASTER_WIDTH
                        + ","
                        + RASTER_HEIGHT

                        + "&format=tiff"

                        + "&pixelType=F32"

                        + "&interpolation=RSP_BilinearInterpolation"

                        + "&f=image";


        System.out.println(
                "[EarthBound] Downloading USGS Guemes elevation raster..."
        );


        URL url =
                new URL(
                        requestUrl
                );


        HttpURLConnection connection =
                (HttpURLConnection)
                        url.openConnection();


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
                "EarthBound-Minecraft-Server/0.1.0"
        );


        int responseCode =
                connection.getResponseCode();


        if (
                responseCode
                        != HttpURLConnection.HTTP_OK
        ) {


            throw new IllegalStateException(
                    "USGS elevation request failed. HTTP "
                            + responseCode
            );

        }


        File temporaryFile =
                File.createTempFile(
                        "earthbound-guemes-elevation-",
                        ".tif"
                );


        temporaryFile.deleteOnExit();


        long totalBytes =
                0;


        try (
                InputStream input =
                        new BufferedInputStream(
                                connection.getInputStream()
                        );

                FileOutputStream output =
                        new FileOutputStream(
                                temporaryFile
                        )
        ) {


            byte[] buffer =
                    new byte[8192];


            int bytesRead;


            while (
                    (bytesRead =
                            input.read(buffer))
                            != -1
            ) {


                output.write(
                        buffer,
                        0,
                        bytesRead
                );


                totalBytes +=
                        bytesRead;

            }


        } finally {


            connection.disconnect();

        }


        if (totalBytes <= 0) {


            throw new IllegalStateException(
                    "USGS elevation download was empty."
            );

        }


        System.out.println(
                "[EarthBound] Elevation TIFF downloaded: "
                        + totalBytes
                        + " bytes"
        );


        return temporaryFile;

    }



    /*
     * ========================================================
     * DECODE TIFF
     * ========================================================
     */

    private static EarthTerrainData
            decodeElevationRaster(
                    File terrainFile
            )
            throws Exception {


        System.out.println(
                "[EarthBound] Decoding elevation TIFF..."
        );


        BufferedImage image =
                ImageIO.read(
                        terrainFile
                );


        if (image == null) {


            throw new IllegalStateException(
                    "Java could not decode the USGS TIFF."
            );

        }


        int width =
                image.getWidth();

        int height =
                image.getHeight();


        System.out.println(
                "[EarthBound] Decoded raster: "
                        + width
                        + "x"
                        + height
        );


        if (
                width != RASTER_WIDTH
                        ||
                height != RASTER_HEIGHT
        ) {


            throw new IllegalStateException(
                    "Unexpected elevation raster size: "
                            + width
                            + "x"
                            + height
            );

        }


        Raster raster =
                image.getRaster();


        EarthTerrainData terrainData =
                new EarthTerrainData(
                        width,
                        height
                );


        for (
                int x = 0;
                x < width;
                x++
        ) {


            for (
                    int z = 0;
                    z < height;
                    z++
            ) {


                double elevation =
                        raster.getSampleDouble(
                                x,
                                z,
                                0
                        );


                /*
                 * Protect EarthBound from invalid
                 * elevation values.
                 */

                if (
                        Double.isNaN(elevation)
                                ||
                        Double.isInfinite(elevation)
                                ||
                        elevation < -1000
                                ||
                        elevation > 10000
                ) {


                    elevation =
                            0;

                }


                terrainData.setElevation(
                        x,
                        z,
                        elevation
                );

            }

        }


        return terrainData;

    }



    /*
     * ========================================================
     * STATUS
     * ========================================================
     */

    public static boolean isLoaded() {


        return loaded;

    }


}
