package com.earthbound;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator.ChunkData;

public class EarthResidentialGenerator {

    /*
     * ============================================================
     * EARTHBOUND - GUEMES RESIDENTIAL TEST BLOCK
     * ============================================================
     *
     * Area A
     *
     * VERSION 4:
     * - Keeps existing natural USGS terrain.
     * - Keeps neighborhood road and sidewalks.
     * - Keeps all 8 test property markers.
     * - Adds ONE test starter house.
     * - House uses a small foundation instead of grading terrain.
     *
     * IMPORTANT:
     * This class does NOT modify the General Store.
     * ============================================================
     */

    private static final int CENTER_X = -900;
    private static final int CENTER_Z = -150;

    private static final int DISTRICT_HALF_WIDTH = 45;
    private static final int DISTRICT_HALF_LENGTH = 55;

    private static final int ROAD_HALF_WIDTH = 3;

    private static final int PLOT_WIDTH = 16;
    private static final int PLOT_DEPTH = 20;
    private static final int PLOT_SPACING = 4;
    private static final int PLOTS_PER_SIDE = 4;

    private static final Material ROAD_MATERIAL =
            Material.GRAY_CONCRETE;

    private static final Material SIDEWALK_MATERIAL =
            Material.SMOOTH_STONE;

    private static final Material PLOT_MARKER =
            Material.YELLOW_CONCRETE;

    /*
     * ============================================================
     * FIRST TEST HOUSE
     * ============================================================
     *
     * Uses the first NORTH property.
     *
     * The road is south of this house, so the front door faces
     * SOUTH toward the neighborhood road.
     *
     * House footprint:
     * 11 blocks wide
     * 13 blocks deep
     */

    private static final int TEST_HOUSE_WIDTH = 11;
    private static final int TEST_HOUSE_DEPTH = 13;

    private EarthResidentialGenerator() {
    }

    /*
     * ============================================================
     * MAIN GENERATION METHOD
     * ============================================================
     */

    public static void generate(
            ChunkData chunkData,
            int chunkX,
            int chunkZ
    ) {

        int chunkMinX = chunkX << 4;
        int chunkMinZ = chunkZ << 4;

        int chunkMaxX = chunkMinX + 15;
        int chunkMaxZ = chunkMinZ + 15;

        if (chunkMaxX < CENTER_X - DISTRICT_HALF_WIDTH
                || chunkMinX > CENTER_X + DISTRICT_HALF_WIDTH
                || chunkMaxZ < CENTER_Z - DISTRICT_HALF_LENGTH
                || chunkMinZ > CENTER_Z + DISTRICT_HALF_LENGTH) {

            return;
        }

        generateNeighborhoodRoad(
                chunkData,
                chunkMinX,
                chunkMinZ
        );

        generateHousePlots(
                chunkData,
                chunkMinX,
                chunkMinZ
        );

        /*
         * Generate ONE house only.
         */
        generateFirstTestHouse(
                chunkData,
                chunkMinX,
                chunkMinZ
        );
    }

    /*
     * ============================================================
     * NEIGHBORHOOD ROAD
     * ============================================================
     */

    private static void generateNeighborhoodRoad(
            ChunkData chunkData,
            int chunkMinX,
            int chunkMinZ
    ) {

        int roadStartX =
                CENTER_X - DISTRICT_HALF_WIDTH;

        int roadEndX =
                CENTER_X + DISTRICT_HALF_WIDTH;

        for (int worldX = roadStartX;
             worldX <= roadEndX;
             worldX++) {

            for (int worldZ =
                 CENTER_Z - ROAD_HALF_WIDTH;
                 worldZ <= CENTER_Z + ROAD_HALF_WIDTH;
                 worldZ++) {

                if (!belongsToChunk(
                        worldX,
                        worldZ,
                        chunkMinX,
                        chunkMinZ
                )) {
                    continue;
                }

                int surfaceY =
                        getNaturalSurfaceHeight(
                                worldX,
                                worldZ
                        );

                setWorldBlock(
                        chunkData,
                        chunkMinX,
                        chunkMinZ,
                        worldX,
                        surfaceY,
                        worldZ,
                        ROAD_MATERIAL
                );
            }
        }

        int northSidewalkZ =
                CENTER_Z + ROAD_HALF_WIDTH + 1;

        int southSidewalkZ =
                CENTER_Z - ROAD_HALF_WIDTH - 1;

        for (int worldX = roadStartX;
             worldX <= roadEndX;
             worldX++) {

            placeNaturalSurfaceBlock(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    worldX,
                    northSidewalkZ,
                    SIDEWALK_MATERIAL
            );

            placeNaturalSurfaceBlock(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    worldX,
                    southSidewalkZ,
                    SIDEWALK_MATERIAL
            );
        }
    }

    /*
     * ============================================================
     * HOUSE PLOTS
     * ============================================================
     */

    private static void generateHousePlots(
            ChunkData chunkData,
            int chunkMinX,
            int chunkMinZ
    ) {

        int totalWidth =
                (PLOTS_PER_SIDE * PLOT_WIDTH)
                        + ((PLOTS_PER_SIDE - 1)
                        * PLOT_SPACING);

        int firstPlotX =
                CENTER_X - (totalWidth / 2);

        int northPlotMinZ =
                CENTER_Z + ROAD_HALF_WIDTH + 5;

        int southPlotMaxZ =
                CENTER_Z - ROAD_HALF_WIDTH - 5;

        int southPlotMinZ =
                southPlotMaxZ - PLOT_DEPTH;

        for (int plot = 0;
             plot < PLOTS_PER_SIDE;
             plot++) {

            int minX =
                    firstPlotX
                            + plot
                            * (PLOT_WIDTH + PLOT_SPACING);

            int maxX =
                    minX + PLOT_WIDTH;

            int northMinZ =
                    northPlotMinZ;

            int northMaxZ =
                    northMinZ + PLOT_DEPTH;

            markPlot(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    minX,
                    maxX,
                    northMinZ,
                    northMaxZ
            );

            markPlot(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    minX,
                    maxX,
                    southPlotMinZ,
                    southPlotMaxZ
            );
        }
    }

    /*
     * ============================================================
     * FIRST TEST HOUSE
     * ============================================================
     */

    private static void generateFirstTestHouse(
            ChunkData chunkData,
            int chunkMinX,
            int chunkMinZ
    ) {

        int totalWidth =
                (PLOTS_PER_SIDE * PLOT_WIDTH)
                        + ((PLOTS_PER_SIDE - 1)
                        * PLOT_SPACING);

        int firstPlotX =
                CENTER_X - (totalWidth / 2);

        int plotMinX = firstPlotX;

        int plotMinZ =
                CENTER_Z + ROAD_HALF_WIDTH + 5;

        /*
         * Center the house inside the first north plot.
         */
        int houseMinX =
                plotMinX
                        + ((PLOT_WIDTH - TEST_HOUSE_WIDTH) / 2);

        int houseMaxX =
                houseMinX + TEST_HOUSE_WIDTH - 1;

        /*
         * Leave a small front yard between the road and house.
         */
        int houseMinZ =
                plotMinZ + 5;

        int houseMaxZ =
                houseMinZ + TEST_HOUSE_DEPTH - 1;

        /*
         * Use the terrain height at the center of the house.
         * We do NOT flatten the surrounding property.
         */
        int centerHouseX =
                (houseMinX + houseMaxX) / 2;

        int centerHouseZ =
                (houseMinZ + houseMaxZ) / 2;

        int groundY =
                getNaturalSurfaceHeight(
                        centerHouseX,
                        centerHouseZ
                );

        /*
         * ========================================================
         * FOUNDATION
         * ========================================================
         *
         * Stone foundation reaches downward into the natural land.
         * This lets the house remain level without grading the plot.
         */

        for (int worldX = houseMinX;
             worldX <= houseMaxX;
             worldX++) {

            for (int worldZ = houseMinZ;
                 worldZ <= houseMaxZ;
                 worldZ++) {

                if (!belongsToChunk(
                        worldX,
                        worldZ,
                        chunkMinX,
                        chunkMinZ
                )) {
                    continue;
                }

                int naturalY =
                        getNaturalSurfaceHeight(
                                worldX,
                                worldZ
                        );

                int foundationBottom =
                        Math.min(
                                naturalY,
                                groundY
                        );

                for (int y = foundationBottom;
                     y <= groundY;
                     y++) {

                    setWorldBlock(
                            chunkData,
                            chunkMinX,
                            chunkMinZ,
                            worldX,
                            y,
                            worldZ,
                            Material.STONE_BRICKS
                    );
                }

                /*
                 * Clear space above the house foundation.
                 */
                for (int y = groundY + 1;
                     y <= groundY + 9;
                     y++) {

                    setWorldBlock(
                            chunkData,
                            chunkMinX,
                            chunkMinZ,
                            worldX,
                            y,
                            worldZ,
                            Material.AIR
                    );
                }

                /*
                 * Interior floor.
                 */
                setWorldBlock(
                        chunkData,
                        chunkMinX,
                        chunkMinZ,
                        worldX,
                        groundY + 1,
                        worldZ,
                        Material.SPRUCE_PLANKS
                );
            }
        }

        /*
         * ========================================================
         * EXTERIOR WALLS
         * ========================================================
         */

        for (int worldX = houseMinX;
             worldX <= houseMaxX;
             worldX++) {

            for (int worldZ = houseMinZ;
                 worldZ <= houseMaxZ;
                 worldZ++) {

                boolean westWall =
                        worldX == houseMinX;

                boolean eastWall =
                        worldX == houseMaxX;

                boolean southWall =
                        worldZ == houseMinZ;

                boolean northWall =
                        worldZ == houseMaxZ;

                boolean exterior =
                        westWall
                                || eastWall
                                || southWall
                                || northWall;

                if (!exterior) {
                    continue;
                }

                for (int y = groundY + 2;
                     y <= groundY + 5;
                     y++) {

                    setWorldBlock(
                            chunkData,
                            chunkMinX,
                            chunkMinZ,
                            worldX,
                            y,
                            worldZ,
                            Material.WHITE_TERRACOTTA
                    );
                }

                /*
                 * Corner timber posts.
                 */
                boolean corner =
                        (westWall || eastWall)
                                && (southWall || northWall);

                if (corner) {

                    for (int y = groundY + 2;
                         y <= groundY + 5;
                         y++) {

                        setWorldBlock(
                                chunkData,
                                chunkMinX,
                                chunkMinZ,
                                worldX,
                                y,
                                worldZ,
                                Material.STRIPPED_SPRUCE_LOG
                        );
                    }
                }
            }
        }

        /*
         * ========================================================
         * FRONT DOOR - SOUTH / ROAD SIDE
         * ========================================================
         */

        int doorX =
                (houseMinX + houseMaxX) / 2;

        setWorldBlock(
                chunkData,
                chunkMinX,
                chunkMinZ,
                doorX,
                groundY + 2,
                houseMinZ,
                Material.AIR
        );

        setWorldBlock(
                chunkData,
                chunkMinX,
                chunkMinZ,
                doorX,
                groundY + 3,
                houseMinZ,
                Material.AIR
        );

        /*
         * ========================================================
         * WINDOWS
         * ========================================================
         */

        /*
         * Front windows.
         */
        placeWindow(
                chunkData,
                chunkMinX,
                chunkMinZ,
                houseMinX + 2,
                groundY + 3,
                houseMinZ
        );

        placeWindow(
                chunkData,
                chunkMinX,
                chunkMinZ,
                houseMaxX - 2,
                groundY + 3,
                houseMinZ
        );

        /*
         * Rear windows.
         */
        placeWindow(
                chunkData,
                chunkMinX,
                chunkMinZ,
                houseMinX + 3,
                groundY + 3,
                houseMaxZ
        );

        placeWindow(
                chunkData,
                chunkMinX,
                chunkMinZ,
                houseMaxX - 3,
                groundY + 3,
                houseMaxZ
        );

        /*
         * Side windows.
         */
        placeWindow(
                chunkData,
                chunkMinX,
                chunkMinZ,
                houseMinX,
                groundY + 3,
                houseMinZ + 5
        );

        placeWindow(
                chunkData,
                chunkMinX,
                chunkMinZ,
                houseMaxX,
                groundY + 3,
                houseMinZ + 5
        );

        /*
         * ========================================================
         * PITCHED ROOF
         * ========================================================
         */

        for (int worldX = houseMinX - 1;
             worldX <= houseMaxX + 1;
             worldX++) {

            int distanceFromEdge =
                    Math.min(
                            Math.abs(worldX - (houseMinX - 1)),
                            Math.abs(worldX - (houseMaxX + 1))
                    );

            int roofY =
                    groundY + 6 + distanceFromEdge;

            for (int worldZ = houseMinZ - 1;
                 worldZ <= houseMaxZ + 1;
                 worldZ++) {

                setWorldBlock(
                        chunkData,
                        chunkMinX,
                        chunkMinZ,
                        worldX,
                        roofY,
                        worldZ,
                        Material.DARK_OAK_PLANKS
                );
            }
        }

        /*
         * ========================================================
         * FRONT PORCH
         * ========================================================
         */

        int porchZ =
                houseMinZ - 1;

        for (int x = doorX - 1;
             x <= doorX + 1;
             x++) {

            setWorldBlock(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    x,
                    groundY + 1,
                    porchZ,
                    Material.SPRUCE_PLANKS
            );
        }

        /*
         * ========================================================
         * WALKWAY
         * ========================================================
         *
         * Follow natural terrain from the sidewalk toward the porch.
         */

        int sidewalkZ =
                CENTER_Z + ROAD_HALF_WIDTH + 1;

        for (int z = sidewalkZ + 1;
             z < porchZ;
             z++) {

            placeNaturalSurfaceBlock(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    doorX,
                    z,
                    Material.STONE_BRICKS
            );
        }
    }

    /*
     * ============================================================
     * WINDOW HELPER
     * ============================================================
     */

    private static void placeWindow(
            ChunkData chunkData,
            int chunkMinX,
            int chunkMinZ,
            int worldX,
            int worldY,
            int worldZ
    ) {

        setWorldBlock(
                chunkData,
                chunkMinX,
                chunkMinZ,
                worldX,
                worldY,
                worldZ,
                Material.GLASS_PANE
        );

        setWorldBlock(
                chunkData,
                chunkMinX,
                chunkMinZ,
                worldX,
                worldY + 1,
                worldZ,
                Material.GLASS_PANE
        );
    }

    /*
     * ============================================================
     * PROPERTY MARKERS
     * ============================================================
     */

    private static void markPlot(
            ChunkData chunkData,
            int chunkMinX,
            int chunkMinZ,
            int minX,
            int maxX,
            int minZ,
            int maxZ
    ) {

        for (int x = minX;
             x <= maxX;
             x++) {

            placeNaturalSurfaceBlock(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    x,
                    minZ,
                    PLOT_MARKER
            );

            placeNaturalSurfaceBlock(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    x,
                    maxZ,
                    PLOT_MARKER
            );
        }

        for (int z = minZ;
             z <= maxZ;
             z++) {

            placeNaturalSurfaceBlock(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    minX,
                    z,
                    PLOT_MARKER
            );

            placeNaturalSurfaceBlock(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    maxX,
                    z,
                    PLOT_MARKER
            );
        }
    }

    /*
     * ============================================================
     * NATURAL USGS TERRAIN HEIGHT
     * ============================================================
     */

    private static int getNaturalSurfaceHeight(
            int worldX,
            int worldZ
    ) {

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

        double elevationMeters =
                EarthTerrainLoader.getGuemesElevation(
                        latitude,
                        longitude
                );

        return EarthElevation.getMinecraftHeight(
                elevationMeters
        );
    }

    /*
     * ============================================================
     * NATURAL SURFACE BLOCK HELPER
     * ============================================================
     */

    private static void placeNaturalSurfaceBlock(
            ChunkData chunkData,
            int chunkMinX,
            int chunkMinZ,
            int worldX,
            int worldZ,
            Material material
    ) {

        if (!belongsToChunk(
                worldX,
                worldZ,
                chunkMinX,
                chunkMinZ
        )) {
            return;
        }

        int surfaceY =
                getNaturalSurfaceHeight(
                        worldX,
                        worldZ
                );

        setWorldBlock(
                chunkData,
                chunkMinX,
                chunkMinZ,
                worldX,
                surfaceY,
                worldZ,
                material
        );
    }

    /*
     * ============================================================
     * CHUNK CHECK
     * ============================================================
     */

    private static boolean belongsToChunk(
            int worldX,
            int worldZ,
            int chunkMinX,
            int chunkMinZ
    ) {

        return worldX >= chunkMinX
                && worldX <= chunkMinX + 15
                && worldZ >= chunkMinZ
                && worldZ <= chunkMinZ + 15;
    }

    /*
     * ============================================================
     * WORLD BLOCK -> CHUNK BLOCK
     * ============================================================
     */

    private static void setWorldBlock(
            ChunkData chunkData,
            int chunkMinX,
            int chunkMinZ,
            int worldX,
            int y,
            int worldZ,
            Material material
    ) {

        if (y < chunkData.getMinHeight()
                || y >= chunkData.getMaxHeight()) {

            return;
        }

        int localX =
                worldX - chunkMinX;

        int localZ =
                worldZ - chunkMinZ;

        if (localX < 0
                || localX > 15
                || localZ < 0
                || localZ > 15) {

            return;
        }

        chunkData.setBlock(
                localX,
                y,
                localZ,
                material
        );
    }
}
