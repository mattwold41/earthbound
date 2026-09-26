package com.earthbound;

public class GuemesGeneralStore {

    /*
     * =====================================================
     * GUEMES ISLAND GENERAL STORE
     * =====================================================
     *
     * First EarthBound real-world building.
     *
     * Location is stored using latitude and longitude.
     * Minecraft coordinates are calculated automatically
     * from EarthCoordinates.
     *
     * This keeps the building ready for future scale
     * changes (1:2, 1:1, etc.).
     */


    /*
     * Real-world location.
     */
    public static final double LATITUDE =
            48.529460;

    public static final double LONGITUDE =
            -122.624110;


    /*
     * Playable building size.
     *
     * These are Minecraft building dimensions,
     * not map scale dimensions.
     */
    public static final int WIDTH =
            24;

    public static final int LENGTH =
            16;


    /*
     * Small foundation area around building.
     */
    public static final int FOUNDATION_MARGIN =
            2;


    private GuemesGeneralStore() {
    }


    /*
     * Convert real longitude to Minecraft X.
     */
    public static int getCenterX() {

        return EarthCoordinates
                .longitudeToMinecraftX(
                        LONGITUDE
                );
    }


    /*
     * Convert real latitude to Minecraft Z.
     */
    public static int getCenterZ() {

        return EarthCoordinates
                .latitudeToMinecraftZ(
                        LATITUDE
                );
    }


    /*
     * Check if a block is inside the store.
     */
    public static boolean isInsideBuilding(
            int x,
            int z) {

        int centerX =
                getCenterX();

        int centerZ =
                getCenterZ();

        int halfWidth =
                WIDTH / 2;

        int halfLength =
                LENGTH / 2;


        return x >= centerX - halfWidth
                && x <= centerX + halfWidth
                && z >= centerZ - halfLength
                && z <= centerZ + halfLength;
    }


    /*
     * Check foundation area.
     */
    public static boolean isInsideFoundationArea(
            int x,
            int z) {

        int centerX =
                getCenterX();

        int centerZ =
                getCenterZ();

        int halfWidth =
                WIDTH / 2
                        + FOUNDATION_MARGIN;

        int halfLength =
                LENGTH / 2
                        + FOUNDATION_MARGIN;


        return x >= centerX - halfWidth
                && x <= centerX + halfWidth
                && z >= centerZ - halfLength
                && z <= centerZ + halfLength;
    }


    /*
     * Future entrance location.
     */
    public static int getEntranceX() {

        return getCenterX();
    }


    public static int getEntranceZ() {

        return getCenterZ()
                - (LENGTH / 2)
                - 2;
    }


    /*
     * Debug information.
     */
    public static String getLocationInfo() {

        return "Guemes General Store "
                + "X="
                + getCenterX()
                + " Z="
                + getCenterZ();
    }
}
