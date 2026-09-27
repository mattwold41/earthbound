package com.earthbound;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class EarthElevation {


    public static double getElevation(
            double longitude,
            double latitude
    ) {

        try {

            String requestUrl =
                    "https://epqs.nationalmap.gov/v1/json"
                    + "?x=" + longitude
                    + "&y=" + latitude
                    + "&units=Meters"
                    + "&wkid=4326"
                    + "&includeDate=false";


            System.out.println(
                    "[EarthBound] Requesting elevation: "
                    + requestUrl
            );


            URL url = new URL(requestUrl);


            HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection();


            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);


            int responseCode =
                    connection.getResponseCode();


            System.out.println(
                    "[EarthBound] USGS response code: "
                    + responseCode
            );


            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    connection.getInputStream()
                            )
                    );


            StringBuilder response =
                    new StringBuilder();


            String line;

            while ((line = reader.readLine()) != null) {

                response.append(line);

            }


            reader.close();


            String json =
                    response.toString();


            System.out.println(
                    "[EarthBound] USGS response: "
                    + json
            );


            String key =
                    "\"value\":";


            int index =
                    json.indexOf(key);


            if (index != -1) {

                int start =
                        index + key.length();


                int end =
                        json.indexOf(
                                ",",
                                start
                        );


                if (end == -1) {

                    end =
                            json.indexOf(
                                    "}",
                                    start
                            );
                }


                double elevation =
                        Double.parseDouble(
                                json.substring(
                                        start,
                                        end
                                ).trim()
                        );


                System.out.println(
                        "[EarthBound] Real elevation: "
                        + elevation
                        + " meters"
                );


                return elevation;
            }


            System.out.println(
                    "[EarthBound] Could not find elevation value."
            );


        } catch (Exception e) {


            System.out.println(
                    "[EarthBound] Elevation error: "
                    + e.getMessage()
            );

        }


        return 0.0;
    }
}
