package com.earthbound;

import com.earthbound.terrain.EarthTerrainLoader;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator.ChunkData;

public class EarthBuildingGenerator {

    private EarthBuildingGenerator() {
    }

    /*
     * =====================================================
     * GUEMES ISLAND GENERAL STORE
     * =====================================================
     *
     * Approved coordinates and dimensions.
     *
     * Front: WEST (negative X)
     * Rear: EAST (positive X)
     *
     * Ground height: Y=65
     * Terrain blending: 32 blocks
     *
     * 1 Minecraft block = 2 real-world meters.
     */

    public static final double GUEMES_STORE_LATITUDE =
            48.529460;

    public static final double GUEMES_STORE_LONGITUDE =
            -122.624110;

    private static final int GUEMES_STORE_SIZE_X = 16;
    private static final int GUEMES_STORE_SIZE_Z = 24;

    private static final int FOUNDATION_MARGIN = 2;
    private static final int STORE_BLEND_MARGIN = 32;

    private static final int GUEMES_STORE_GROUND_Y = 65;

    /*
     * =====================================================
     * STORE COORDINATES
     * =====================================================
     */

    public static int getGuemesStoreX() {

        return EarthCoordinates.longitudeToMinecraftX(
                GUEMES_STORE_LONGITUDE
        );
    }

    public static int getGuemesStoreZ() {

        return EarthCoordinates.latitudeToMinecraftZ(
                GUEMES_STORE_LATITUDE
        );
    }

    public static int getGuemesStoreGroundY() {

        return GUEMES_STORE_GROUND_Y;
    }

    public static Double getGuemesStoreElevation() {

        return EarthTerrainLoader.getGuemesElevation(
                GUEMES_STORE_LATITUDE,
                GUEMES_STORE_LONGITUDE
        );
    }

    /*
     * =====================================================
     * BUILDING BOUNDARIES
     * =====================================================
     */

    private static int minX() {

        return getGuemesStoreX()
                - GUEMES_STORE_SIZE_X / 2;
    }

    private static int maxX() {

        return getGuemesStoreX()
                + GUEMES_STORE_SIZE_X / 2;
    }

    private static int minZ() {

        return getGuemesStoreZ()
                - GUEMES_STORE_SIZE_Z / 2;
    }

    private static int maxZ() {

        return getGuemesStoreZ()
                + GUEMES_STORE_SIZE_Z / 2;
    }

    /*
     * Actual store footprint.
     */

    public static boolean isInsideGuemesStore(
            int worldX,
            int worldZ) {

        return worldX >= minX()
                && worldX <= maxX()
                && worldZ >= minZ()
                && worldZ <= maxZ();
    }

    /*
     * Foundation immediately around the building.
     */

    public static boolean isInsideGuemesStoreFoundation(
            int worldX,
            int worldZ) {

        return worldX >= minX() - FOUNDATION_MARGIN
                && worldX <= maxX() + FOUNDATION_MARGIN
                && worldZ >= minZ() - FOUNDATION_MARGIN
                && worldZ <= maxZ() + FOUNDATION_MARGIN;
    }

    /*
     * Includes the building, foundation and WEST porch.
     *
     * This method is required by EarthGenerator.
     */

    public static boolean isInsideGuemesStoreSite(
            int worldX,
            int worldZ) {

        return worldX >= minX() - 5
                && worldX <= maxX() + FOUNDATION_MARGIN
                && worldZ >= minZ() - FOUNDATION_MARGIN
                && worldZ <= maxZ() + FOUNDATION_MARGIN;
    }

    /*
     * Terrain-blending region.
     */

    public static boolean isInsideGuemesStoreBlendArea(
            int worldX,
            int worldZ) {

        return worldX >= minX() - 5
                        - STORE_BLEND_MARGIN
                && worldX <= maxX()
                        + FOUNDATION_MARGIN
                        + STORE_BLEND_MARGIN
                && worldZ >= minZ()
                        - FOUNDATION_MARGIN
                        - STORE_BLEND_MARGIN
                && worldZ <= maxZ()
                        + FOUNDATION_MARGIN
                        + STORE_BLEND_MARGIN;
    }

    /*
     * Distance from the building footprint.
     */

    public static double getDistanceFromGuemesStore(
            int worldX,
            int worldZ) {

        int dx = Math.max(
                Math.max(
                        minX() - worldX,
                        worldX - maxX()
                ),
                0
        );

        int dz = Math.max(
                Math.max(
                        minZ() - worldZ,
                        worldZ - maxZ()
                ),
                0
        );

        return Math.hypot(dx, dz);
    }

    /*
     * Distance from the complete store site.
     */

    public static double getDistanceFromStoreSite(
            int worldX,
            int worldZ) {

        int siteMinX = minX() - 5;
        int siteMaxX = maxX() + FOUNDATION_MARGIN;

        int siteMinZ = minZ() - FOUNDATION_MARGIN;
        int siteMaxZ = maxZ() + FOUNDATION_MARGIN;

        int dx = Math.max(
                Math.max(
                        siteMinX - worldX,
                        worldX - siteMaxX
                ),
                0
        );

        int dz = Math.max(
                Math.max(
                        siteMinZ - worldZ,
                        worldZ - siteMaxZ
                ),
                0
        );

        return Math.hypot(dx, dz);
    }

    /*
     * =====================================================
     * TERRAIN BLENDING
     * =====================================================
     *
     * Required by EarthGenerator.
     *
     * Store site is fixed at Y=65.
     *
     * Outside the site, terrain smoothly transitions
     * back to natural USGS elevation over 32 blocks.
     */

    public static int getBlendedTerrainHeight(
            int worldX,
            int worldZ,
            int naturalHeight) {

        if (isInsideGuemesStoreSite(
                worldX,
                worldZ)) {

            return GUEMES_STORE_GROUND_Y;
        }

        double distance =
                getDistanceFromStoreSite(
                        worldX,
                        worldZ
                );

        if (distance >= STORE_BLEND_MARGIN) {

            return naturalHeight;
        }

        double t =
                distance / STORE_BLEND_MARGIN;

        // Smoothstep transition.
        t = t * t * (3.0 - 2.0 * t);

        return (int) Math.round(
                GUEMES_STORE_GROUND_Y
                        + (naturalHeight
                        - GUEMES_STORE_GROUND_Y) * t
        );
    }

    /*
     * =====================================================
     * SAFE CHUNK BLOCK PLACEMENT
     * =====================================================
     *
     * A structure can cross multiple chunks.
     *
     * Only place blocks belonging to the current chunk.
     */

    private static void setBlock(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            int worldX,
            int y,
            int worldZ,
            Material material) {

        int localX =
                worldX - chunkX * 16;

        int localZ =
                worldZ - chunkZ * 16;

        if (localX < 0 || localX >= 16
                || localZ < 0 || localZ >= 16) {

            return;
        }

        if (y < chunk.getMinHeight()
                || y >= chunk.getMaxHeight()) {

            return;
        }

        chunk.setBlock(
                localX,
                y,
                localZ,
                material
        );
    }

    /*
     * =====================================================
     * GENERATE GUEMES GENERAL STORE
     * =====================================================
     */

    public static void generateGuemesStore(
            ChunkData chunk,
            int chunkX,
            int chunkZ) {

        int groundY =
                getGuemesStoreGroundY();

        int storeX =
                getGuemesStoreX();

        int storeZ =
                getGuemesStoreZ();

        int buildingMinX = minX();
        int buildingMaxX = maxX();

        int buildingMinZ = minZ();
        int buildingMaxZ = maxZ();

        /*
         * Skip chunks outside the store and porch.
         */

        int chunkMinX = chunkX * 16;
        int chunkMaxX = chunkMinX + 15;

        int chunkMinZ = chunkZ * 16;
        int chunkMaxZ = chunkMinZ + 15;

        if (chunkMinX > buildingMaxX + 1
                || chunkMaxX < buildingMinX - 4
                || chunkMinZ > buildingMaxZ + 1
                || chunkMaxZ < buildingMinZ - 1) {

            return;
        }

        /*
         * =================================================
         * MAIN BUILDING
         * =================================================
         */

        for (int worldX = buildingMinX;
             worldX <= buildingMaxX;
             worldX++) {

            for (int worldZ = buildingMinZ;
                 worldZ <= buildingMaxZ;
                 worldZ++) {

                /*
                 * Clear space above the store.
                 */

                for (int y = groundY + 1;
                     y <= groundY + 13;
                     y++) {

                    setBlock(
                            chunk,
                            chunkX,
                            chunkZ,
                            worldX,
                            y,
                            worldZ,
                            Material.AIR
                    );
                }

                /*
                 * Foundation.
                 */

                setBlock(
                        chunk,
                        chunkX,
                        chunkZ,
                        worldX,
                        groundY,
                        worldZ,
                        Material.STONE_BRICKS
                );

                /*
                 * Interior floor.
                 */

                setBlock(
                        chunk,
                        chunkX,
                        chunkZ,
                        worldX,
                        groundY + 1,
                        worldZ,
                        Material.SPRUCE_PLANKS
                );

                boolean westWall =
                        worldX == buildingMinX;

                boolean eastWall =
                        worldX == buildingMaxX;

                boolean northWall =
                        worldZ == buildingMinZ;

                boolean southWall =
                        worldZ == buildingMaxZ;

                boolean exterior =
                        westWall
                                || eastWall
                                || northWall
                                || southWall;

                /*
                 * Exterior spruce walls.
                 */

                if (exterior) {

                    for (int y = groundY + 2;
                         y <= groundY + 5;
                         y++) {

                        setBlock(
                                chunk,
                                chunkX,
                                chunkZ,
                                worldX,
                                y,
                                worldZ,
                                Material.SPRUCE_PLANKS
                        );
                    }
                }

                /*
                 * WEST FRONT WINDOWS AND ENTRANCE.
                 */

                if (westWall) {

                    int relativeZ =
                            worldZ - buildingMinZ;

                    boolean firstWindow =
                            relativeZ >= 3
                                    && relativeZ <= 6;

                    boolean secondWindow =
                            relativeZ >= 17
                                    && relativeZ <= 20;

                    if (firstWindow || secondWindow) {

                        for (int y = groundY + 3;
                             y <= groundY + 4;
                             y++) {

                            setBlock(
                                    chunk,
                                    chunkX,
                                    chunkZ,
                                    worldX,
                                    y,
                                    worldZ,
                                    Material.GLASS_PANE
                            );
                        }
                    }

                    /*
                     * Two-block-wide front entrance.
                     */

                    if (relativeZ == 11
                            || relativeZ == 12) {

                        setBlock(
                                chunk,
                                chunkX,
                                chunkZ,
                                worldX,
                                groundY + 2,
                                worldZ,
                                Material.AIR
                        );

                        setBlock(
                                chunk,
                                chunkX,
                                chunkZ,
                                worldX,
                                groundY + 3,
                                worldZ,
                                Material.AIR
                        );
                    }
                }

                /*
                 * North and south side windows.
                 */

                if (northWall || southWall) {

                    int relativeX =
                            worldX - buildingMinX;

                    boolean firstWindow =
                            relativeX >= 3
                                    && relativeX <= 5;

                    boolean secondWindow =
                            relativeX >= 11
                                    && relativeX <= 13;

                    if (firstWindow || secondWindow) {

                        for (int y = groundY + 3;
                             y <= groundY + 4;
                             y++) {

                            setBlock(
                                    chunk,
                                    chunkX,
                                    chunkZ,
                                    worldX,
                                    y,
                                    worldZ,
                                    Material.GLASS_PANE
                            );
                        }
                    }
                }
            }
        }

        /*
         * =================================================
         * MAIN ROOF
         * =================================================
         */

        generateMainRoof(
                chunk,
                chunkX,
                chunkZ,
                groundY,
                storeX,
                storeZ
        );

        /*
         * =================================================
         * RAISED ROOF
         * =================================================
         */

        generateCenterRoof(
                chunk,
                chunkX,
                chunkZ,
                groundY,
                storeX,
                storeZ
        );

        /*
         * =================================================
         * WEST PORCH
         * =================================================
         */

        generateWestPorch(
                chunk,
                chunkX,
                chunkZ,
                groundY,
                storeX,
                storeZ
        );
    }

    /*
     * =====================================================
     * MAIN ROOF
     * =====================================================
     */

    private static void generateMainRoof(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            int groundY,
            int storeX,
            int storeZ) {

        int roofMinX = minX() - 1;
        int roofMaxX = maxX() + 1;

        int roofMinZ = minZ() - 1;
        int roofMaxZ = maxZ() + 1;

        for (int worldX = roofMinX;
             worldX <= roofMaxX;
             worldX++) {

            for (int worldZ = roofMinZ;
                 worldZ <= roofMaxZ;
                 worldZ++) {

                int distance =
                        Math.abs(worldX - storeX);

                int roofY;

                if (distance <= 2) {

                    roofY = groundY + 8;

                } else if (distance <= 5) {

                    roofY = groundY + 7;

                } else {

                    roofY = groundY + 6;
                }

                setBlock(
                        chunk,
                        chunkX,
                        chunkZ,
                        worldX,
                        roofY,
                        worldZ,
                        Material.DARK_OAK_PLANKS
                );
            }
        }
    }

    /*
     * =====================================================
     * RAISED CENTER ROOF
     *
     * Located toward the EAST/rear.
     * =====================================================
     */

    private static void generateCenterRoof(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            int groundY,
            int storeX,
            int storeZ) {

        int centerX = storeX + 2;

        int roofMinZ = storeZ - 5;
        int roofMaxZ = storeZ + 5;

        for (int worldX = centerX - 4;
             worldX <= centerX + 4;
             worldX++) {

            for (int worldZ = roofMinZ;
                 worldZ <= roofMaxZ;
                 worldZ++) {

                int distance =
                        Math.abs(worldX - centerX);

                int roofY;

                if (distance <= 1) {

                    roofY = groundY + 11;

                } else if (distance <= 2) {

                    roofY = groundY + 10;

                } else {

                    roofY = groundY + 9;
                }

                setBlock(
                        chunk,
                        chunkX,
                        chunkZ,
                        worldX,
                        roofY,
                        worldZ,
                        Material.DARK_OAK_PLANKS
                );
            }
        }
    }

    /*
     * =====================================================
     * WEST PORCH
     *
     * Negative X = WEST.
     * =====================================================
     */

    private static void generateWestPorch(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            int groundY,
            int storeX,
            int storeZ) {

        int frontX =
                storeX - GUEMES_STORE_SIZE_X / 2;

        int porchMinX = frontX - 4;
        int porchMaxX = frontX - 1;

        int porchMinZ = storeZ - 7;
        int porchMaxZ = storeZ + 7;

        for (int worldX = porchMinX;
             worldX <= porchMaxX;
             worldX++) {

            for (int worldZ = porchMinZ;
                 worldZ <= porchMaxZ;
                 worldZ++) {

                /*
                 * Clear porch headroom.
                 */

                for (int y = groundY + 2;
                     y <= groundY + 5;
                     y++) {

                    setBlock(
                            chunk,
                            chunkX,
                            chunkZ,
                            worldX,
                            y,
                            worldZ,
                            Material.AIR
                    );
                }

                /*
                 * Porch floor.
                 */

                setBlock(
                        chunk,
                        chunkX,
                        chunkZ,
                        worldX,
                        groundY + 1,
                        worldZ,
                        Material.SPRUCE_PLANKS
                );

                /*
                 * Porch roof.
                 */

                setBlock(
                        chunk,
                        chunkX,
                        chunkZ,
                        worldX,
                        groundY + 6,
                        worldZ,
                        Material.DARK_OAK_PLANKS
                );

                /*
                 * Support posts along the outer edge.
                 */

                boolean outsideEdge =
                        worldX == porchMinX;

                boolean post =
                        worldZ == porchMinZ
                                || worldZ == storeZ - 3
                                || worldZ == storeZ + 3
                                || worldZ == porchMaxZ;

                if (outsideEdge && post) {

                    for (int y = groundY + 2;
                         y <= groundY + 5;
                         y++) {

                        setBlock(
                                chunk,
                                chunkX,
                                chunkZ,
                                worldX,
                                y,
                                worldZ,
                                Material.STRIPPED_SPRUCE_LOG
                        );
                    }
                }
            }
        }
    }

    /*
     * =====================================================
     * DEBUG / LOCATION INFORMATION
     * =====================================================
     */

    public static String getGuemesStoreLocationInfo() {

        return "Guemes General Store: "
                + "lat=" + GUEMES_STORE_LATITUDE
                + ", lon=" + GUEMES_STORE_LONGITUDE
                + ", x=" + getGuemesStoreX()
                + ", y=" + getGuemesStoreGroundY()
                + ", z=" + getGuemesStoreZ();
    }
}
