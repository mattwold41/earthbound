package com.earthbound;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import com.earthbound.roads.EarthRoadData;
import com.earthbound.roads.EarthRoadData.RoadType;

import com.earthbound.roads.EarthRoadSpatialIndex;
import com.earthbound.roads.EarthRoadSpatialIndex.RoadColumn;

import com.earthbound.water.EarthWaterGenerator;

public class EarthGenerator extends ChunkGenerator {

    private static final double METERS_PER_BLOCK = 2.0;

    private static final double ROAD_BORDER_BLOCKS = 1.0;

    private static final int MINIMUM_ROAD_HEIGHT = 64;

    public EarthGenerator() {

        System.out.println(
                "=== EARTHBOUND GENERATOR ACTIVE ==="
        );

        System.out.println(
                "[EarthBound] Terrain, water, roads "
                + "and General Store generator initialized."
        );
    }

    /*
     * =========================================================
     * EARTHBOUND CHUNK GENERATION
     * =========================================================
     */

    @Override
    public ChunkData generateChunkData(
            World world,
            Random random,
            int chunkX,
            int chunkZ,
            BiomeGrid biome
    ) {

        ChunkData chunkData = createChunkData(world);

        boolean smoothingAvailable =
                EarthRoadSpatialIndex.isLoaded();

        for (int localX = 0; localX < 16; localX++) {

            for (int localZ = 0; localZ < 16; localZ++) {

                int worldX = chunkX * 16 + localX;
                int worldZ = chunkZ * 16 + localZ;

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

                boolean storeSite =
                        EarthBuildingGenerator
                                .isInsideGuemesStoreSite(
                                        worldX,
                                        worldZ
                                );

                boolean storeBlendArea =
                        EarthBuildingGenerator
                                .isInsideGuemesStoreBlendArea(
                                        worldX,
                                        worldZ
                                );

                /*
                 * =================================================
                 * WATER GENERATION
                 * =================================================
                 */

                if (!storeSite
                        && EarthWaterGenerator.isWater(
                                latitude,
                                longitude
                        )) {

                    EarthWaterGenerator.generateWaterColumn(
                            chunkData,
                            localX,
                            localZ,
                            latitude,
                            longitude
                    );

                    continue;
                }

                /*
                 * =================================================
                 * NATURAL TERRAIN
                 * =================================================
                 */

                int naturalHeight =
                        EarthTerrainGenerator.getTerrainHeight(
                                latitude,
                                longitude
                        );

                int terrainHeight =
                        EarthBuildingGenerator
                                .getBlendedTerrainHeight(
                                        worldX,
                                        worldZ,
                                        naturalHeight
                                );

                /*
                 * =================================================
                 * PROTECT GENERAL STORE FOUNDATION
                 * =================================================
                 */

                if (storeSite) {

                    terrainHeight = clampTerrainHeight(
                            chunkData,
                            terrainHeight
                    );

                    EarthTerrainGenerator
                            .generateNaturalLandColumn(
                                    chunkData,
                                    localX,
                                    localZ,
                                    terrainHeight
                            );

                    continue;
                }

                /*
                 * =================================================
                 * ORIGINAL ROAD FALLBACK
                 * =================================================
                 *
                 * Preserves road sections without valid
                 * smoothed elevation profiles.
                 */

                Material roadMaterial =
                        getOriginalRoadMaterial(
                                latitude,
                                longitude
                        );

                /*
                 * =================================================
                 * IMPROVED ROAD GENERATION
                 * =================================================
                 */

                if (smoothingAvailable
                        && !storeBlendArea
                        && naturalHeight
                                >= MINIMUM_ROAD_HEIGHT) {

                    RoadColumn roadColumn =
                            EarthRoadSpatialIndex
                                    .getRoadColumn(
                                            worldX,
                                            worldZ
                                    );

                    if (roadColumn != null) {

                        int roadHeight =
                                roadColumn.getRoadHeight();

                        boolean validRoadHeight =
                                roadHeight
                                        >= MINIMUM_ROAD_HEIGHT
                                && roadHeight
                                        < chunkData.getMaxHeight();

                        if (validRoadHeight) {

                            /*
                             * Main driving surface.
                             */

                            if (roadColumn.isDrivingSurface()) {

                                terrainHeight = roadHeight;

                                roadMaterial =
                                        Material
                                                .POLISHED_BLACKSTONE_BRICKS;

                            /*
                             * Stone brick road border.
                             *
                             * Do not replace an existing
                             * driving surface with a border.
                             */

                            } else if (
                                    roadColumn.isRoadBorder()
                            ) {

                                if (roadMaterial
                                        != Material
                                                .POLISHED_BLACKSTONE_BRICKS) {

                                    terrainHeight = roadHeight;

                                    roadMaterial =
                                            Material.STONE_BRICKS;
                                }

                            /*
                             * Natural terrain transition
                             * alongside the road.
                             *
                             * Never overwrite fallback roads.
                             */

                            } else if (roadMaterial == null) {

                                terrainHeight =
                                        roadColumn
                                                .getBlendedTerrainHeight(
                                                        naturalHeight
                                                );

                                terrainHeight = Math.max(
                                        MINIMUM_ROAD_HEIGHT,
                                        terrainHeight
                                );
                            }
                        }
                    }
                }

                /*
                 * =================================================
                 * GENERATE FINAL TERRAIN COLUMN
                 * =================================================
                 */

                terrainHeight = clampTerrainHeight(
                        chunkData,
                        terrainHeight
                );

                EarthTerrainGenerator.generateNaturalLandColumn(
                        chunkData,
                        localX,
                        localZ,
                        terrainHeight
                );

                /*
                 * =================================================
                 * PLACE ROAD SURFACE
                 * =================================================
                 */

                if (roadMaterial != null) {

                    chunkData.setBlock(
                            localX,
                            terrainHeight,
                            localZ,
                            roadMaterial
                    );
                }
            }
        }

        /*
         * =========================================================
         * GUEMES GENERAL STORE
         * =========================================================
         */

        EarthBuildingGenerator.generateGuemesStore(
                chunkData,
                chunkX,
                chunkZ
        );

        /*
         * =========================================================
         * FRIENDS ALPHA RESIDENTIAL DEVELOPMENT
         * =========================================================
         */

        if ("earthbound".equals(world.getName())) {

            EarthGuemesGenerator.generate(
                    chunkData,
                    chunkX,
                    chunkZ
            );
        }

        return chunkData;
    }

    /*
     * =========================================================
     * ORIGINAL ROAD MATERIAL
     * =========================================================
     */

    private static Material getOriginalRoadMaterial(
            double latitude,
            double longitude
    ) {

        RoadType roadType =
                EarthRoadData.getNearestRoadType(
                        latitude,
                        longitude
                );

        if (roadType == RoadType.NONE) {

            return null;
        }

        double distanceMeters =
                EarthRoadData
                        .getDistanceToRoadTypeMeters(
                                roadType,
                                latitude,
                                longitude
                        );

        if (!Double.isFinite(distanceMeters)) {

            return null;
        }

        double drivingRadiusMeters =
                roadType.getWidth()
                        * METERS_PER_BLOCK
                        / 2.0;

        double borderRadiusMeters =
                drivingRadiusMeters
                        + ROAD_BORDER_BLOCKS
                        * METERS_PER_BLOCK;

        if (distanceMeters <= drivingRadiusMeters) {

            return Material.POLISHED_BLACKSTONE_BRICKS;
        }

        if (distanceMeters <= borderRadiusMeters) {

            return Material.STONE_BRICKS;
        }

        return null;
    }

    /*
     * =========================================================
     * WORLD HEIGHT SAFETY
     * =========================================================
     *
     * Uses the world's available upper generation boundary.
     *
     * Does not change Minecraft's build height.
     */

    private static int clampTerrainHeight(
            ChunkData chunkData,
            int height
    ) {

        return Math.max(
                1,
                Math.min(
                        height,
                        chunkData.getMaxHeight() - 1
                )
        );
    }
}
