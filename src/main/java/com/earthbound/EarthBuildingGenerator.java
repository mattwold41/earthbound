package com.earthbound;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator.ChunkData;

public class EarthBuildingGenerator {

    private EarthBuildingGenerator() {
    }

    /*
     * GUEMES ISLAND GENERAL STORE
     */
    public static final double GUEMES_STORE_LATITUDE = 48.529460;
    public static final double GUEMES_STORE_LONGITUDE = -122.624110;

    /*
     * Approved store footprint.
     */
    private static final int GUEMES_STORE_SIZE_X = 16;
    private static final int GUEMES_STORE_SIZE_Z = 24;

    /*
     * Small flat foundation around the building.
     */
    private static final int FOUNDATION_MARGIN = 2;

    /*
     * Approved fixed ground height.
     *
     * DO NOT calculate this from USGS elevation.
     * The General Store sits at Y=65.
     */
    private static final int GUEMES_STORE_GROUND_Y = 65;

    /*
     * Gives EarthGenerator 32 blocks around the store
     * for the gentler terrain transition.
     */
    private static final int STORE_BLEND_MARGIN = 32;

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

    /*
     * Actual store footprint.
     */
    public static boolean isInsideGuemesStore(
            int worldX,
            int worldZ
    ) {

        int storeX = getGuemesStoreX();
        int storeZ = getGuemesStoreZ();

        int halfX =
                GUEMES_STORE_SIZE_X / 2;

        int halfZ =
                GUEMES_STORE_SIZE_Z / 2;

        return worldX >= storeX - halfX
                && worldX <= storeX + halfX
                && worldZ >= storeZ - halfZ
                && worldZ <= storeZ + halfZ;
    }

    /*
     * Flat foundation immediately around the store.
     */
    public static boolean isInsideGuemesStoreFoundation(
            int worldX,
            int worldZ
    ) {

        int storeX = getGuemesStoreX();
        int storeZ = getGuemesStoreZ();

        int halfX =
                GUEMES_STORE_SIZE_X / 2
                        + FOUNDATION_MARGIN;

        int halfZ =
                GUEMES_STORE_SIZE_Z / 2
                        + FOUNDATION_MARGIN;

        return worldX >= storeX - halfX
                && worldX <= storeX + halfX
                && worldZ >= storeZ - halfZ
                && worldZ <= storeZ + halfZ;
    }

    /*
     * Large terrain-blending area.
     *
     * EarthGenerator uses a 32-block transition,
     * so this must also be 32.
     */
    public static boolean isInsideGuemesStoreBlendArea(
            int worldX,
            int worldZ
    ) {

        int storeX = getGuemesStoreX();
        int storeZ = getGuemesStoreZ();

        int halfX =
                GUEMES_STORE_SIZE_X / 2
                        + STORE_BLEND_MARGIN;

        int halfZ =
                GUEMES_STORE_SIZE_Z / 2
                        + STORE_BLEND_MARGIN;

        return worldX >= storeX - halfX
                && worldX <= storeX + halfX
                && worldZ >= storeZ - halfZ
                && worldZ <= storeZ + halfZ;
    }

    /*
     * Real USGS elevation remains available for
     * information/debugging, but it does NOT control
     * the store's Y position.
     */
    public static Double getGuemesStoreElevation() {

        return EarthTerrainLoader.getGuemesElevation(
                GUEMES_STORE_LATITUDE,
                GUEMES_STORE_LONGITUDE
        );
    }

    /*
     * APPROVED STORE HEIGHT.
     */
    public static int getGuemesStoreGroundY() {

        return GUEMES_STORE_GROUND_Y;
    }

    /*
     * Distance from the outside edge of the store.
     *
     * EarthGenerator uses this for terrain blending.
     */
    public static double getDistanceFromGuemesStore(
            int worldX,
            int worldZ
    ) {

        int storeX = getGuemesStoreX();
        int storeZ = getGuemesStoreZ();

        int halfX =
                GUEMES_STORE_SIZE_X / 2;

        int halfZ =
                GUEMES_STORE_SIZE_Z / 2;

        int dx =
                Math.max(
                        Math.abs(worldX - storeX)
                                - halfX,
                        0
                );

        int dz =
                Math.max(
                        Math.abs(worldZ - storeZ)
                                - halfZ,
                        0
                );

        return Math.sqrt(
                (double) dx * dx
                        + (double) dz * dz
        );
    }

    /*
     * Generate the General Store.
     *
     * APPROVED ORIENTATION:
     *
     * Front/entrance = WEST
     * Porch          = WEST
     * Rear           = EAST
     *
     * Do not rotate this again.
     */
    public static void generateGuemesStore(
            ChunkData chunk,
            int chunkX,
            int chunkZ
    ) {

        int groundY =
                getGuemesStoreGroundY();

        int storeX =
                getGuemesStoreX();

        int storeZ =
                getGuemesStoreZ();

        int minX =
                storeX
                        - GUEMES_STORE_SIZE_X / 2;

        int maxX =
                storeX
                        + GUEMES_STORE_SIZE_X / 2;

        int minZ =
                storeZ
                        - GUEMES_STORE_SIZE_Z / 2;

        int maxZ =
                storeZ
                        + GUEMES_STORE_SIZE_Z / 2;

        /*
         * Main building.
         */
        for (int localX = 0;
             localX < 16;
             localX++) {

            for (int localZ = 0;
                 localZ < 16;
                 localZ++) {

                int worldX =
                        chunkX * 16
                                + localX;

                int worldZ =
                        chunkZ * 16
                                + localZ;

                if (worldX < minX
                        || worldX > maxX
                        || worldZ < minZ
                        || worldZ > maxZ) {

                    continue;
                }

                /*
                 * Clear building space.
                 */
                for (int y = groundY + 1;
                     y <= groundY + 13;
                     y++) {

                    chunk.setBlock(
                            localX,
                            y,
                            localZ,
                            Material.AIR
                    );
                }

                /*
                 * Foundation.
                 */
                chunk.setBlock(
                        localX,
                        groundY,
                        localZ,
                        Material.STONE_BRICKS
                );

                /*
                 * Interior floor.
                 */
                chunk.setBlock(
                        localX,
                        groundY + 1,
                        localZ,
                        Material.SPRUCE_PLANKS
                );

                boolean eastWall =
                        worldX == minX;

                boolean westWall =
                        worldX == maxX;

                boolean northWall =
                        worldZ == maxZ;

                boolean southWall =
                        worldZ == minZ;

                boolean exterior =
                        eastWall
                                || westWall
                                || northWall
                                || southWall;

                /*
                 * Exterior walls.
                 */
                if (exterior) {

                    for (int y = groundY + 2;
                         y <= groundY + 5;
                         y++) {

                        chunk.setBlock(
                                localX,
                                y,
                                localZ,
                                Material.SPRUCE_PLANKS
                        );
                    }
                }

                /*
                 * WEST WALL = APPROVED FRONT.
                 */
                if (westWall) {

                    int relativeZ =
                            worldZ - minZ;

                    /*
                     * Front windows.
                     */
                    boolean southWindow =
                            relativeZ >= 3
                                    && relativeZ <= 6;

                    boolean northWindow =
                            relativeZ >= 17
                                    && relativeZ <= 20;

                    if (southWindow
                            || northWindow) {

                        chunk.setBlock(
                                localX,
                                groundY + 3,
                                localZ,
                                Material.GLASS_PANE
                        );

                        chunk.setBlock(
                                localX,
                                groundY + 4,
                                localZ,
                                Material.GLASS_PANE
                        );
                    }

                    /*
                     * Main entrance.
                     */
                    if (relativeZ == 11
                            || relativeZ == 12) {

                        chunk.setBlock(
                                localX,
                                groundY + 2,
                                localZ,
                                Material.AIR
                        );

                        chunk.setBlock(
                                localX,
                                groundY + 3,
                                localZ,
                                Material.AIR
                        );
                    }
                }

                /*
                 * North/south side windows.
                 */
                if (northWall
                        || southWall) {

                    int relativeX =
                            worldX - minX;

                    if ((relativeX >= 3
                            && relativeX <= 5)
                            ||
                            (relativeX >= 11
                                    && relativeX <= 13)) {

                        chunk.setBlock(
                                localX,
                                groundY + 3,
                                localZ,
                                Material.GLASS_PANE
                        );

                        chunk.setBlock(
                                localX,
                                groundY + 4,
                                localZ,
                                Material.GLASS_PANE
                        );
                    }
                }
            }
        }

        generateMainRoof(
                chunk,
                chunkX,
                chunkZ,
                groundY,
                storeX,
                storeZ
        );

        generateCenterRoof(
                chunk,
                chunkX,
                chunkZ,
                groundY,
                storeX,
                storeZ
        );

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
     * Main roof.
     */
    private static void generateMainRoof(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            int groundY,
            int storeX,
            int storeZ
    ) {

        int minZ =
                storeZ
                        - GUEMES_STORE_SIZE_Z / 2
                        - 1;

        int maxZ =
                storeZ
                        + GUEMES_STORE_SIZE_Z / 2
                        + 1;

        for (int localX = 0;
             localX < 16;
             localX++) {

            for (int localZ = 0;
                 localZ < 16;
                 localZ++) {

                int worldX =
                        chunkX * 16
                                + localX;

                int worldZ =
                        chunkZ * 16
                                + localZ;

                if (worldZ < minZ
                        || worldZ > maxZ) {

                    continue;
                }

                int distanceFromCenter =
                        Math.abs(
                                worldX - storeX
                        );

                int roofY;

                if (distanceFromCenter <= 2) {

                    roofY =
                            groundY + 8;

                } else if (distanceFromCenter <= 5) {

                    roofY =
                            groundY + 7;

                } else if (distanceFromCenter <= 9) {

                    roofY =
                            groundY + 6;

                } else {

                    continue;
                }

                chunk.setBlock(
                        localX,
                        roofY,
                        localZ,
                        Material.DARK_OAK_PLANKS
                );
            }
        }
    }

    /*
     * Raised rear roof.
     *
     * Rear is EAST in the approved orientation.
     */
    private static void generateCenterRoof(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            int groundY,
            int storeX,
            int storeZ
    ) {

        int centerX =
                storeX - 2;

        int minZ =
                storeZ - 5;

        int maxZ =
                storeZ + 5;

        for (int localX = 0;
             localX < 16;
             localX++) {

            for (int localZ = 0;
                 localZ < 16;
                 localZ++) {

                int worldX =
                        chunkX * 16
                                + localX;

                int worldZ =
                        chunkZ * 16
                                + localZ;

                if (worldZ < minZ
                        || worldZ > maxZ) {

                    continue;
                }

                int distance =
                        Math.abs(
                                worldX - centerX
                        );

                if (distance == 3) {

                    chunk.setBlock(
                            localX,
                            groundY + 8,
                            localZ,
                            Material.SPRUCE_PLANKS
                    );
                }

                int roofY;

                if (distance <= 1) {

                    roofY =
                            groundY + 11;

                } else if (distance <= 2) {

                    roofY =
                            groundY + 10;

                } else if (distance <= 4) {

                    roofY =
                            groundY + 9;

                } else {

                    continue;
                }

                chunk.setBlock(
                        localX,
                        roofY,
                        localZ,
                        Material.DARK_OAK_PLANKS
                );
            }
        }
    }

    /*
     * Approved WEST porch.
     */
    private static void generateWestPorch(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            int groundY,
            int storeX,
            int storeZ
    ) {

        int frontX =
                storeX
                        + GUEMES_STORE_SIZE_X / 2;

        int porchMinX =
                frontX + 1;

        int porchMaxX =
                frontX + 4;

        int porchMinZ =
                storeZ - 7;

        int porchMaxZ =
                storeZ + 7;

        for (int localX = 0;
             localX < 16;
             localX++) {

            for (int localZ = 0;
                 localZ < 16;
                 localZ++) {

                int worldX =
                        chunkX * 16
                                + localX;

                int worldZ =
                        chunkZ * 16
                                + localZ;

                if (worldX < porchMinX
                        || worldX > porchMaxX
                        || worldZ < porchMinZ
                        || worldZ > porchMaxZ) {

                    continue;
                }

                /*
                 * Porch floor.
                 */
                chunk.setBlock(
                        localX,
                        groundY + 1,
                        localZ,
                        Material.SPRUCE_PLANKS
                );

                /*
                 * Porch roof.
                 */
                chunk.setBlock(
                        localX,
                        groundY + 6,
                        localZ,
                        Material.DARK_OAK_PLANKS
                );

                /*
                 * Porch posts.
                 */
                boolean outsideEdge =
                        worldX == porchMaxX;

                boolean post =
                        worldZ == porchMinZ
                                || worldZ == storeZ - 3
                                || worldZ == storeZ + 3
                                || worldZ == porchMaxZ;

                if (outsideEdge
                        && post) {

                    for (int y = groundY + 2;
                         y <= groundY + 5;
                         y++) {

                        chunk.setBlock(
                                localX,
                                y,
                                localZ,
                                Material.STRIPPED_SPRUCE_LOG
                        );
                    }
                }
            }
        }
    }

    /*
     * Debug/location information.
     */
    public static String getGuemesStoreLocationInfo() {

        return "Guemes General Store: "
                + "lat="
                + GUEMES_STORE_LATITUDE
                + ", lon="
                + GUEMES_STORE_LONGITUDE
                + ", x="
                + getGuemesStoreX()
                + ", y="
                + getGuemesStoreGroundY()
                + ", z="
                + getGuemesStoreZ();
    }
}
