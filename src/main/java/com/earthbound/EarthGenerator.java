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
 * Current:
 *
 * - Real terrain elevation
 *
 * Future:
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
                "[EarthBound] Real terrain generator loaded"
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



        int startX =
                chunkX << 4;


        int startZ =
                chunkZ << 4;



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
                 * Convert Minecraft position
                 * into Earth coordinates
                 */

                double latitude =
                        EarthCoordinates.getLatitude(
                                worldX,
                                worldZ
                        );


                double longitude =
                        EarthCoordinates.getLongitude(
                                worldX,
                                worldZ
                        );



                /*
                 * Get real elevation
                 */

                int height =
                        EarthTerrainGenerator.getTerrainHeight(
                                latitude,
                                longitude
                        );



                /*
                 * Generate terrain column
                 */

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
