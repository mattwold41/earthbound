package com.earthbound;


import org.bukkit.generator.ChunkGenerator.ChunkData;


import com.earthbound.water.EarthWaterData;
import com.earthbound.roads.EarthRoadData;
import com.earthbound.terrain.EarthTerrainLoader;
import com.earthbound.terrain.EarthElevation;



/*
 * ============================================================
 * EARTHBOUND GUEMES GENERATOR
 *
 * Controls Guemes Island development.
 *
 * This file connects:
 *
 * - Water
 * - Roads
 * - Terrain
 * - Elevation
 *
 * ============================================================
 */


public final class EarthGuemesGenerator {



    private static final int MINIMUM_BUILD_HEIGHT =
            EarthWaterData.SEA_LEVEL + 3;



    private static final boolean ENABLE_ISLAND_HOMES =
            true;



    private EarthGuemesGenerator() {

    }




    public static void generate(
            ChunkData chunkData,
            int chunkX,
            int chunkZ
    ) {


        if (!ENABLE_ISLAND_HOMES) {

            return;

        }



        if (!EarthRoadData.isLoaded()) {

            return;

        }



        /*
         * Future:
         *
         * Residential districts
         * Roadside homes
         * Farms
         * Businesses
         *
         */



    }




    public static boolean isSuitableForDevelopment(
            double latitude,
            double longitude
    ) {



        double elevationMeters =
                EarthTerrainLoader.getGuemesElevation(
                        latitude,
                        longitude
                );



        int minecraftHeight =
                EarthElevation.getMinecraftHeight(
                        elevationMeters
                );



        return minecraftHeight >= MINIMUM_BUILD_HEIGHT;

    }



}
