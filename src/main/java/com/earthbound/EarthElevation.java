package com.earthbound;


public class EarthElevation {


    public static double getElevation(
            double x,
            double z
    ) {


        double latitude =
                EarthCoordinates.minecraftToLatitude(
                        (int) z
                );


        double longitude =
                EarthCoordinates.minecraftToLongitude(
                        (int) x
                );


        System.out.println(
                "Earth location: lat="
                + latitude
                + " lon="
                + longitude
        );


        /*
         * Temporary Guemes Island terrain test.
         *
         * This proves the generator works
         * before adding the full USGS database.
         */


        double distance =
                Math.sqrt(
                        (x * x) +
                        (z * z)
                );


        double hills =
                Math.sin(distance / 40.0)
                * 25;


        double base =
                80;


        return base + hills;

    }



    public static int getMinecraftHeight(
            double elevationMeters
    ) {


        int seaLevel = 64;


        double verticalScale = 2.0;


        return seaLevel +
                (int)Math.round(
                        elevationMeters /
                        verticalScale
                );
    }

}
