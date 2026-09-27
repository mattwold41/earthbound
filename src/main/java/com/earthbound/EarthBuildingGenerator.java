package com.earthbound;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator.ChunkData;

public class EarthBuildingGenerator {

    private EarthBuildingGenerator() {
    }


    /*
     * =====================================================
     * GUEMES ISLAND GENERAL STORE
     * =====================================================
     */

    public static final double GUEMES_STORE_LATITUDE =
            48.529460;

    public static final double GUEMES_STORE_LONGITUDE =
            -122.624110;


    /*
     * Main playable building.
     */
    private static final int GUEMES_STORE_WIDTH = 24;

    private static final int GUEMES_STORE_LENGTH = 16;

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

        int halfWidth =
                GUEMES_STORE_WIDTH / 2;

        int halfLength =
                GUEMES_STORE_LENGTH / 2;

        return worldX >= storeX - halfWidth
                && worldX <= storeX + halfWidth
                && worldZ >= storeZ - halfLength
                && worldZ <= storeZ + halfLength;
    }


    public static boolean isInsideGuemesStoreFoundation(
            int worldX,
            int worldZ
    ) {

        int storeX = getGuemesStoreX();
        int storeZ = getGuemesStoreZ();

        int halfWidth =
                GUEMES_STORE_WIDTH / 2
                        + FOUNDATION_MARGIN;

        int halfLength =
                GUEMES_STORE_LENGTH / 2
                        + FOUNDATION_MARGIN;

        return worldX >= storeX - halfWidth
                && worldX <= storeX + halfWidth
                && worldZ >= storeZ - halfLength
                && worldZ <= storeZ + halfLength;
    }


    public static Double getGuemesStoreElevation() {

        return EarthTerrainLoader.getGuemesElevation(
                GUEMES_STORE_LATITUDE,
                GUEMES_STORE_LONGITUDE
        );
    }


    public static int getGuemesStoreGroundY() {

        Double elevation =
                getGuemesStoreElevation();

        if (elevation == null
                || !Double.isFinite(elevation)) {

            return 63;
        }

        return EarthElevation.getMinecraftHeight(
                elevation
        );
    }


    /*
     * =====================================================
     * MAIN STORE GENERATION
     * =====================================================
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
                storeX - GUEMES_STORE_WIDTH / 2;

        int maxX =
                storeX + GUEMES_STORE_WIDTH / 2;

        int minZ =
                storeZ - GUEMES_STORE_LENGTH / 2;

        int maxZ =
                storeZ + GUEMES_STORE_LENGTH / 2;


        /*
         * Main building shell.
         */
        for (int localX = 0;
             localX < 16;
             localX++) {

            for (int localZ = 0;
                 localZ < 16;
                 localZ++) {

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
                 * Clear the building interior
                 * so old terrain cannot remain
                 * inside the structure.
                 */
                for (int y = groundY + 1;
                     y <= groundY + 12;
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
                 * Wooden floor.
                 */
                chunk.setBlock(
                        localX,
                        groundY + 1,
                        localZ,
                        Material.SPRUCE_PLANKS
                );


                boolean westWall =
                        worldX == minX;

                boolean eastWall =
                        worldX == maxX;

                boolean northWall =
                        worldZ == minZ;

                boolean southWall =
                        worldZ == maxZ;

                boolean exterior =
                        westWall
                                || eastWall
                                || northWall
                                || southWall;


                /*
                 * Low wooden exterior walls.
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
                 * FRONT WINDOWS
                 *
                 * South side is currently
                 * treated as the storefront.
                 */
                if (southWall) {

                    int relativeX =
                            worldX - minX;

                    boolean leftWindow =
                            relativeX >= 3
                                    && relativeX <= 6;

                    boolean rightWindow =
                            relativeX >= 17
                                    && relativeX <= 20;

                    if (leftWindow
                            || rightWindow) {

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
                     * Main entrance opening.
                     */
                    if (relativeX == 11
                            || relativeX == 12) {

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
                 * Side windows.
                 */
                if (westWall || eastWall) {

                    int relativeZ =
                            worldZ - minZ;

                    if ((relativeZ >= 4
                            && relativeZ <= 6)
                            ||
                            (relativeZ >= 10
                                    && relativeZ <= 12)) {

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


        /*
         * Main dark sloped roof.
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
         * Raised central roof section.
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
         * Covered front porch / awning.
         */
        generateFrontPorch(
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
     * MAIN SLOPED ROOF
     * =====================================================
     */

    private static void generateMainRoof(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            int groundY,
            int storeX,
            int storeZ
    ) {

        int minX =
                storeX - GUEMES_STORE_WIDTH / 2 - 1;

        int maxX =
                storeX + GUEMES_STORE_WIDTH / 2 + 1;


        /*
         * Roof rises toward the center.
         *
         * This produces a Minecraft stepped
         * approximation of the dark pitched roof
         * visible in the real store.
         */
        for (int localX = 0;
             localX < 16;
             localX++) {

            for (int localZ = 0;
                 localZ < 16;
                 localZ++) {

                int worldX =
                        chunkX * 16 + localX;

                int worldZ =
                        chunkZ * 16 + localZ;

                if (worldX < minX
                        || worldX > maxX) {

                    continue;
                }


                int distanceFromCenter =
                        Math.abs(worldZ - storeZ);


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
     * =====================================================
     * RAISED CENTER / REAR ROOF
     * =====================================================
     *
     * The real store has a raised section,
     * but it is much shorter than our
     * original test building.
     */

    private static void generateCenterRoof(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            int groundY,
            int storeX,
            int storeZ
    ) {

        int minX = storeX - 5;
        int maxX = storeX + 5;

        int centerZ =
                storeZ + 2;


        for (int localX = 0;
             localX < 16;
             localX++) {

            for (int localZ = 0;
                 localZ < 16;
                 localZ++) {

                int worldX =
                        chunkX * 16 + localX;

                int worldZ =
                        chunkZ * 16 + localZ;

                if (worldX < minX
                        || worldX > maxX) {

                    continue;
                }


                int distance =
                        Math.abs(
                                worldZ - centerZ
                        );


                /*
                 * Small upper wall.
                 */
                if (distance == 3) {

                    chunk.setBlock(
                            localX,
                            groundY + 8,
                            localZ,
                            Material.SPRUCE_PLANKS
                    );
                }


                /*
                 * Raised pitched roof.
                 */
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
     * =====================================================
     * FRONT PORCH / COVERED ENTRANCE
     * =====================================================
     */

    private static void generateFrontPorch(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            int groundY,
            int storeX,
            int storeZ
    ) {

        int frontZ =
                storeZ
                        - GUEMES_STORE_LENGTH / 2;

        int porchMinX =
                storeX - 7;

        int porchMaxX =
                storeX + 7;

        int porchMinZ =
                frontZ - 4;

        int porchMaxZ =
                frontZ - 1;


        for (int localX = 0;
             localX < 16;
             localX++) {

            for (int localZ = 0;
                 localZ < 16;
                 localZ++) {

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
                 * Front support posts.
                 */
                boolean frontEdge =
                        worldZ == porchMinZ;

                boolean post =
                        worldX == porchMinX
                                || worldX == storeX - 3
                                || worldX == storeX + 3
                                || worldX == porchMaxX;


                if (frontEdge && post) {

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
                + "lat="
                + GUEMES_STORE_LATITUDE
                + ", lon="
                + GUEMES_STORE_LONGITUDE
                + ", x="
                + getGuemesStoreX()
                + ", z="
                + getGuemesStoreZ();
    }
}
