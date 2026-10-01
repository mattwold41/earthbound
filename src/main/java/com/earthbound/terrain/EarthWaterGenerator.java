package com.earthbound.water;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import com.earthbound.EarthTerrainGenerator;


/*
 * ============================================================
 * EARTHBOUND WATER GENERATOR
 *
 * Generates natural coastal water using:
 *
 * - Real TIGERweb water boundaries
 * - Real USGS terrain elevation
 * - Natural ocean floor
 * - Water filled to sea level
 *
 * ============================================================
 */


public class EarthWaterGenerator {


    private static final int SEA_LEVEL = 63;

    /*
     * Maximum floor height for a water coordinate.
     *
     * This prevents the elevation raster from creating
     * land above sea level inside an area that the
     * hydrography data identifies as water.
     */
    private static final int MAX_WATER_FLOOR = 61;


    /*
     * ============================================================
     * WATER CHECK
     * ============================================================
     */


    public static boolean isWater(
            double latitude,
            double longitude
    ) {


        return EarthWaterData.isWater(
                latitude,
                longitude
        );

    }


    /*
     * ============================================================
     * GENERATE WATER COLUMN
     * ============================================================
     */


    public static void generateWaterColumn(
            ChunkData chunkData,
            int x,
            int z,
            double latitude,
            double longitude
    ) {


        /*
         * Start with the same real USGS elevation used
         * by the land generator.
         *
         * This allows the underwater floor to follow
         * the surrounding real terrain instead of
         * becoming a giant flat stone platform.
         */


        int terrainHeight =
                EarthTerrainGenerator.getTerrainHeight(
                        latitude,
                        longitude
                );


        /*
         * Hydrography decides that this location is water.
         *
         * Therefore its floor must remain below sea level.
         */


        int floorHeight =
                Math.min(
                        terrainHeight,
                        MAX_WATER_FLOOR
                );


        /*
         * Safety minimum.
         */


        if (floorHeight < 1) {

            floorHeight = 1;

        }


        /*
         * ========================================================
         * BUILD OCEAN / LAKE FLOOR
         * ========================================================
         *
         * Stone forms the deeper foundation.
         *
         * Dirt forms the upper substrate.
         *
         * Sand forms the exposed floor.
         */


        for (
                int y = 0;
                y <= floorHeight;
                y++
        ) {


            Material material;


            if (y == floorHeight) {

                material = Material.SAND;

            } else if (y >= floorHeight - 2) {

                material = Material.DIRT;

            } else {

                material = Material.STONE;

            }


            chunkData.setBlock(
                    x,
                    y,
                    z,
                    material
            );

        }


        /*
         * ========================================================
         * FILL WITH WATER
         * ========================================================
         *
         * Everything above the floor through sea level
         * becomes water.
         */


        for (
                int y = floorHeight + 1;
                y <= SEA_LEVEL;
                y++
        ) {


            chunkData.setBlock(
                    x,
                    y,
                    z,
                    Material.WATER
            );

        }


    }


}
