package com.earthbound;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.generator.ChunkGenerator.ChunkData;

import com.earthbound.water.EarthWaterData;
import com.earthbound.roads.EarthRoadData;
import com.earthbound.terrain.EarthTerrainLoader;
import com.earthbound.terrain.EarthElevation;

import com.earthbound.EarthAlphaResidentialGenerator.ResidentialSite;

public final class EarthGuemesGenerator {

    /*
     * =========================================================
     * EARTHBOUND GUEMES GENERATOR
     * =========================================================
     *
     * Controls Guemes Island development.
     *
     * Connects:
     *
     * - Water
     * - Roads
     * - Terrain
     * - Elevation
     * - Friends Alpha residential development
     *
     * Future development:
     *
     * - Residential districts
     * - Farms
     * - Businesses
     * - Public infrastructure
     * =========================================================
     */

    private static final int MINIMUM_BUILD_HEIGHT =
            EarthWaterData.SEA_LEVEL + 3;

    /*
     * Keep residential generation disabled until
     * all six property locations are approved.
     */

    private static final boolean ENABLE_ISLAND_HOMES =
            false;

    private static final int MAX_ALPHA_HOMES = 6;

    /*
     * Immutable site snapshots allow safe reading
     * during chunk generation.
     */

    private static volatile List<ResidentialSite>
            residentialSites = List.of();

    private EarthGuemesGenerator() {
    }

    /*
     * =========================================================
     * RESIDENTIAL SITE REGISTRATION
     * =========================================================
     *
     * Register approved sites as a complete collection.
     *
     * Reject:
     *
     * - Null sites
     * - Duplicate property IDs
     * - Overlapping properties
     * - Unsuitable terrain
     * - More than six Alpha homes
     *
     * Existing site registrations remain unchanged
     * if validation fails.
     * =========================================================
     */

    public static synchronized boolean registerResidentialSites(
            List<ResidentialSite> sites
    ) {

        if (sites == null) {
            return false;
        }

        if (sites.size() > MAX_ALPHA_HOMES) {
            return false;
        }

        if (!EarthRoadData.isLoaded()) {
            return false;
        }

        List<ResidentialSite> approvedSites =
                new ArrayList<>();

        Set<String> propertyIds =
                new HashSet<>();

        for (ResidentialSite site : sites) {

            if (site == null) {
                return false;
            }

            if (!propertyIds.add(
                    site.getPropertyId()
            )) {

                return false;
            }

            if (!EarthAlphaResidentialGenerator
                    .isSuitableSite(site)) {

                return false;
            }

            /*
             * Prevent residential properties from
             * overlapping one another.
             */

            for (ResidentialSite approved
                    : approvedSites) {

                if (site.overlaps(approved)) {

                    return false;
                }
            }

            approvedSites.add(site);
        }

        /*
         * Publish the validated collection only
         * after every site passes validation.
         */

        residentialSites =
                List.copyOf(approvedSites);

        return true;
    }

    /*
     * =========================================================
     * RESIDENTIAL REGISTRY INFORMATION
     * =========================================================
     */

    public static int getResidentialSiteCount() {

        return residentialSites.size();
    }

    public static List<ResidentialSite>
            getResidentialSites() {

        return residentialSites;
    }

    public static ResidentialSite getResidentialSite(
            String propertyId
    ) {

        if (propertyId == null) {
            return null;
        }

        for (ResidentialSite site
                : residentialSites) {

            if (site.getPropertyId()
                    .equals(propertyId)) {

                return site;
            }
        }

        return null;
    }

    public static boolean isResidentialGenerationEnabled() {

        return ENABLE_ISLAND_HOMES;
    }

    public static synchronized void clearResidentialSites() {

        residentialSites = List.of();
    }

    /*
     * =========================================================
     * GUEMES ISLAND GENERATION
     * =========================================================
     */

    public static void generate(
            ChunkData chunkData,
            int chunkX,
            int chunkZ
    ) {

        if (chunkData == null) {
            return;
        }

        if (!ENABLE_ISLAND_HOMES) {
            return;
        }

        if (!EarthRoadData.isLoaded()) {
            return;
        }

        List<ResidentialSite> sites =
                residentialSites;

        /*
         * No approved properties means no construction.
         */

        if (sites.isEmpty()) {
            return;
        }

        /*
         * Generate only residential sites that
         * intersect the current chunk.
         */

        for (ResidentialSite site : sites) {

            if (!site.intersectsChunk(
                    chunkX,
                    chunkZ
            )) {

                continue;
            }

            EarthAlphaResidentialGenerator.generate(
                    chunkData,
                    chunkX,
                    chunkZ,
                    site
            );
        }
    }

    /*
     * =========================================================
     * DEVELOPMENT ELEVATION CHECK
     * =========================================================
     *
     * Preserves the original Guemes development
     * elevation check.
     *
     * Full residential validation is handled by
     * EarthAlphaResidentialGenerator.
     * =========================================================
     */

    public static boolean isSuitableForDevelopment(
            double latitude,
            double longitude
    ) {

        double elevationMeters =
                EarthTerrainLoader.getGuemesElevation(
                        latitude,
                        longitude
                );

        int minecraftHeight =
                EarthElevation.getMinecraftHeight(
                        elevationMeters
                );

        return minecraftHeight >= MINIMUM_BUILD_HEIGHT;
    }
}
