package com.earthbound;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class EarthTerrainLoader {

    /*
     * USGS 3DEP Elevation ImageServer
     */
    private static final String USGS_3DEP_SERVICE =
            "https://elevation.nationalmap.gov/arcgis/rest/services/"
            + "3DEPElevation/ImageServer";


    private EarthTerrainLoader() {
        // Utility class
    }


    /*
     * Loads a single elevation point.
     *
     * Uses the cache first.
     * If missing, downloads from USGS and stores it.
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
     * Converts real elevation into Minecraft height.
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
     * Tests connection to USGS 3DEP.
     *
     * We only check that USGS responds with data.
     */
    public static boolean test3DEPConnection() {

        try {

            String address =
                    USGS_3DEP_SERVICE
                    + "?f=pjson";


            URL url =
                    URI.create(address).toURL();


            HttpURLConnection connection =
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
        }
    }



    /*
     * Creates a future terrain tile request.
     *
     * Used when we start downloading Guemes Island
     * elevation areas instead of single points.
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
                + west
                + ","
                + south
                + ","
                + east
                + ","
                + north
                + "&bboxSR=4326"
                + "&imageSR=4326"
                + "&size="
                + width
                + ","
                + height
                + "&format=tiff"
                + "&pixelType=F32"
                + "&interpolation=RSP_BilinearInterpolation"
                + "&f=image";
    }
}
