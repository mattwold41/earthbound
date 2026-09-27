package com.earthbound;

public class EarthElevation {


    public static double getElevation(
            double x,
            double z
    ) {


        /*
         * Test terrain for Guemes Island.
         *
         * Creates hills so we know
         * the generator is working.
         *
         * Later this is replaced
         * with real USGS elevation data.
         */


        double hill1 =
                Math.sin(x * 0.02)
                * 20;


        double hill2 =
                Math.cos(z * 0.015)
                * 15;


        double hill3 =
                Math.sin((x + z) * 0.01)
                * 10;


        return 30
                + hill1
                + hill2
                + hill3;

    }



    public static int getMinecraftHeight(
            double elevationMeters
    ) {


        int seaLevel = 64;


        double verticalScale = 1.5;


        return seaLevel
                + (int)Math.round(
                        elevationMeters
                        / verticalScale
                );

    }

}
