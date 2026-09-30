package com.earthbound.water;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator.ChunkData;


/*
 * ============================================================
 * EARTHBOUND WATER GENERATOR
 *
 * Handles:
 *
 * - Ocean/lake generation
 * - Water columns
 * - Water placement
 *
 * ============================================================
 */


public class EarthWaterGenerator {


    private static final int SEA_LEVEL = 63;



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
            int z
    ) {



        for (
                int y = 0;
                y <= SEA_LEVEL;
                y++
        ) {



            if (y < SEA_LEVEL) {


                chunkData.setBlock(
                        x,
                        y,
                        z,
                        Material.STONE
                );


            } else {


                chunkData.setBlock(
                        x,
                        y,
                        z,
                        Material.WATER
                );


            }

        }


    }


}
