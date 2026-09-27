package com.earthbound;


public class EarthTerrainLoader {


    public static void loadGuemesTerrainTile() {

        System.out.println(
                "[EarthBound] Guemes Island terrain system loaded"
        );

    }



    public static boolean isGuemesTerrainLoaded() {

        return true;

    }



    public static double getGuemesElevation(
            double x,
            double z
    ) {


        return EarthElevation.getElevation(
                x,
                z
        );

    }

}
