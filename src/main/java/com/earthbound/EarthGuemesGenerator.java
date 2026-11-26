package com.earthbound;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;

import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import com.earthbound.water.EarthWaterData;
import com.earthbound.roads.EarthRoadData;
import com.earthbound.roads.EarthRoadSpatialIndex;
import com.earthbound.terrain.EarthTerrainLoader;
import com.earthbound.terrain.EarthElevation;

import com.earthbound.EarthAlphaResidentialGenerator
        .ResidentialSite;

import com.earthbound.EarthResidentialSafety
        .SafetyResult;

public final class EarthGuemesGenerator {

    private static final int MINIMUM_BUILD_HEIGHT =
            EarthWaterData.SEA_LEVEL + 3;

    private static final int MAX_ALPHA_HOMES = 6;

    private static final int MINIMUM_PROPERTY_GAP = 8;

    private static final int MAXIMUM_WORLD_COORDINATE =
            29999900;

    /*
     * ========================================================
     * CONSTRUCTION SAFETY LOCK
     * ========================================================
     *
     * Construction remains disabled until:
     *
     * - Six residential locations are approved.
     * - Geographic safety inspection succeeds.
     * - Existing chunks are inspected.
     * - Installation tracking is implemented.
     * - Construction receives explicit approval.
     */

    private static final boolean CONSTRUCTION_APPROVED =
            false;

    private static volatile List<ResidentialSite>
            residentialSites = List.of();

    private static volatile boolean residentialEnabled =
            false;

    private EarthGuemesGenerator() {
    }

    /*
     * ========================================================
     * LOAD RESIDENTIAL CONFIGURATION
     * ========================================================
     */

    public static synchronized boolean loadResidentialSites(
            EarthBound plugin
    ) {

        clearResidentialSites();

        if (!plugin.getConfig().getBoolean(
                "residential.enabled", false
        )) {

            plugin.getLogger().info(
                    "Residential development disabled "
                    + "in configuration."
            );

            return true;
        }

        if (!EarthTerrainLoader.isLoaded()
                || !EarthRoadData.isLoaded()
                || !EarthRoadSpatialIndex.isLoaded()) {

            plugin.getLogger().warning(
                    "Residential configuration rejected: "
                    + "geographic data is unavailable."
            );

            return false;
        }

        String residentialWorld =
                plugin.getConfig().getString(
                        "residential.world", ""
                );

        if (!"earthbound".equals(residentialWorld)) {

            plugin.getLogger().warning(
                    "Invalid residential world."
            );

            return false;
        }

        ConfigurationSection homes =
                plugin.getConfig()
                        .getConfigurationSection(
                                "properties.alpha-homes"
                        );

        if (homes == null) {

            plugin.getLogger().warning(
                    "Alpha home configuration missing."
            );

            return false;
        }

        List<ResidentialSite> configuredSites =
                new ArrayList<>();

        for (String propertyId : homes.getKeys(false)) {

            ConfigurationSection home =
                    homes.getConfigurationSection(
                            propertyId
                    );

            if (home == null) {

                return reject(
                        plugin,
                        propertyId,
                        "Invalid property configuration."
                );
            }

            if (!home.getBoolean("enabled", false)) {

                continue;
            }

            if (!residentialWorld.equals(
                    home.getString("world")
            )) {

                return reject(
                        plugin,
                        propertyId,
                        "Incorrect property world."
                );
            }

            if (home.getInt("width") != 24
                    || home.getInt("length") != 32) {

                return reject(
                        plugin,
                        propertyId,
                        "Invalid plot dimensions."
                );
            }

            if (!home.isInt("front-x")
                    || !home.isInt("front-z")
                    || !home.isInt("ground-y")
                    || !home.isInt("min-x")
                    || !home.isInt("min-z")) {

                return reject(
                        plugin,
                        propertyId,
                        "Missing property coordinates."
                );
            }

            int frontX = home.getInt("front-x");
            int frontZ = home.getInt("front-z");
            int groundY = home.getInt("ground-y");

            /*
             * Reject unreasonable coordinates before
             * constructing rotated property boundaries.
             */

            if (Math.abs((long) frontX)
                            > MAXIMUM_WORLD_COORDINATE

                    || Math.abs((long) frontZ)
                            > MAXIMUM_WORLD_COORDINATE) {

                return reject(
                        plugin,
                        propertyId,
                        "Invalid property coordinates."
                );
            }

            if (groundY < MINIMUM_BUILD_HEIGHT
                    || groundY > 280) {

                return reject(
                        plugin,
                        propertyId,
                        "Invalid construction elevation."
                );
            }

            String configuredDesign =
                    home.getString("design", "");

            String facingName =
                    home.getString("facing", "");

            BlockFace facing;

            try {

                facing = BlockFace.valueOf(
                        facingName.trim()
                                .toUpperCase(Locale.ROOT)
                );

            } catch (IllegalArgumentException exception) {

                return reject(
                        plugin,
                        propertyId,
                        "Invalid facing direction."
                );
            }

            /*
             * Current Alpha property ownership uses
             * world-aligned 24 x 32 block boundaries.
             */

            if (facing != BlockFace.NORTH
                    && facing != BlockFace.SOUTH) {

                return reject(
                        plugin,
                        propertyId,
                        "Alpha properties must face "
                        + "north or south."
                );
            }

            ResidentialSite site;

            try {

                site = new ResidentialSite(
                        propertyId,
                        frontX,
                        frontZ,
                        groundY,
                        facing
                );

            } catch (IllegalArgumentException exception) {

                return reject(
                        plugin,
                        propertyId,
                        "Unknown residential property."
                );
            }

            if (!site.getDesign().name().equals(
                    configuredDesign
            )) {

                return reject(
                        plugin,
                        propertyId,
                        "House design does not match property."
                );
            }

            /*
             * Calculate rotated plot boundaries.
             */

            int firstX = site.getWorldX(0, 0);
            int firstZ = site.getWorldZ(0, 0);

            int lastX = site.getWorldX(23, 31);
            int lastZ = site.getWorldZ(23, 31);

            int minX = Math.min(firstX, lastX);
            int maxX = Math.max(firstX, lastX);

            int minZ = Math.min(firstZ, lastZ);
            int maxZ = Math.max(firstZ, lastZ);

            if (home.getInt("min-x") != minX
                    || home.getInt("min-z") != minZ) {

                return reject(
                        plugin,
                        propertyId,
                        "Property boundaries do not "
                        + "match residential coordinates."
                );
            }

            if (maxX - minX + 1 != home.getInt("width")
                    || maxZ - minZ + 1
                            != home.getInt("length")) {

                return reject(
                        plugin,
                        propertyId,
                        "Rotated property dimensions "
                        + "do not match ownership boundaries."
                );
            }

            /*
             * Validate government property price.
             */

            double price = home.getDouble("price", -1);

            if (!Double.isFinite(price)
                    || price <= 0
                    || price > 1000000000.0) {

                return reject(
                        plugin,
                        propertyId,
                        "Invalid property price."
                );
            }

            if (java.math.BigDecimal.valueOf(price)
                    .stripTrailingZeros()
                    .scale() > 2) {

                return reject(
                        plugin,
                        propertyId,
                        "Property price cannot contain "
                        + "fractional cents."
                );
            }

            /*
             * Validate official property sale markers.
             */

            if (!matchesMarker(
                    home,
                    "banner",
                    site.getBannerX(),
                    site.getBannerY(),
                    site.getBannerZ()
            )) {

                return reject(
                        plugin,
                        propertyId,
                        "Invalid property banner coordinates."
                );
            }

            if (!matchesMarker(
                    home,
                    "sign",
                    site.getSignX(),
                    site.getSignY(),
                    site.getSignZ()
            )) {

                return reject(
                        plugin,
                        propertyId,
                        "Invalid property sign coordinates."
                );
            }

            String markerFacing =
                    home.getString("marker-facing", "");

            if (!facing.name().equalsIgnoreCase(
                    markerFacing
            )) {

                return reject(
                        plugin,
                        propertyId,
                        "Sale marker must face the "
                        + "property frontage."
                );
            }

            configuredSites.add(site);
        }

        /*
         * Friends Alpha requires exactly six homes.
         */

        if (configuredSites.size() != MAX_ALPHA_HOMES) {

            plugin.getLogger().warning(
                    "Residential development requires "
                    + "six configured Alpha homes."
            );

            return false;
        }

        /*
         * Register only geographically safe properties.
         */

        if (!registerResidentialSites(configuredSites)) {

            plugin.getLogger().warning(
                    "Residential site safety validation failed."
            );

            return false;
        }

        /*
         * Geographic registration is not construction approval.
         *
         * Construction must remain disabled until the
         * world inspection and installation authorization
         * systems are completed.
         */

        residentialEnabled = false;

        plugin.getLogger().info(
                "Geographically validated residential properties: "
                + getResidentialSiteCount()
        );

        plugin.getLogger().info(
                "Existing chunk inspection is still required."
        );

        plugin.getLogger().info(
                "Construction approved: "
                + CONSTRUCTION_APPROVED
        );

        plugin.getLogger().info(
                "Residential construction enabled: "
                + residentialEnabled
        );

        return true;
    }

    /*
     * ========================================================
     * CONFIGURATION VALIDATION HELPERS
     * ========================================================
     */

    private static boolean matchesMarker(
            ConfigurationSection home,
            String marker,
            int x,
            int y,
            int z
    ) {

        return home.isInt(marker + "-x")
                && home.isInt(marker + "-y")
                && home.isInt(marker + "-z")
                && home.getInt(marker + "-x") == x
                && home.getInt(marker + "-y") == y
                && home.getInt(marker + "-z") == z;
    }

    private static boolean reject(
            EarthBound plugin,
            String propertyId,
            String reason
    ) {

        plugin.getLogger().warning(
                "Residential property rejected: "
                + propertyId + " - " + reason
        );

        clearResidentialSites();

        return false;
    }

    /*
     * ========================================================
     * RESIDENTIAL SITE REGISTRATION
     * ========================================================
     *
     * Registration does not enable construction.
     */

    public static synchronized boolean registerResidentialSites(
            List<ResidentialSite> sites
    ) {

        /*
         * Any registration attempt disables construction.
         */

        clearResidentialSites();

        if (sites == null
                || sites.size() > MAX_ALPHA_HOMES) {

            return false;
        }

        if (!EarthTerrainLoader.isLoaded()
                || !EarthRoadData.isLoaded()
                || !EarthRoadSpatialIndex.isLoaded()) {

            System.out.println(
                    "[EarthBound] Residential registration "
                    + "rejected: geographic data unavailable."
            );

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

                System.out.println(
                        "[EarthBound] Duplicate property: "
                        + site.getPropertyId()
                );

                return false;
            }

            /*
             * NEW:
             *
             * Run the complete geographic safety inspection.
             *
             * Protects:
             *
             * - General Store terrain
             * - Smoothed roads
             * - Road shoulders
             * - Original roads
             * - Water
             * - Property coordinates
             * - Property dimensions
             * - Terrain suitability
             */

            SafetyResult safety;

            try {

                safety =
                        EarthResidentialSafety
                                .inspectGeography(site);

            } catch (RuntimeException exception) {

                System.out.println(
                        "[EarthBound] Residential safety "
                        + "inspection failed for "
                        + site.getPropertyId()
                        + ": "
                        + exception.getMessage()
                );

                return false;
            }

            if (!safety.isSafe()) {

                System.out.println(
                        "[EarthBound] Residential safety "
                        + "rejection: "
                        + safety.getStatus()
                );

                System.out.println(
                        "[EarthBound] "
                        + safety.getMessage()
                );

                return false;
            }

            /*
             * Protect neighboring properties.
             */

            for (ResidentialSite approved
                    : approvedSites) {

                if (site.overlaps(approved)) {

                    System.out.println(
                            "[EarthBound] Property overlap: "
                            + site.getPropertyId()
                            + " and "
                            + approved.getPropertyId()
                    );

                    return false;
                }

                if (!hasPropertyClearance(
                        site,
                        approved
                )) {

                    System.out.println(
                            "[EarthBound] Insufficient "
                            + "property separation: "
                            + site.getPropertyId()
                            + " and "
                            + approved.getPropertyId()
                    );

                    return false;
                }
            }

            approvedSites.add(site);

            System.out.println(
                    "[EarthBound] Geographic safety passed: "
                    + site.getPropertyId()
            );
        }

        /*
         * Publish the complete validated site list.
         *
         * No partially validated property list is published.
         */

        residentialSites =
                List.copyOf(approvedSites);

        return true;
    }

    /*
     * ========================================================
     * RESIDENTIAL PROPERTY SEPARATION
     * ========================================================
     */

    private static boolean hasPropertyClearance(
            ResidentialSite first,
            ResidentialSite second
    ) {

        boolean intersectsProtectedBuffer =

                (long) first.getMinX()
                        <= (long) second.getMaxX()
                                + MINIMUM_PROPERTY_GAP

                && (long) first.getMaxX()
                        >= (long) second.getMinX()
                                - MINIMUM_PROPERTY_GAP

                && (long) first.getMinZ()
                        <= (long) second.getMaxZ()
                                + MINIMUM_PROPERTY_GAP

                && (long) first.getMaxZ()
                        >= (long) second.getMinZ()
                                - MINIMUM_PROPERTY_GAP;

        return !intersectsProtectedBuffer;
    }

    /*
     * ========================================================
     * EXISTING WORLD SAFETY INSPECTION
     * ========================================================
     *
     * Must be called on the server main thread after
     * the EarthBound world becomes available.
     *
     * This method does not enable construction.
     *
     * Construction authorization will be implemented
     * separately after persistent installation tracking.
     */

    public static synchronized boolean inspectRegisteredSites(
            EarthBound plugin,
            World world,
            boolean geographicDataReady
    ) {

        residentialEnabled = false;

        if (plugin == null) {

            return false;
        }

        if (residentialSites.size() != MAX_ALPHA_HOMES) {

            plugin.getLogger().warning(
                    "Residential world inspection skipped: "
                    + "six validated properties are required."
            );

            return false;
        }

        plugin.getLogger().info(
                "Starting residential world safety inspection..."
        );

        try {

            SafetyResult result =
                    EarthResidentialSafety.inspectDevelopment(
                            world,
                            residentialSites,
                            geographicDataReady
                    );

            if (!result.isSafe()) {

                plugin.getLogger().warning(
                        "Residential world inspection failed: "
                        + result.getStatus()
                );

                plugin.getLogger().warning(
                        result.getMessage()
                );

                plugin.getLogger().warning(
                        "Residential construction remains locked."
                );

                return false;
            }

            plugin.getLogger().info(
                    result.getMessage()
            );

            plugin.getLogger().info(
                    "Residential world safety inspection passed."
            );

            plugin.getLogger().info(
                    "Construction remains disabled pending "
                    + "installation authorization."
            );

            return true;

        } catch (RuntimeException exception) {

            plugin.getLogger().log(
                    java.util.logging.Level.WARNING,
                    "Residential world safety inspection failed.",
                    exception
            );

            return false;
        }
    }

    /*
     * ========================================================
     * RESIDENTIAL REGISTRY ACCESS
     * ========================================================
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

        for (ResidentialSite site : residentialSites) {

            if (site.getPropertyId().equals(propertyId)) {

                return site;
            }
        }

        return null;
    }

    public static boolean isResidentialGenerationEnabled() {

        return CONSTRUCTION_APPROVED
                && residentialEnabled;
    }

    public static synchronized void clearResidentialSites() {

        residentialEnabled = false;

        residentialSites = List.of();
    }

    /*
     * ========================================================
     * GUEMES ISLAND RESIDENTIAL GENERATION
     * ========================================================
     */

    public static void generate(
            ChunkData chunkData,
            int chunkX,
            int chunkZ
    ) {

        /*
         * Fail closed while construction is unapproved.
         */

        if (chunkData == null
                || !CONSTRUCTION_APPROVED
                || !residentialEnabled
                || !EarthTerrainLoader.isLoaded()
                || !EarthRoadData.isLoaded()
                || !EarthRoadSpatialIndex.isLoaded()) {

            return;
        }

        List<ResidentialSite> sites =
                residentialSites;

        if (sites.size() != MAX_ALPHA_HOMES) {

            return;
        }

        for (ResidentialSite site : sites) {

            if (!site.intersectsChunk(chunkX, chunkZ)) {

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
     * ========================================================
     * DEVELOPMENT ELEVATION CHECK
     * ========================================================
     */

    public static boolean isSuitableForDevelopment(
            double latitude,
            double longitude
    ) {

        if (!EarthTerrainLoader.isLoaded()
                || !Double.isFinite(latitude)
                || !Double.isFinite(longitude)) {

            return false;
        }

        double elevationMeters =
                EarthTerrainLoader.getGuemesElevation(
                        latitude,
                        longitude
                );

        if (!Double.isFinite(elevationMeters)) {

            return false;
        }

        int minecraftHeight =
                EarthElevation.getMinecraftHeight(
                        elevationMeters
                );

        return minecraftHeight >= MINIMUM_BUILD_HEIGHT;
    }
}
