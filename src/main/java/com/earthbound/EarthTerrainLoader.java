package com.earthbound;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import java.util.Iterator;

public class EarthTerrainLoader {

    /*
     * Official USGS 3DEP Elevation ImageServer
     */
    private static final String USGS_3DEP_SERVICE =
            "https://elevation.nationalmap.gov/arcgis/rest/services/"
            + "3DEPElevation/ImageServer";


    /*
     * Guemes Island test area.
     *
     * This is intentionally a 512 x 512 test raster.
     * Later we will divide the Earth into higher-resolution
     * cached tiles appropriate for the 2 meter/block scale.
     */
    private static final double GUEMES_WEST =
            -122.70;

    private static final double GUEMES_SOUTH =
            48.47;

    private static final double GUEMES_EAST =
            -122.55;

    private static final double GUEMES_NORTH =
            48.60;


    private static final int GUEMES_TILE_WIDTH =
            512;

    private static final int GUEMES_TILE_HEIGHT =
            512;


    /*
     * Decoded Guemes elevation raster.
     *
     * Indexed:
     *
     * [y][x]
     *
     * Values are elevation in meters.
     */
    private static volatile float[][] guemesElevationGrid = null;


    private EarthTerrainLoader() {
        // Utility class
    }


    /*
     * Existing single-point elevation loader.
     *
     * This remains useful for /earth locate.
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
     * progressive Minecraft height scale.
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
     * Tests basic access to USGS 3DEP.
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
     * Creates a USGS floating-point TIFF request.
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
     * Downloads and decodes the Guemes elevation TIFF.
     */
    public static boolean loadGuemesTerrainTile() {

        HttpURLConnection connection = null;

        try {

            System.out.println(
                    "[EarthBound] Downloading Guemes elevation raster..."
            );

            URL url =
                    URI.create(
                            createGuemesTerrainTileURL()
                    ).toURL();

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
                        "[EarthBound] Guemes raster request failed. HTTP "
                                + responseCode
                );

                return false;
            }


            byte[] data;

            try (InputStream input =
                         connection.getInputStream();

                 ByteArrayOutputStream output =
                         new ByteArrayOutputStream()) {

                byte[] buffer =
                        new byte[8192];

                int bytesRead;

                while ((bytesRead =
                                input.read(buffer)) != -1) {

                    output.write(
                            buffer,
                            0,
                            bytesRead
                    );
                }

                data =
                        output.toByteArray();
            }


            System.out.println(
                    "[EarthBound] Guemes TIFF downloaded: "
                            + data.length
                            + " bytes"
            );


            return decodeGuemesRaster(data);


        } catch (Exception exception) {

            System.out.println(
                    "[EarthBound] Could not load Guemes terrain raster."
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
     * Decode the floating-point TIFF returned by USGS.
     */
    private static boolean decodeGuemesRaster(
            byte[] data) {

        try {

            ByteArrayInputStream byteStream =
                    new ByteArrayInputStream(data);

            ImageInputStream imageStream =
                    ImageIO.createImageInputStream(
                            byteStream
                    );

            if (imageStream == null) {

                System.out.println(
                        "[EarthBound] Could not create TIFF image stream."
                );

                return false;
            }


            Iterator<ImageReader> readers =
                    ImageIO.getImageReaders(
                            imageStream
                    );


            if (!readers.hasNext()) {

                System.out.println(
                        "[EarthBound] No Java TIFF reader was found."
                );

                imageStream.close();

                return false;
            }


            ImageReader reader =
                    readers.next();


            try {

                reader.setInput(
                        imageStream,
                        true,
                        true
                );


                BufferedImage image =
                        reader.read(0);


                Raster raster =
                        image.getRaster();


                int width =
                        raster.getWidth();

                int height =
                        raster.getHeight();


                System.out.println(
                        "[EarthBound] Decoded Guemes raster: "
                                + width
                                + " x "
                                + height
                );


                float[][] grid =
                        new float[height][width];


                for (int y = 0;
                     y < height;
                     y++) {

                    for (int x = 0;
                         x < width;
                         x++) {

                        grid[y][x] =
                                raster.getSampleFloat(
                                        x,
                                        y,
                                        0
                                );
                    }
                }


                guemesElevationGrid =
                        grid;


                int centerX =
                        width / 2;

                int centerY =
                        height / 2;


                float centerElevation =
                        grid[centerY][centerX];


                System.out.println(
                        "[EarthBound] Guemes raster decoded successfully."
                );

                System.out.println(
                        "[EarthBound] Center elevation sample: "
                                + centerElevation
                                + " meters"
                );


                return true;


            } finally {

                reader.dispose();
                imageStream.close();
            }


        } catch (Exception exception) {

            System.out.println(
                    "[EarthBound] Failed to decode Guemes TIFF."
            );

            exception.printStackTrace();

            return false;
        }
    }


    /*
     * Returns whether the Guemes raster
     * has been downloaded and decoded.
     */
    public static boolean isGuemesTerrainLoaded() {

        return guemesElevationGrid != null;
    }


    /*
     * Reads an elevation from the loaded Guemes raster
     * using latitude and longitude.
     */
    public static Double getGuemesElevation(
            double latitude,
            double longitude) {

        float[][] grid =
                guemesElevationGrid;


        if (grid == null) {
            return null;
        }


        if (latitude < GUEMES_SOUTH
                || latitude > GUEMES_NORTH
                || longitude < GUEMES_WEST
                || longitude > GUEMES_EAST) {

            return null;
        }


        double xPercent =
                (longitude - GUEMES_WEST)
                        / (GUEMES_EAST - GUEMES_WEST);


        /*
         * Raster row 0 is the north edge.
         */
        double yPercent =
                (GUEMES_NORTH - latitude)
                        / (GUEMES_NORTH - GUEMES_SOUTH);


        int width =
                grid[0].length;

        int height =
                grid.length;


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


        return (double) grid[y][x];
    }


    /*
     * Compatibility method used by the previous test.
     */
    public static boolean testGuemesTerrainTile() {

        return loadGuemesTerrainTile();
    }
}
