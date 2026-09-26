package com.earthbound;

public class EarthBuildingGenerator {

    /*
     * EarthBound building system.
     *
     * Buildings are stored using real-world
     * latitude and longitude instead of
     * permanent Minecraft X/Z coordinates.
     *
     * This allows EarthBound to reposition
     * buildings if the horizontal map scale
     * changes in the future.
     */

    private EarthBuildingGenerator() {
    }


    /*
     * =====================================================
     * GUEMES ISLAND GENERAL STORE
     * =====================================================
     *
     * EarthBound's first scale-independent
     * real-world building.
     *
     * Geographic location is always the
     * source of truth.
     */

    public static final double
            GUEMES_STORE_LATITUDE =
            48.529460;

    public static final double
            GUEMES_STORE_LONGITUDE =
            -122.624110;


    /*
     * Playable Minecraft building footprint.
     *
     * The geographic map itself is 1:2,
     * but buildings do NOT have to be
     * physically shrunk to half size.
     *
     * This keeps doors, rooms, windows,
     * porches, and interiors usable.
     *
     * We can fine-tune these dimensions
     * when we construct the actual store.
     */
    private static final int
            GUEMES_STORE_WIDTH =
            24;

    private static final int
            GUEMES_STORE_LENGTH =
            16;


    /*
     * Small amount of extra leveled ground
     * around the building.
     *
     * We intentionally keep this small so
     * the nearby shoreline and road are not
     * flattened unnecessarily.
     */
    private static final int
            FOUNDATION_MARGIN =
            2;


    /*
     * Returns the Minecraft X coordinate
     * for the center of the General Store.
     *
     * This is calculated from real-world
     * longitude every time instead of being
     * permanently hard-coded.
     */
    public static int
    getGuemesStoreX() {

        return EarthCoordinates
                .longitudeToMinecraftX(
                        GUEMES_STORE_LONGITUDE
                );
    }


    /*
     * Returns the Minecraft Z coordinate
     * for the center of the General Store.
     *
     * This is calculated from real-world
     * latitude every time.
     */
    public static int
    getGuemesStoreZ() {

        return EarthCoordinates
                .latitudeToMinecraftZ(
                        GUEMES_STORE_LATITUDE
                );
    }


    /*
     * Returns true when a Minecraft block
     * is inside the General Store's main
     * building footprint.
     */
    public static boolean
    isInsideGuemesStore(
            int worldX,
            int worldZ) {

        int storeX =
                getGuemesStoreX();

        int storeZ =
                getGuemesStoreZ();

        int halfWidth =
                GUEMES_STORE_WIDTH / 2;

        int halfLength =
                GUEMES_STORE_LENGTH / 2;

        return worldX >=
                storeX - halfWidth
                && worldX <=
                storeX + halfWidth
                && worldZ >=
                storeZ - halfLength
                && worldZ <=
                storeZ + halfLength;
    }


    /*
     * Returns true when a block is inside
     * the small foundation-preparation area
     * surrounding the General Store.
     */
    public static boolean
    isInsideGuemesStoreFoundation(
            int worldX,
            int worldZ) {

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

        return worldX >=
                storeX - halfWidth
                && worldX <=
                storeX + halfWidth
                && worldZ >=
                storeZ - halfLength
                && worldZ <=
                storeZ + halfLength;
    }


    /*
     * Returns the real-world elevation
     * underneath the center of the store.
     *
     * EarthTerrainLoader already has the
     * USGS raster in memory, so this does
     * not make a new Internet request.
     */
    public static Double
    getGuemesStoreElevation() {

        return EarthTerrainLoader
                .getGuemesElevation(
                        GUEMES_STORE_LATITUDE,
                        GUEMES_STORE_LONGITUDE
                );
    }


    /*
     * Calculates the Minecraft Y level
     * for the General Store foundation.
     *
     * The same EarthBound vertical scaling
     * used by the surrounding terrain is
     * used here.
     */
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
     * Useful diagnostic description.
     *
     * Later we can expose this through an
     * EarthBound admin/debug command.
     */
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
