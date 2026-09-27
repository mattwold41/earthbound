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
         * Temporary terrain model.
         * Smooth Guemes Island style hills.
         */


        double largeHill =
                Math.sin(x / 120.0)
                * Math.cos(z / 120.0)
                * 25;


        double smallHill =
                Math.sin(x / 35.0)
                * Math.cos(z / 35.0)
                * 8;


        double shoreline =
                15;


        return shoreline
                + largeHill
                + smallHill;

    }



    public static int getMinecraftHeight(
            double elevationMeters
    ) {


        int seaLevel = 64;


        double verticalScale = 2.0;


        return seaLevel
                + (int)Math.round(
                        elevationMeters
                        / verticalScale
                );

    }

}
