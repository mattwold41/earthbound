package com.earthbound;


import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator.ChunkData;


import com.earthbound.terrain.EarthTerrainLoader;
import com.earthbound.terrain.EarthElevation;


/*
 * ============================================================
 * EARTHBOUND TERRAIN GENERATOR
 *
 * Handles terrain block placement only.
 *
 * Data comes from:
 *
 * EarthTerrainLoader
 * EarthElevation
 *
 * ============================================================
 */


public class EarthTerrainGenerator {



    /*
     * Generate a natural terrain column
     */

    public static void generateNaturalLandColumn(
            ChunkData chunkData,
            int x,
            int z,
            int height
    ) {


        if (height < 1) {

            height = 1;

        }



        for (
                int y = 0;
                y <= height;
                y++
        ) {


            if (y == height) {


                chunkData.setBlock(
                        x,
                        y,
                        z,
                        Material.GRASS_BLOCK
                );


            } else if (y > height - 4) {


                chunkData.setBlock(
                        x,
                        y,
                        z,
                        Material.DIRT
                );


            } else {


                chunkData.setBlock(
                        x,
                        y,
                        z,
                        Material.STONE
                );

            }

        }

    }




    /*
     * Get real Earth elevation
     */

    public static int getTerrainHeight(
            double latitude,
            double longitude
    ) {


        double elevationMeters =
                EarthTerrainLoader.getGuemesElevation(
                        latitude,
                        longitude
                );


        return EarthElevation.getMinecraftHeight(
                elevationMeters
        );

    }



}
