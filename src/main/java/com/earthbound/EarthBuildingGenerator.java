package com.earthbound;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator.ChunkData;

public class EarthBuildingGenerator {

    private EarthBuildingGenerator() {
    }

    // Real-world Guemes Island General Store location
    public static final double GUEMES_STORE_LATITUDE = 48.529460;
    public static final double GUEMES_STORE_LONGITUDE = -122.624110;

    /*
     * ROTATED GENERAL STORE
     *
     * Previous footprint:
     *   24 blocks X
     *   16 blocks Z
     *
     * New footprint after 90-degree rotation:
     *   16 blocks X
     *   24 blocks Z
     *
     * Entrance/porch is on the EAST side.
     */
    private static final int GUEMES_STORE_SIZE_X = 16;
    private static final int GUEMES_STORE_SIZE_Z = 24;

    private static final int FOUNDATION_MARGIN = 2;

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

    public static boolean isInsideGuemesStore(
            int worldX,
            int worldZ
    ) {
        int storeX = getGuemesStoreX();
        int storeZ = getGuemesStoreZ();

        int halfX = GUEMES_STORE_SIZE_X / 2;
        int halfZ = GUEMES_STORE_SIZE_Z / 2;

        return worldX >= storeX - halfX
                && worldX <= storeX + halfX
                && worldZ >= storeZ - halfZ
                && worldZ <= storeZ + halfZ;
    }

    public static boolean isInsideGuemesStoreFoundation(
            int worldX,
            int worldZ
    ) {
        int storeX = getGuemesStoreX();
        int storeZ = getGuemesStoreZ();

        int halfX =
                GUEMES_STORE_SIZE_X / 2 + FOUNDATION_MARGIN;

        int halfZ =
                GUEMES_STORE_SIZE_Z / 2 + FOUNDATION_MARGIN;

        return worldX >= storeX - halfX
                && worldX <= storeX + halfX
                && worldZ >= storeZ - halfZ
                && worldZ <= storeZ + halfZ;
    }

    /*
     * Larger area used by EarthGenerator to gradually blend
     * the natural terrain into the store foundation.
     */
    public static boolean isInsideGuemesStoreBlendArea(
            int worldX,
            int worldZ
    ) {
        int storeX = getGuemesStoreX();
        int storeZ = getGuemesStoreZ();

        int blendMargin = 14;

        int halfX =
                GUEMES_STORE_SIZE_X / 2 + blendMargin;

        int halfZ =
                GUEMES_STORE_SIZE_Z / 2 + blendMargin;

        return worldX >= storeX - halfX
                && worldX <= storeX + halfX
                && worldZ >= storeZ - halfZ
                && worldZ <= storeZ + halfZ;
    }

    public static Double getGuemesStoreElevation() {
        return EarthTerrainLoader.getGuemesElevation(
                GUEMES_STORE_LATITUDE,
                GUEMES_STORE_LONGITUDE
        );
    }

    public static int getGuemesStoreGroundY() {
        Double elevation = getGuemesStoreElevation();

        if (elevation == null || !Double.isFinite(elevation)) {
            return 63;
        }

        return EarthElevation.getMinecraftHeight(elevation);
    }

    /*
     * Returns the distance outside the actual building footprint.
     *
     * 0 = underneath the building
     * larger values = farther away from building
     *
     * EarthGenerator will use this to make a gradual terrain
     * transition instead of a vertical rectangular pedestal.
     */
    public static double getDistanceFromGuemesStore(
            int worldX,
            int worldZ
    ) {
        int storeX = getGuemesStoreX();
        int storeZ = getGuemesStoreZ();

        int halfX = GUEMES_STORE_SIZE_X / 2;
        int halfZ = GUEMES_STORE_SIZE_Z / 2;

        int dx =
                Math.max(
                        Math.abs(worldX - storeX) - halfX,
                        0
                );

        int dz =
                Math.max(
                        Math.abs(worldZ - storeZ) - halfZ,
                        0
                );

        return Math.sqrt(
                (double) dx * dx
                        + (double) dz * dz
        );
    }

    public static void generateGuemesStore(
            ChunkData chunk,
            int chunkX,
            int chunkZ
    ) {
        int groundY = getGuemesStoreGroundY();

        int storeX = getGuemesStoreX();
        int storeZ = getGuemesStoreZ();

        int minX =
                storeX - GUEMES_STORE_SIZE_X / 2;

        int maxX =
                storeX + GUEMES_STORE_SIZE_X / 2;

        int minZ =
                storeZ - GUEMES_STORE_SIZE_Z / 2;

        int maxZ =
                storeZ + GUEMES_STORE_SIZE_Z / 2;

        /*
         * MAIN BUILDING
         */
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {

                int worldX =
                        chunkX * 16 + localX;

                int worldZ =
                        chunkZ * 16 + localZ;

                if (worldX < minX
                        || worldX > maxX
                        || worldZ < minZ
                        || worldZ > maxZ) {
                    continue;
                }

                /*
                 * Clear space above building.
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
                 * Foundation and floor.
                 */
                chunk.setBlock(
                        localX,
                        groundY,
                        localZ,
                        Material.STONE_BRICKS
                );

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
                 * EAST WALL
                 *
                 * This is now the main entrance side.
                 */
                if (eastWall) {

                    int relativeZ =
                            worldZ - minZ;

                    boolean southWindow =
                            relativeZ >= 3
                                    && relativeZ <= 6;

                    boolean northWindow =
                            relativeZ >= 17
                                    && relativeZ <= 20;

                    if (southWindow || northWindow) {

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
                     * Double entrance.
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
                 * NORTH AND SOUTH WINDOWS
                 */
                if (northWall || southWall) {

                    int relativeX =
                            worldX - minX;

                    if ((relativeX >= 3
                            && relativeX <= 5)
                            || (relativeX >= 11
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

        generateEastPorch(
                chunk,
                chunkX,
                chunkZ,
                groundY,
                storeX,
                storeZ
        );
    }

    /*
     * MAIN ROOF
     *
     * Ridge now runs north/south because the building
     * has been rotated 90 degrees.
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
                storeZ - GUEMES_STORE_SIZE_Z / 2 - 1;

        int maxZ =
                storeZ + GUEMES_STORE_SIZE_Z / 2 + 1;

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {

                int worldX =
                        chunkX * 16 + localX;

                int worldZ =
                        chunkZ * 16 + localZ;

                if (worldZ < minZ
                        || worldZ > maxZ) {
                    continue;
                }

                int distanceFromCenter =
                        Math.abs(worldX - storeX);

                int roofY;

                if (distanceFromCenter <= 2) {

                    roofY = groundY + 8;

                } else if (distanceFromCenter <= 5) {

                    roofY = groundY + 7;

                } else if (distanceFromCenter <= 9) {

                    roofY = groundY + 6;

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
     * RAISED CENTER / REAR ROOF
     *
     * The entrance is on the east side.
     * This raised section sits slightly toward
     * the west/rear side of the building.
     */
    private static void generateCenterRoof(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            int groundY,
            int storeX,
            int storeZ
    ) {
        int centerX = storeX + 2;

        int minZ = storeZ - 5;
        int maxZ = storeZ + 5;

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {

                int worldX =
                        chunkX * 16 + localX;

                int worldZ =
                        chunkZ * 16 + localZ;

                if (worldZ < minZ
                        || worldZ > maxZ) {
                    continue;
                }

                int distance =
                        Math.abs(worldX - centerX);

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

                    roofY = groundY + 11;

                } else if (distance <= 2) {

                    roofY = groundY + 10;

                } else if (distance <= 4) {

                    roofY = groundY + 9;

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
     * EAST-SIDE COVERED PORCH
     *
     * This replaces the old south-facing porch.
     */
    private static void generateEastPorch(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            int groundY,
            int storeX,
            int storeZ
    ) {
        int frontX =
                storeX - GUEMES_STORE_SIZE_X / 2;

        int porchMinX =
                frontX - 4;

        int porchMaxX =
                frontX - 1;

        int porchMinZ =
                storeZ - 7;

        int porchMaxZ =
                storeZ + 7;

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {

                int worldX =
                        chunkX * 16 + localX;

                int worldZ =
                        chunkZ * 16 + localZ;

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
                 * Posts along outside edge.
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

    public static String getGuemesStoreLocationInfo() {

        return "Guemes General Store: "
                + "lat=" + GUEMES_STORE_LATITUDE
                + ", lon=" + GUEMES_STORE_LONGITUDE
                + ", x=" + getGuemesStoreX()
                + ", z=" + getGuemesStoreZ();
    }
}
