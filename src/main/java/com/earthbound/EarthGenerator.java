package com.earthbound;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;


/*
 * ============================================================
 * EARTHBOUND WORLD GENERATOR
 *
 * Main coordinator.
 *
 * Terrain, roads, water, vegetation,
 * and buildings will connect here.
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
         * Systems will be added here:
         *
         * Terrain
         * Water
         * Roads
         * Vegetation
         * Buildings
         *
         */



        return chunkData;

    }


}
