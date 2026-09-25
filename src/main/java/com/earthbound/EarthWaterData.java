package com.earthbound;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class EarthWaterData {

    /*
     * Minecraft sea level.
     */
    public static final int SEA_LEVEL = 63;


    /*
     * U.S. Census Bureau TIGERweb
     * Areal Hydrography layer.
     *
     * This contains polygon water features.
     */
    private static final String HYDRO_SERVICE =
            "https://tigerweb.geo.census.gov/"
                    + "arcgis/rest/services/TIGERweb/Hydro/"
                    + "MapServer/1/query";


    private EarthWaterData() {
        // Utility class
    }


    /*
     * Checks whether a latitude/longitude point
     * falls inside an areal hydrography polygon.
     *
     * IMPORTANT:
     * We will NOT call this once per Minecraft block
     * during terrain generation.
     *
     * This method is the data-source connection.
     * We will add a cached Guemes water mask next.
     */
    public static boolean queryWater(
            double latitude,
            double longitude) {

        HttpURLConnection connection = null;

        try {

            String address =
                    HYDRO_SERVICE
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
                        "[EarthBound] Water lookup HTTP "
                                + responseCode
                );

                return false;
            }


            StringBuilder response =
                    new StringBuilder();


            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
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


            /*
             * ArcGIS returns:
             *
             * {"count":0}
             *
             * for land, and a count greater
             * than zero when the point intersects
             * a water polygon.
             */
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

            System.out.println(
                    "[EarthBound] Water lookup failed."
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
     * Temporary compatibility method.
     *
     * We are deliberately NOT performing
     * Internet requests here yet.
     *
     * The next step will build a Guemes
     * water mask in memory and this method
     * will read from that mask.
     */
    public static boolean isWater(
            double latitude,
            double longitude) {

        return false;
    }
}
