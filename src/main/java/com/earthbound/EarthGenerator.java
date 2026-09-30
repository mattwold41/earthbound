package com.earthbound;


import java.util.Random;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;


import com.earthbound.terrain.EarthTerrainGenerator;
import com.earthbound.water.EarthWaterGenerator;
import com.earthbound.roads.EarthRoadGenerator;
import com.earthbound.vegetation.EarthVegetationGenerator;



/*
 * ============================================================
 * EARTHBOUND WORLD GENERATOR
 *
 * Main coordinator.
 *
 * Connects:
 *
 * Terrain
 * Water
 * Roads
 * Vegetation
 * Buildings
 *
 * ============================================================
 */


public class EarthGenerator extends ChunkGenerator {



    public EarthGenerator() {


        System.out.println(
                "=== EARTHBOUND GENERATOR ACTIVE ==="
        );


        System.out.println(
                "[EarthBound] Modular terrain system loaded"
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
         * into block coordinates
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
                 * Temporary height.
                 *
                 * Next step will connect
                 * real latitude/longitude
                 * elevation here.
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



        /*
         * Add water
         */

        EarthWaterGenerator.generate(
                chunkData,
                chunkX,
                chunkZ
        );



        /*
         * Add roads
         */

        EarthRoadGenerator.generate(
                chunkData,
                chunkX,
                chunkZ
        );



        /*
         * Add vegetation
         */

        EarthVegetationGenerator.generate(
                chunkData,
                chunkX,
                chunkZ
        );



        return chunkData;

    }



}
