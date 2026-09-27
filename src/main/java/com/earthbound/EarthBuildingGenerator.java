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

    public static final double
            GUEMES_STORE_LATITUDE =
            48.529460;

    public static final double
            GUEMES_STORE_LONGITUDE =
            -122.624110;


    /*
     * Playable building dimensions.
     */
    private static final int
            GUEMES_STORE_WIDTH =
            24;

    private static final int
            GUEMES_STORE_LENGTH =
            16;


    private static final int
            FOUNDATION_MARGIN =
            2;


    public static int
    getGuemesStoreX() {

        return EarthCoordinates
                .longitudeToMinecraftX(
                        GUEMES_STORE_LONGITUDE
                );
    }


    public static int
    getGuemesStoreZ() {

        return EarthCoordinates
                .latitudeToMinecraftZ(
                        GUEMES_STORE_LATITUDE
                );
    }


    public static boolean
    isInsideGuemesStore(
            int worldX,
            int worldZ
    ) {

        int storeX =
                getGuemesStoreX();

        int storeZ =
                getGuemesStoreZ();


        int halfWidth =
                GUEMES_STORE_WIDTH / 2;

        int halfLength =
                GUEMES_STORE_LENGTH / 2;


        return worldX >= storeX - halfWidth
                && worldX <= storeX + halfWidth
                && worldZ >= storeZ - halfLength
                && worldZ <= storeZ + halfLength;
    }


    public static boolean
    isInsideGuemesStoreFoundation(
            int worldX,
            int worldZ
    ) {

        int storeX =
                getGuemesStoreX();

        int storeZ =
                getGuemesStoreZ();


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


    public static Double
    getGuemesStoreElevation() {

        return EarthTerrainLoader
                .getGuemesElevation(
                        GUEMES_STORE_LATITUDE,
                        GUEMES_STORE_LONGITUDE
                );
    }


    public static int
    getGuemesStoreGroundY() {

        Double elevation =
                getGuemesStoreElevation();


        if (elevation == null
                || !Double.isFinite(
                        elevation
                )) {

            return 63;

        }


        return EarthElevation
                .getMinecraftHeight(
                        elevation
                );
    }


    /*
     * =====================================================
     * STORE GENERATION
     * =====================================================
     *
     * Called for each generated chunk.
     *
     * Every block is checked using WORLD
     * coordinates, so the building can cross
     * chunk boundaries safely.
     */
    public static void
    generateGuemesStore(
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
                        - GUEMES_STORE_WIDTH / 2;

        int maxX =
                storeX
                        + GUEMES_STORE_WIDTH / 2;

        int minZ =
                storeZ
                        - GUEMES_STORE_LENGTH / 2;

        int maxZ =
                storeZ
                        + GUEMES_STORE_LENGTH / 2;


        /*
         * Check every horizontal block
         * belonging to this chunk.
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
                 * FOUNDATION / FLOOR
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


                /*
                 * Determine exterior walls.
                 */
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
                 * WALLS
                 */
                if (exterior) {


                    for (int y =
                         groundY + 2;
                         y <= groundY + 6;
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
                 * Front of store is treated
                 * as the south side.
                 */
                if (southWall) {


                    int relativeX =
                            worldX - minX;


                    boolean window =
                            (relativeX >= 3
                                    && relativeX <= 7)
                                    ||
                                    (relativeX >= 17
                                            && relativeX <= 21);


                    if (window) {


                        chunk.setBlock(
                                localX,
                                groundY + 3,
                                localZ,
                                Material.GLASS
                        );


                        chunk.setBlock(
                                localX,
                                groundY + 4,
                                localZ,
                                Material.GLASS
                        );

                    }

                }


                /*
                 * FRONT DOUBLE DOOR OPENING
                 */
                if (southWall) {


                    int relativeX =
                            worldX - minX;


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
                 * LOWER ROOF
                 */
                chunk.setBlock(
                        localX,
                        groundY + 7,
                        localZ,
                        Material.DARK_OAK_PLANKS
                );

            }

        }


        /*
         * Add the taller center section.
         */
        generateStoreUpperSection(
                chunk,
                chunkX,
                chunkZ,
                groundY,
                storeX,
                storeZ
        );

    }


    /*
     * Taller central portion that gives
     * the General Store its recognizable
     * stepped roof/profile.
     */
    private static void
    generateStoreUpperSection(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            int groundY,
            int storeX,
            int storeZ
    ) {


        int upperMinX =
                storeX - 5;

        int upperMaxX =
                storeX + 5;

        int upperMinZ =
                storeZ - 4;

        int upperMaxZ =
                storeZ + 4;


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


                if (worldX < upperMinX
                        || worldX > upperMaxX
                        || worldZ < upperMinZ
                        || worldZ > upperMaxZ) {

                    continue;

                }


                boolean exterior =
                        worldX == upperMinX
                                || worldX == upperMaxX
                                || worldZ == upperMinZ
                                || worldZ == upperMaxZ;


                if (exterior) {


                    chunk.setBlock(
                            localX,
                            groundY + 8,
                            localZ,
                            Material.SPRUCE_PLANKS
                    );


                    chunk.setBlock(
                            localX,
                            groundY + 9,
                            localZ,
                            Material.SPRUCE_PLANKS
                    );

                }


                /*
                 * Upper dark roof.
                 */
                chunk.setBlock(
                        localX,
                        groundY + 10,
                        localZ,
                        Material.DARK_OAK_PLANKS
                );

            }

        }

    }


    public static String
    getGuemesStoreLocationInfo() {

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
