package com.earthbound;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import com.earthbound.roads.EarthRoadData;
import com.earthbound.roads.EarthRoadData.RoadType;
import com.earthbound.water.EarthWaterGenerator;

public class EarthGenerator extends ChunkGenerator {

    private static final double METERS_PER_BLOCK = 2.0;
    private static final double ROAD_BORDER_BLOCKS = 1.0;

    public EarthGenerator() {
        System.out.println(
                "=== EARTHBOUND GENERATOR ACTIVE ===");

        System.out.println(
                "[EarthBound] Guemes terrain + water + roads"
                        + " + General Store loaded");
    }

    @Override
    public ChunkData generateChunkData(
            World world,
            Random random,
            int chunkX,
            int chunkZ,
            BiomeGrid biome) {

        ChunkData chunkData = createChunkData(world);

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {

                int worldX = chunkX * 16 + localX;
                int worldZ = chunkZ * 16 + localZ;

                double latitude =
                        EarthCoordinates.getLatitude(
                                worldX, worldZ);

                double longitude =
                        EarthCoordinates.getLongitude(
                                worldX, worldZ);

                boolean storeSite =
                        EarthBuildingGenerator
                                .isInsideGuemesStoreSite(
                                        worldX, worldZ);

                /*
                 * WATER
                 *
                 * Preserve the existing coastline.
                 * Only the approved store site overrides
                 * water classification.
                 */
                if (!storeSite
                        && EarthWaterGenerator.isWater(
                                latitude, longitude)) {

                    EarthWaterGenerator.generateWaterColumn(
                            chunkData,
                            localX,
                            localZ,
                            latitude,
                            longitude);

                    continue;
                }

                /*
                 * TERRAIN
                 */
                int naturalHeight =
                        EarthTerrainGenerator.getTerrainHeight(
                                latitude, longitude);

                int terrainHeight =
                        EarthBuildingGenerator
                                .getBlendedTerrainHeight(
                                        worldX,
                                        worldZ,
                                        naturalHeight);

                EarthTerrainGenerator.generateNaturalLandColumn(
                        chunkData,
                        localX,
                        localZ,
                        terrainHeight);

                /*
                 * STORE FOUNDATION
                 *
                 * Keep the site at Y=65.
                 * Roads must not overwrite the building
                 * or its western porch.
                 */
                if (storeSite) {
                    continue;
                }

                /*
                 * ROADS
                 *
                 * Preserve the existing road system.
                 */
                RoadType roadType =
                        EarthRoadData.getNearestRoadType(
                                latitude, longitude);

                if (roadType == RoadType.NONE) {
                    continue;
                }

                double distanceMeters =
                        EarthRoadData
                                .getDistanceToRoadTypeMeters(
                                        roadType,
                                        latitude,
                                        longitude);

                double drivingRadiusMeters =
                        (roadType.getWidth()
                                * METERS_PER_BLOCK) / 2.0;

                double borderRadiusMeters =
                        drivingRadiusMeters
                                + ROAD_BORDER_BLOCKS
                                * METERS_PER_BLOCK;

                if (distanceMeters > borderRadiusMeters) {
                    continue;
                }

                int roadY = terrainHeight;

                if (distanceMeters <= drivingRadiusMeters) {

                    chunkData.setBlock(
                            localX,
                            roadY,
                            localZ,
                            Material.POLISHED_BLACKSTONE_BRICKS);

                } else {

                    chunkData.setBlock(
                            localX,
                            roadY,
                            localZ,
                            Material.STONE_BRICKS);
                }
            }
        }

        /*
         * GENERATE GUEMES GENERAL STORE
         *
         * Runs after terrain, water and roads.
         * Block placement is restricted to this chunk.
         */
        EarthBuildingGenerator.generateGuemesStore(
                chunkData,
                chunkX,
                chunkZ);

        return chunkData;
    }
}
