package com.earthbound;


import java.util.Random;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;



/*
 * ============================================================
 * EARTHBOUND WORLD GENERATOR
 *
 * Main world generation coordinator.
 *
 * Current systems:
 *
 * - Terrain
 *
 * Future connections:
 *
 * - Water
 * - Roads
 * - Vegetation
 * - Buildings
 *
 * ============================================================
 */


public class EarthGenerator extends ChunkGenerator {



    public EarthGenerator() {


        System.out.println(
                "=== EARTHBOUND GENERATOR ACTIVE ==="
        );


        System.out.println(
                "[EarthBound] Terrain generator loaded"
        );


    }




    @Override
    public ChunkData generateChunkData(
            World world,
            Random random,
            int chunkX,
            int chunkZ,
            BiomeGrid biome
    ) {


        ChunkData chunkData =
                createChunkData(world);



        /*
         * Convert chunk coordinates
         */

        int startX =
                chunkX << 4;


        int startZ =
                chunkZ << 4;



        /*
         * Generate terrain
         */

        for (
                int x = 0;
                x < 16;
                x++
        ) {


            for (
                    int z = 0;
                    z < 16;
                    z++
            ) {


                int worldX =
                        startX + x;


                int worldZ =
                        startZ + z;



                /*
                 * Temporary terrain height.
                 *
                 * Next step:
                 * connect real USGS elevation.
                 */

                int height =
                        70;



                EarthTerrainGenerator.generateNaturalLandColumn(
                        chunkData,
                        x,
                        z,
                        height
                );


            }

        }



        return chunkData;

    }



}
