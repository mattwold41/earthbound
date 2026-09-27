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

    public static final String USGS_3DEP_SERVICE =
            "https://elevation.nationalmap.gov/"
            + "arcgis/rest/services/"
            + "3DEPElevation/ImageServer";


    public static final double WEST =
            -122.70;

    public static final double SOUTH =
            48.47;

    public static final double EAST =
            -122.55;

    public static final double NORTH =
            48.60;


    public static final int TILE_WIDTH =
            512;

    public static final int TILE_HEIGHT =
            512;


    private static double[][] elevationData;

    private static boolean loaded = false;


    private EarthTerrainDownloader() {
    }


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


            if (!elevationFile.exists()) {

                downloadElevationTile(
                        elevationFile
                );

            } else {

                System.out.println(
                        "Using cached Guemes elevation tile."
                );

            }


            readElevationTile(
                    elevationFile
            );


            loaded = true;


            System.out.println(
                    "=== Guemes USGS elevation loaded ==="
            );


            System.out.println(
                    "Raster size: "
                    + TILE_WIDTH
                    + "x"
                    + TILE_HEIGHT
            );


            System.out.println(
                    "Center elevation sample: "
                    + elevationData[
                            TILE_HEIGHT / 2
                            ][
                            TILE_WIDTH / 2
                            ]
                    + " meters"
            );


        } catch (Exception exception) {

            loaded = false;

            System.err.println(
                    "Failed to load Guemes USGS elevation."
            );

            exception.printStackTrace();

        }

    }


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
                        URI.create(url)
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
                new double[height][width];


        for (int y = 0; y < height; y++) {

            for (int x = 0; x < width; x++) {

                elevationData[y][x] =
                        raster.getSampleDouble(
                                x,
                                y,
                                0
                        );

            }

        }

    }


    public static boolean isLoaded() {

        return loaded
                && elevationData != null;

    }


    public static double getElevation(
            double latitude,
            double longitude
    ) {


        if (!isLoaded()) {

            return 0.0;

        }


        double xPercent =
                (longitude - WEST)
                / (EAST - WEST);


        double yPercent =
                (NORTH - latitude)
                / (NORTH - SOUTH);


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


        int pixelX =
                (int) Math.round(
                        xPercent
                        * (elevationData[0].length - 1)
                );


        int pixelY =
                (int) Math.round(
                        yPercent
                        * (elevationData.length - 1)
                );


        return elevationData[
                pixelY
                ][
                pixelX
                ];

    }

}
