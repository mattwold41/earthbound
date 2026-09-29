package com.earthbound;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.generator.ChunkGenerator.ChunkData;

public class EarthResidentialGenerator {

    /*
     * ============================================================
     * EARTHBOUND - GUEMES RESIDENTIAL AREA A
     * ============================================================
     *
     * VERSION 6 - REUSABLE HOUSE SYSTEM
     *
     * - Preserves natural USGS terrain.
     * - Keeps the approved neighborhood road.
     * - Keeps sidewalks.
     * - Keeps all 8 property boundaries.
     * - Automatically generates a home on all 8 properties.
     * - Homes on both sides face the neighborhood road.
     * - Preserves the approved pitched-roof/gable design.
     * - Adds basic finished interiors.
     * - Adds simple house-to-house visual variation.
     * - Uses foundations instead of flattening entire properties.
     * - Approved Area A behavior remains unchanged.
     * - Homes can now use another east-west road center.
     *
     * IMPORTANT:
     * This class does NOT modify the General Store.
     *
     * Actual NPC residents will be handled later by the
     * EarthBound NPC/property system.
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

    private static final int HOUSE_WIDTH = 11;
    private static final int HOUSE_DEPTH = 13;

    private static final Material ROAD_MATERIAL =
            Material.GRAY_CONCRETE;

    private static final Material SIDEWALK_MATERIAL =
            Material.SMOOTH_STONE;

    private static final Material PLOT_MARKER =
            Material.YELLOW_CONCRETE;

    private EarthResidentialGenerator() {
    }

    /*
     * ============================================================
     * MAIN AREA A GENERATION
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

        generateAllHomes(
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

            for (int worldZ = CENTER_Z - ROAD_HALF_WIDTH;
                 worldZ <= CENTER_Z + ROAD_HALF_WIDTH;
                 worldZ++) {

                placeNaturalSurfaceBlock(
                        chunkData,
                        chunkMinX,
                        chunkMinZ,
                        worldX,
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
     * PROPERTY PLOTS
     * ============================================================
     */

    private static void generateHousePlots(
            ChunkData chunkData,
            int chunkMinX,
            int chunkMinZ
    ) {

        int totalWidth =
                (PLOTS_PER_SIDE * PLOT_WIDTH)
                        + ((PLOTS_PER_SIDE - 1) * PLOT_SPACING);

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

            markPlot(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    minX,
                    maxX,
                    northPlotMinZ,
                    northPlotMinZ + PLOT_DEPTH
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
     * GENERATE ALL 8 APPROVED AREA A HOMES
     * ============================================================
     */

    private static void generateAllHomes(
            ChunkData chunkData,
            int chunkMinX,
            int chunkMinZ
    ) {

        int totalWidth =
                (PLOTS_PER_SIDE * PLOT_WIDTH)
                        + ((PLOTS_PER_SIDE - 1) * PLOT_SPACING);

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

            int plotMinX =
                    firstPlotX
                            + plot
                            * (PLOT_WIDTH + PLOT_SPACING);

            /*
             * NORTH SIDE
             *
             * Road is SOUTH of these houses.
             */
            generateHome(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    plotMinX,
                    northPlotMinZ,
                    true,
                    plot
            );

            /*
             * SOUTH SIDE
             *
             * Road is NORTH of these houses.
             */
            generateHome(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    plotMinX,
                    southPlotMinZ,
                    false,
                    plot + PLOTS_PER_SIDE
            );
        }
    }

    /*
     * ============================================================
     * APPROVED AREA A HOME ENTRY POINT
     * ============================================================
     *
     * This keeps the original public method intact.
     *
     * Existing Area A calls automatically use the approved
     * Area A road center.
     */

    public static void generateHome(
            ChunkData chunkData,
            int chunkMinX,
            int chunkMinZ,
            int plotMinX,
            int plotMinZ,
            boolean northSide,
            int houseNumber
    ) {

        generateHome(
                chunkData,
                chunkMinX,
                chunkMinZ,
                plotMinX,
                plotMinZ,
                northSide,
                houseNumber,
                CENTER_Z
        );
    }

    /*
     * ============================================================
     * REUSABLE GUEMES HOME ENTRY POINT
     * ============================================================
     *
     * This version allows another east-west neighborhood road
     * to provide its own center Z.
     *
     * This does NOT yet rotate houses for north-south roads.
     * That will be handled separately when we add road-direction
     * support.
     */

    public static void generateHome(
            ChunkData chunkData,
            int chunkMinX,
            int chunkMinZ,
            int plotMinX,
            int plotMinZ,
            boolean northSide,
            int houseNumber,
            int roadCenterZ
    ) {

        int houseMinX =
                plotMinX
                        + ((PLOT_WIDTH - HOUSE_WIDTH) / 2);

        int houseMaxX =
                houseMinX + HOUSE_WIDTH - 1;

        int houseMinZ;
        int houseMaxZ;

        if (northSide) {

            /*
             * Front faces SOUTH toward road.
             */
            houseMinZ =
                    plotMinZ + 5;

            houseMaxZ =
                    houseMinZ + HOUSE_DEPTH - 1;

        } else {

            /*
             * Front faces NORTH toward road.
             */
            int plotMaxZ =
                    plotMinZ + PLOT_DEPTH;

            houseMaxZ =
                    plotMaxZ - 5;

            houseMinZ =
                    houseMaxZ - HOUSE_DEPTH + 1;
        }

        int centerX =
                (houseMinX + houseMaxX) / 2;

        int centerZ =
                (houseMinZ + houseMaxZ) / 2;

        int groundY =
                getNaturalSurfaceHeight(
                        centerX,
                        centerZ
                );

        Material wallMaterial =
                getWallMaterial(houseNumber);

        Material roofMaterial =
                getRoofMaterial(houseNumber);

        Material floorMaterial =
                getFloorMaterial(houseNumber);

        /*
         * ========================================================
         * FOUNDATION + INTERIOR CLEARING
         * ========================================================
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
                 * Clear only the actual building footprint.
                 */
                for (int y = groundY + 1;
                     y <= groundY + 10;
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

                setWorldBlock(
                        chunkData,
                        chunkMinX,
                        chunkMinZ,
                        worldX,
                        groundY + 1,
                        worldZ,
                        floorMaterial
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

                if (!(westWall
                        || eastWall
                        || southWall
                        || northWall)) {

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
                            wallMaterial
                    );
                }

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
         * GABLE WALLS
         * ========================================================
         */

        for (int worldX = houseMinX;
             worldX <= houseMaxX;
             worldX++) {

            int distanceFromSide =
                    Math.min(
                            worldX - houseMinX,
                            houseMaxX - worldX
                    );

            int gableTopY =
                    groundY + 5 + distanceFromSide;

            for (int y = groundY + 6;
                 y <= gableTopY;
                 y++) {

                setWorldBlock(
                        chunkData,
                        chunkMinX,
                        chunkMinZ,
                        worldX,
                        y,
                        houseMinZ,
                        wallMaterial
                );

                setWorldBlock(
                        chunkData,
                        chunkMinX,
                        chunkMinZ,
                        worldX,
                        y,
                        houseMaxZ,
                        wallMaterial
                );
            }
        }

        /*
         * ========================================================
         * WINDOWS
         * ========================================================
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

        placeWindow(
                chunkData,
                chunkMinX,
                chunkMinZ,
                houseMinX,
                groundY + 3,
                centerZ
        );

        placeWindow(
                chunkData,
                chunkMinX,
                chunkMinZ,
                houseMaxX,
                groundY + 3,
                centerZ
        );

        /*
         * ========================================================
         * FRONT DOOR
         * ========================================================
         */

        int doorX =
                centerX;

        int frontZ =
                northSide
                        ? houseMinZ
                        : houseMaxZ;

        setWorldBlock(
                chunkData,
                chunkMinX,
                chunkMinZ,
                doorX,
                groundY + 2,
                frontZ,
                Material.AIR
        );

        setWorldBlock(
                chunkData,
                chunkMinX,
                chunkMinZ,
                doorX,
                groundY + 3,
                frontZ,
                Material.AIR
        );

        placeDoor(
                chunkData,
                chunkMinX,
                chunkMinZ,
                doorX,
                groundY + 2,
                frontZ,
                northSide
                        ? BlockFace.SOUTH
                        : BlockFace.NORTH
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
                            Math.abs(
                                    worldX
                                            - (houseMinX - 1)
                            ),
                            Math.abs(
                                    worldX
                                            - (houseMaxX + 1)
                            )
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
                        roofMaterial
                );
            }
        }

        /*
         * ========================================================
         * FRONT PORCH + WALKWAY
         * ========================================================
         */

        int porchZ =
                northSide
                        ? houseMinZ - 1
                        : houseMaxZ + 1;

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
                    floorMaterial
            );
        }

        generateWalkway(
                chunkData,
                chunkMinX,
                chunkMinZ,
                doorX,
                porchZ,
                northSide,
                roadCenterZ
        );

        /*
         * ========================================================
         * BASIC FINISHED INTERIOR
         * ========================================================
         */

        generateInterior(
                chunkData,
                chunkMinX,
                chunkMinZ,
                houseMinX,
                houseMaxX,
                houseMinZ,
                houseMaxZ,
                groundY,
                houseNumber
        );
    }

    /*
     * ============================================================
     * BASIC INTERIOR
     * ============================================================
     */

    private static void generateInterior(
            ChunkData chunkData,
            int chunkMinX,
            int chunkMinZ,
            int minX,
            int maxX,
            int minZ,
            int maxZ,
            int groundY,
            int houseNumber
    ) {

        setWorldBlock(
                chunkData,
                chunkMinX,
                chunkMinZ,
                minX + 1,
                groundY + 2,
                minZ + 2,
                Material.CRAFTING_TABLE
        );

        setWorldBlock(
                chunkData,
                chunkMinX,
                chunkMinZ,
                minX + 1,
                groundY + 2,
                minZ + 3,
                Material.FURNACE
        );

        setWorldBlock(
                chunkData,
                chunkMinX,
                chunkMinZ,
                minX + 1,
                groundY + 2,
                minZ + 4,
                Material.BARREL
        );

        setWorldBlock(
                chunkData,
                chunkMinX,
                chunkMinZ,
                maxX - 1,
                groundY + 2,
                maxZ - 2,
                Material.BARREL
        );

        setWorldBlock(
                chunkData,
                chunkMinX,
                chunkMinZ,
                maxX - 1,
                groundY + 2,
                minZ + 3,
                Material.BOOKSHELF
        );

        setWorldBlock(
                chunkData,
                chunkMinX,
                chunkMinZ,
                maxX - 1,
                groundY + 3,
                minZ + 3,
                Material.BOOKSHELF
        );

        int tableX =
                (minX + maxX) / 2;

        int tableZ =
                (minZ + maxZ) / 2;

        setWorldBlock(
                chunkData,
                chunkMinX,
                chunkMinZ,
                tableX,
                groundY + 2,
                tableZ,
                Material.SPRUCE_FENCE
        );

        setWorldBlock(
                chunkData,
                chunkMinX,
                chunkMinZ,
                tableX,
                groundY + 3,
                tableZ,
                Material.SPRUCE_PRESSURE_PLATE
        );

        setWorldBlock(
                chunkData,
                chunkMinX,
                chunkMinZ,
                tableX - 1,
                groundY + 2,
                tableZ,
                Material.SPRUCE_STAIRS
        );

        setWorldBlock(
                chunkData,
                chunkMinX,
                chunkMinZ,
                tableX + 1,
                groundY + 2,
                tableZ,
                Material.SPRUCE_STAIRS
        );

        setWorldBlock(
                chunkData,
                chunkMinX,
                chunkMinZ,
                minX + 2,
                groundY + 2,
                maxZ - 2,
                Material.RED_CARPET
        );

        setWorldBlock(
                chunkData,
                chunkMinX,
                chunkMinZ,
                minX + 3,
                groundY + 2,
                maxZ - 2,
                Material.RED_CARPET
        );

        setWorldBlock(
                chunkData,
                chunkMinX,
                chunkMinZ,
                minX + 4,
                groundY + 2,
                maxZ - 2,
                Material.RED_CARPET
        );

        setWorldBlock(
                chunkData,
                chunkMinX,
                chunkMinZ,
                minX + 2,
                groundY + 5,
                minZ + 2,
                Material.GLOWSTONE
        );

        setWorldBlock(
                chunkData,
                chunkMinX,
                chunkMinZ,
                maxX - 2,
                groundY + 5,
                maxZ - 2,
                Material.GLOWSTONE
        );

        if (houseNumber % 2 == 0) {

            setWorldBlock(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    maxX - 2,
                    groundY + 2,
                    minZ + 2,
                    Material.FLOWER_POT
            );

        } else {

            setWorldBlock(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    maxX - 2,
                    groundY + 2,
                    minZ + 2,
                    Material.BOOKSHELF
            );
        }
    }

    /*
     * ============================================================
     * WALKWAY
     * ============================================================
     */

    private static void generateWalkway(
            ChunkData chunkData,
            int chunkMinX,
            int chunkMinZ,
            int doorX,
            int porchZ,
            boolean northSide,
            int roadCenterZ
    ) {

        if (northSide) {

            int sidewalkZ =
                    roadCenterZ + ROAD_HALF_WIDTH + 1;

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

        } else {

            int sidewalkZ =
                    roadCenterZ - ROAD_HALF_WIDTH - 1;

            for (int z = sidewalkZ - 1;
                 z > porchZ;
                 z--) {

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
    }

    /*
     * ============================================================
     * DOOR HELPER
     * ============================================================
     */

    private static void placeDoor(
            ChunkData chunkData,
            int chunkMinX,
            int chunkMinZ,
            int worldX,
            int worldY,
            int worldZ,
            BlockFace facing
    ) {

        if (!belongsToChunk(
                worldX,
                worldZ,
                chunkMinX,
                chunkMinZ
        )) {
            return;
        }

        Door bottom =
                (Door) Bukkit.createBlockData(
                        Material.SPRUCE_DOOR
                );

        bottom.setFacing(facing);
        bottom.setHalf(Bisected.Half.BOTTOM);

        Door top =
                (Door) Bukkit.createBlockData(
                        Material.SPRUCE_DOOR
                );

        top.setFacing(facing);
        top.setHalf(Bisected.Half.TOP);

        setWorldBlockData(
                chunkData,
                chunkMinX,
                chunkMinZ,
                worldX,
                worldY,
                worldZ,
                bottom
        );

        setWorldBlockData(
                chunkData,
                chunkMinX,
                chunkMinZ,
                worldX,
                worldY + 1,
                worldZ,
                top
        );
    }

    /*
     * ============================================================
     * HOUSE MATERIAL VARIATION
     * ============================================================
     */

    private static Material getWallMaterial(
            int houseNumber
    ) {

        return switch (houseNumber % 4) {

            case 1 ->
                    Material.LIGHT_GRAY_TERRACOTTA;

            case 2 ->
                    Material.WHITE_CONCRETE;

            case 3 ->
                    Material.SANDSTONE;

            default ->
                    Material.WHITE_TERRACOTTA;
        };
    }

    private static Material getRoofMaterial(
            int houseNumber
    ) {

        return switch (houseNumber % 3) {

            case 1 ->
                    Material.SPRUCE_PLANKS;

            case 2 ->
                    Material.DARK_OAK_PLANKS;

            default ->
                    Material.DARK_OAK_PLANKS;
        };
    }

    private static Material getFloorMaterial(
            int houseNumber
    ) {

        return switch (houseNumber % 3) {

            case 1 ->
                    Material.OAK_PLANKS;

            case 2 ->
                    Material.BIRCH_PLANKS;

            default ->
                    Material.SPRUCE_PLANKS;
        };
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
     * MATERIAL BLOCK HELPER
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

    /*
     * ============================================================
     * BLOCK DATA HELPER
     * ============================================================
     */

    private static void setWorldBlockData(
            ChunkData chunkData,
            int chunkMinX,
            int chunkMinZ,
            int worldX,
            int y,
            int worldZ,
            org.bukkit.block.data.BlockData blockData
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
                blockData
        );
    }
}
