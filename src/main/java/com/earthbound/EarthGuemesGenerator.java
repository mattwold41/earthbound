package com.earthbound;

import org.bukkit.generator.ChunkGenerator.ChunkData;

/**
 * ============================================================
 * EARTHBOUND - GUEMES ISLAND DEVELOPMENT CONTROLLER
 * ============================================================
 *
 * This class controls generated development outside the
 * already-approved Area A neighborhood.
 *
 * IMPORTANT DESIGN RULES:
 *
 * - Preserve the real USGS terrain.
 * - Preserve the real Census road network.
 * - Do not cover the entire island with houses.
 * - Keep large forested areas open.
 * - Keep Guemes Mountain undeveloped.
 * - Keep shoreline/coastal areas protected.
 * - Keep the approved Area A neighborhood separate.
 * - Buildings use foundations rather than flattening terrain.
 *
 * This is the beginning of the island-wide development system.
 * More Guemes districts can be added here without changing
 * the approved Area A generator.
 * ============================================================
 */
public final class EarthGuemesGenerator {

    /*
     * ============================================================
     * GUEMES ISLAND REFERENCE BOUNDS
     * ============================================================
     *
     * These match the Guemes geographic area already used by
     * EarthBound's terrain/water systems.
     */

    private static final double WEST_LONGITUDE = -122.70;
    private static final double EAST_LONGITUDE = -122.55;

    private static final double SOUTH_LATITUDE = 48.47;
    private static final double NORTH_LATITUDE = 48.60;

    /*
     * ============================================================
     * PROTECTED GUEMES MOUNTAIN AREA
     * ============================================================
     *
     * Approximate development-protection zone.
     *
     * This does NOT alter the terrain.
     * It simply prevents this generator from placing residential
     * development in the mountain area.
     */

    private static final double GUEMES_MOUNTAIN_LATITUDE =
            48.5445;

    private static final double GUEMES_MOUNTAIN_LONGITUDE =
            -122.5945;

    /*
     * Rough real-world protection radius in meters.
     *
     * We can refine this later during the final Guemes walkthrough.
     */
    private static final double GUEMES_MOUNTAIN_PROTECTION_METERS =
            850.0;

    /*
     * ============================================================
     * COASTLINE PROTECTION
     * ============================================================
     *
     * Houses should not be automatically placed directly on
     * beaches, tidal areas, or water.
     *
     * The final coastline remains controlled by EarthWaterData.
     */

    private static final int MINIMUM_BUILD_HEIGHT =
            EarthWaterData.SEA_LEVEL + 3;

    /*
     * ============================================================
     * FUTURE DEVELOPMENT SWITCH
     * ============================================================
     *
     * We intentionally start false.
     *
     * This means adding this controller to EarthGenerator is safe:
     * it will NOT suddenly cover Guemes with experimental houses.
     *
     * After the island-wide placement plan is verified, this can
     * be enabled.
     */

    private static final boolean ENABLE_ISLAND_HOMES =
            false;

    private EarthGuemesGenerator() {
    }

    /*
     * ============================================================
     * MAIN GUEMES GENERATION ENTRY POINT
     * ============================================================
     */

    public static void generate(
            ChunkData chunkData,
            int chunkX,
            int chunkZ
    ) {

        /*
         * Development is deliberately disabled until the first
         * island-wide placement map is approved.
         *
         * Keeping the controller connected but inactive lets us
         * build the system safely without risking the approved
         * Area A neighborhood.
         */
        if (!ENABLE_ISLAND_HOMES) {
            return;
        }

        int chunkMinX =
                chunkX << 4;

        int chunkMinZ =
                chunkZ << 4;

        int chunkCenterX =
                chunkMinX + 8;

        int chunkCenterZ =
                chunkMinZ + 8;

        double latitude =
                EarthCoordinates.getLatitude(
                        chunkCenterX,
                        chunkCenterZ
                );

        double longitude =
                EarthCoordinates.getLongitude(
                        chunkCenterX,
                        chunkCenterZ
                );

        /*
         * Never generate Guemes development outside the
         * Guemes geographic working area.
         */
        if (!isInsideGuemesBounds(
                latitude,
                longitude
        )) {
            return;
        }

        /*
         * Preserve Guemes Mountain and the surrounding
         * natural/conservation area.
         */
        if (isInsideGuemesMountainProtection(
                latitude,
                longitude
        )) {
            return;
        }

        /*
         * Additional Guemes districts will be called from here.
         *
         * Examples later:
         *
         * generateSouthGuemesResidential(...);
         * generateCentralGuemesResidential(...);
         * generateNorthGuemesResidential(...);
         * generateCommunityArea(...);
         *
         * We will only add them after their locations have been
         * checked against the real roads and the island plan.
         */
    }

    /*
     * ============================================================
     * GUEMES BOUNDARY CHECK
     * ============================================================
     */

    public static boolean isInsideGuemesBounds(
            double latitude,
            double longitude
    ) {

        return latitude >= SOUTH_LATITUDE
                && latitude <= NORTH_LATITUDE
                && longitude >= WEST_LONGITUDE
                && longitude <= EAST_LONGITUDE;
    }

    /*
     * ============================================================
     * GUEMES MOUNTAIN PROTECTION
     * ============================================================
     */

    public static boolean isInsideGuemesMountainProtection(
            double latitude,
            double longitude
    ) {

        double distanceMeters =
                distanceMeters(
                        latitude,
                        longitude,
                        GUEMES_MOUNTAIN_LATITUDE,
                        GUEMES_MOUNTAIN_LONGITUDE
                );

        return distanceMeters
                <= GUEMES_MOUNTAIN_PROTECTION_METERS;
    }

    /*
     * ============================================================
     * TERRAIN DEVELOPMENT CHECK
     * ============================================================
     *
     * This can be used before placing future homes or businesses.
     */

    public static boolean isSuitableForDevelopment(
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

        if (!isInsideGuemesBounds(
                latitude,
                longitude
        )) {
            return false;
        }

        if (isInsideGuemesMountainProtection(
                latitude,
                longitude
        )) {
            return false;
        }

        double elevationMeters =
                EarthTerrainLoader.getGuemesElevation(
                        latitude,
                        longitude
                );

        int minecraftHeight =
                EarthElevation.getMinecraftHeight(
                        elevationMeters
                );

        /*
         * Protect shoreline / very-low coastal ground.
         */
        if (minecraftHeight < MINIMUM_BUILD_HEIGHT) {
            return false;
        }

        return true;
    }

    /*
     * ============================================================
     * REUSABLE RESIDENTIAL HOME
     * ============================================================
     *
     * This provides one controlled doorway into the approved
     * Version 6 house generator.
     *
     * Future Guemes districts can use the same approved house
     * design instead of duplicating it.
     */

    public static void generateResidentialHome(
            ChunkData chunkData,
            int chunkMinX,
            int chunkMinZ,
            int plotMinX,
            int plotMinZ,
            boolean northSide,
            int houseNumber
    ) {

        /*
         * Use the center of the future property as a quick
         * development-safety check.
         */
        int propertyCenterX =
                plotMinX + 8;

        int propertyCenterZ =
                plotMinZ + 10;

        if (!isSuitableForDevelopment(
                propertyCenterX,
                propertyCenterZ
        )) {
            return;
        }

        EarthResidentialGenerator.generateHome(
                chunkData,
                chunkMinX,
                chunkMinZ,
                plotMinX,
                plotMinZ,
                northSide,
                houseNumber
        );
    }

    /*
     * ============================================================
     * DISTANCE HELPER
     * ============================================================
     *
     * Haversine distance between two geographic coordinates.
     */

    private static double distanceMeters(
            double latitude1,
            double longitude1,
            double latitude2,
            double longitude2
    ) {

        final double earthRadiusMeters =
                6371000.0;

        double latitude1Radians =
                Math.toRadians(
                        latitude1
                );

        double latitude2Radians =
                Math.toRadians(
                        latitude2
                );

        double latitudeDifference =
                Math.toRadians(
                        latitude2 - latitude1
                );

        double longitudeDifference =
                Math.toRadians(
                        longitude2 - longitude1
                );

        double sinLatitude =
                Math.sin(
                        latitudeDifference / 2.0
                );

        double sinLongitude =
                Math.sin(
                        longitudeDifference / 2.0
                );

        double a =
                sinLatitude * sinLatitude
                        + Math.cos(
                                latitude1Radians
                        )
                        * Math.cos(
                                latitude2Radians
                        )
                        * sinLongitude
                        * sinLongitude;

        double c =
                2.0
                        * Math.atan2(
                                Math.sqrt(a),
                                Math.sqrt(1.0 - a)
                        );

        return earthRadiusMeters * c;
    }
}
