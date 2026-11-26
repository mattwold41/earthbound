
package com.earthbound;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
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

import com.earthbound.EarthResidentialInstallation
        .InstallationStatus;

import com.earthbound.EarthResidentialInstaller
        .ConstructionChunk;

import com.earthbound.EarthResidentialInstaller
        .PropertyPlan;

/*
 * ============================================================
 * EARTHBOUND GUEMES RESIDENTIAL GENERATOR
 * Friends Alpha
 * ============================================================
 *
 * Geographic registration and controlled construction.
 *
 * Registration alone never enables construction.
 *
 * Future construction requires an individually authorized
 * property, matching world identity, approved chunk list
 * and persistent installation start record.
 *
 * Physical construction remains locked.
 * ============================================================
 */

public final class EarthGuemesGenerator {

    private static final int MINIMUM_BUILD_HEIGHT =
            EarthWaterData.SEA_LEVEL + 3;

    private static final int MAX_ALPHA_HOMES = 6;

    private static final int MINIMUM_PROPERTY_GAP = 8;

    private static final int MAXIMUM_WORLD_COORDINATE =
            29999900;

    /*
     * Independent generator construction lock.
     *
     * Do not enable until installer execution and
     * completed structure verification are implemented.
     */

    private static final boolean CONSTRUCTION_APPROVED =
            false;

    private static volatile List<ResidentialSite>
            residentialSites = List.of();

    /*
     * Protect construction permit access and residential
     * block generation against concurrent callbacks.
     */

    private static final Object CONSTRUCTION_LOCK =
            new Object();

    private static ConstructionPermit activePermit;

    private EarthGuemesGenerator() {
    }

    /*
     * ========================================================
     * TEMPORARY CONSTRUCTION PERMIT
     * ========================================================
     *
     * Permits are never persisted.
     *
     * A server restart must never automatically reactivate
     * an interrupted residential construction operation.
     *
     * All mutable permit fields are protected by
     * CONSTRUCTION_LOCK.
     * ========================================================
     */

    private static final class ConstructionPermit {

        private final UUID worldId;

        private final ResidentialSite site;

        private final Set<Long> requiredChunks;

        private final Set<Long> processedChunks =
                new HashSet<>();

        private boolean failed;

        private ConstructionPermit(
                UUID worldId,
                ResidentialSite site,
                Set<Long> requiredChunks
        ) {

            this.worldId = worldId;
            this.site = site;

            this.requiredChunks =
                    Set.copyOf(requiredChunks);
        }
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

        if (plugin == null) {

            return false;
        }

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

            if (!isValidPropertyId(propertyId)) {

                return reject(
                        plugin,
                        propertyId,
                        "Invalid Alpha property ID."
                );
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
             * Alpha ownership currently uses world-aligned
             * 24 x 32 block boundaries.
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
                        "Sale marker must face "
                        + "the property frontage."
                );
            }

            configuredSites.add(site);
        }

        if (configuredSites.size() != MAX_ALPHA_HOMES) {

            plugin.getLogger().warning(
                    "Residential development requires "
                    + "six configured Alpha homes."
            );

            return false;
        }

        if (!registerResidentialSites(configuredSites)) {

            plugin.getLogger().warning(
                    "Residential site safety validation failed."
            );

            return false;
        }

        plugin.getLogger().info(
                "Geographically validated residential properties: "
                + getResidentialSiteCount()
        );

        plugin.getLogger().info(
                "Temporary Alpha chunk separation: VERIFIED"
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
                + isResidentialGenerationEnabled()
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
     * Registration always withdraws existing permits.
     *
     * Registration never authorizes construction.
     * ========================================================
     */

    public static synchronized boolean registerResidentialSites(
            List<ResidentialSite> sites
    ) {

        clearResidentialSites();

        if (sites == null
                || sites.size() != MAX_ALPHA_HOMES) {

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

            if (site == null
                    || !isValidPropertyId(
                            site.getPropertyId()
                    )) {

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
             * Preserve complete geographic safety inspection.
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
             * Preserve existing property separation.
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
         * Independently enforce temporary Friends Alpha
         * property, corridor and chunk separation.
         */

        if (!EarthAlphaSiteFinder
                .hasDevelopmentChunkClearance(
                        approvedSites
                )) {

            System.out.println(
                    "[EarthBound] Residential registration "
                    + "rejected: unsafe Alpha chunk separation."
            );

            return false;
        }

        /*
         * Publish only the complete validated registry.
         */

        synchronized (CONSTRUCTION_LOCK) {

            activePermit = null;

            residentialSites =
                    List.copyOf(approvedSites);
        }

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
     */

    public static synchronized boolean inspectRegisteredSites(
            EarthBound plugin,
            World world,
            boolean geographicDataReady
    ) {

        revokeConstructionPermit();

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

            if (!EarthAlphaSiteFinder
                    .hasDevelopmentChunkClearance(
                            residentialSites
                    )) {

                plugin.getLogger().warning(
                        "Residential world inspection rejected: "
                        + "unsafe Alpha chunk separation."
                );

                return false;
            }

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
                    Level.WARNING,
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

        synchronized (CONSTRUCTION_LOCK) {

            return CONSTRUCTION_APPROVED

                    && EarthResidentialInstaller
                            .isPhysicalConstructionEnabled()

                    && activePermit != null

                    && !activePermit.failed

                    && activePermit.processedChunks.size()
                            < activePermit.requiredChunks.size();
        }
    }

    public static synchronized void clearResidentialSites() {

        synchronized (CONSTRUCTION_LOCK) {

            activePermit = null;

            residentialSites = List.of();
        }
    }

    /*
     * ========================================================
     * ISSUE TEMPORARY CONSTRUCTION PERMIT
     * ========================================================
     *
     * Intended exclusively for the future controlled
     * installer.
     *
     * Must be called on the server main thread after
     * persistent IN_PROGRESS status has been recorded.
     *
     * Does not generate or load chunks.
     *
     * Does not replace administrator authorization.
     *
     * Both independent physical construction locks
     * must be enabled before a permit can be issued.
     * ========================================================
     */

    static boolean issueConstructionPermit(
            EarthBound plugin,
            World world,
            PropertyPlan propertyPlan,
            boolean geographicDataReady
    ) {

        if (!CONSTRUCTION_APPROVED

                || !EarthResidentialInstaller
                        .isPhysicalConstructionEnabled()) {

            return false;
        }

        if (plugin == null
                || !plugin.isEnabled()
                || !Bukkit.isPrimaryThread()) {

            return false;
        }

        if (world == null
                || !"earthbound".equals(world.getName())

                || Bukkit.getWorld(world.getUID()) != world

                || propertyPlan == null) {

            return false;
        }

        if (!geographicDataReady
                || !EarthTerrainLoader.isLoaded()
                || !EarthRoadData.isLoaded()
                || !EarthRoadSpatialIndex.isLoaded()) {

            return false;
        }

        if (!EarthResidentialAuthorization
                .isAuthorizationEnabled(plugin)

                || !EarthResidentialInstallation.isReady()

                || !EarthEconomy.isReady()

                || !EarthPropertyManager.isReady()) {

            return false;
        }

        try {

            synchronized (CONSTRUCTION_LOCK) {

                /*
                 * Never replace an existing permit.
                 */

                if (activePermit != null) {

                    return false;
                }

                List<ResidentialSite> sites =
                        residentialSites;

                if (sites.size() != MAX_ALPHA_HOMES

                        || !EarthAlphaSiteFinder
                                .hasDevelopmentChunkClearance(
                                        sites
                                )) {

                    return false;
                }

                String propertyId =
                        propertyPlan.getPropertyId();

                if (!isValidPropertyId(propertyId)) {

                    return false;
                }

                ResidentialSite site =
                        getResidentialSite(propertyId);

                if (!sameSite(
                        site,
                        propertyPlan.getSite()
                )) {

                    return false;
                }

                /*
                 * Require exactly one active installation.
                 */

                for (ResidentialSite registeredSite : sites) {

                    String registeredId =
                            registeredSite.getPropertyId();

                    if (!EarthResidentialInstallation
                            .matchesApprovedSite(
                                    world,
                                    registeredSite
                            )) {

                        return false;
                    }

                    InstallationStatus status =
                            EarthResidentialInstallation
                                    .getStatus(registeredId);

                    if (propertyId.equals(registeredId)) {

                        if (status
                                != InstallationStatus.IN_PROGRESS) {

                            return false;
                        }

                    } else if (

                            status != InstallationStatus.APPROVED

                            && status
                                != InstallationStatus.COMPLETED) {

                        return false;
                    }

                    /*
                     * Completed sites may have owners.
                     * Unfinished sites must remain unowned.
                     */

                    if (status != InstallationStatus.COMPLETED

                            && EarthEconomy.getPropertyOwner(
                                    registeredId
                            ) != null) {

                        return false;
                    }

                    /*
                     * Recheck all unfinished sites without
                     * loading or generating their chunks.
                     */

                    if (status != InstallationStatus.COMPLETED) {

                        SafetyResult safety =
                                EarthResidentialSafety.inspectSite(
                                        world,
                                        registeredSite,
                                        geographicDataReady
                                );

                        if (!safety.isSafe()) {

                            return false;
                        }
                    }
                }

                /*
                 * Recalculate the permitted chunk set.
                 */

                List<ConstructionChunk> required =
                        EarthResidentialInstaller
                                .calculateRequiredChunks(site);

                if (required.isEmpty()

                        || required.size()
                            != propertyPlan.getChunkCount()) {

                    return false;
                }

                Set<Long> expectedKeys =
                        new HashSet<>();

                Set<Long> plannedKeys =
                        new HashSet<>();

                for (ConstructionChunk chunk : required) {

                    expectedKeys.add(chunk.getKey());

                    if (world.isChunkLoaded(
                            chunk.getChunkX(),
                            chunk.getChunkZ()
                    )) {

                        return false;
                    }

                    if (world.isChunkGenerated(
                            chunk.getChunkX(),
                            chunk.getChunkZ()
                    )) {

                        return false;
                    }
                }

                for (ConstructionChunk chunk
                        : propertyPlan.getChunks()) {

                    plannedKeys.add(chunk.getKey());
                }

                if (expectedKeys.size() != required.size()

                        || plannedKeys.size()
                            != propertyPlan.getChunkCount()

                        || !expectedKeys.equals(plannedKeys)) {

                    return false;
                }

                /*
                 * Publish the individually scoped permit.
                 */

                activePermit = new ConstructionPermit(
                        world.getUID(),
                        site,
                        expectedKeys
                );

                return true;
            }

        } catch (RuntimeException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Residential construction permit failed.",
                    exception
            );

            return false;
        }
    }

    /*
     * ========================================================
     * CONSTRUCTION PERMIT REVOCATION
     * ========================================================
     *
     * Waits for any residential generation section
     * currently holding CONSTRUCTION_LOCK.
     *
     * Does not change persistent installation records.
     *
     * The future installer must record failures and
     * handle any partially generated construction.
     * ========================================================
     */

    static void revokeConstructionPermit() {

        synchronized (CONSTRUCTION_LOCK) {

            activePermit = null;
        }
    }

    /*
     * ========================================================
     * CONSTRUCTION CALLBACK PROGRESS
     * ========================================================
     *
     * Counts generator callbacks that returned successfully.
     *
     * This does not prove that chunks were committed to
     * disk or that a completed house is structurally valid.
     * ========================================================
     */

    static int getProcessedConstructionChunkCount(
            UUID worldId,
            String propertyId
    ) {

        synchronized (CONSTRUCTION_LOCK) {

            if (!matchesActivePermit(
                    worldId,
                    propertyId
            )) {

                return 0;
            }

            return activePermit.processedChunks.size();
        }
    }

    static boolean hasProcessedAllConstructionChunks(
            UUID worldId,
            String propertyId
    ) {

        synchronized (CONSTRUCTION_LOCK) {

            if (!matchesActivePermit(
                    worldId,
                    propertyId
            )) {

                return false;
            }

            return !activePermit.failed

                    && activePermit.processedChunks.equals(
                            activePermit.requiredChunks
                    );
        }
    }

    static boolean hasConstructionFailed(
            UUID worldId,
            String propertyId
    ) {

        synchronized (CONSTRUCTION_LOCK) {

            /*
             * A missing or mismatched permit is not a
             * valid active construction operation.
             */

            return !matchesActivePermit(
                    worldId,
                    propertyId
            )

                    || activePermit.failed;
        }
    }

    private static boolean matchesActivePermit(
            UUID worldId,
            String propertyId
    ) {

        return activePermit != null

                && worldId != null

                && propertyId != null

                && activePermit.worldId.equals(worldId)

                && activePermit.site.getPropertyId()
                        .equals(propertyId);
    }

    /*
     * ========================================================
     * LEGACY GENERATION ENTRY POINT
     * ========================================================
     *
     * Preserved for compatibility with EarthGenerator.
     *
     * Calls without explicit world identity must never
     * generate residential structures.
     *
     * EarthGenerator will be integrated separately.
     * ========================================================
     */

    public static void generate(
            ChunkData chunkData,
            int chunkX,
            int chunkZ
    ) {

        /*
         * Intentionally disabled.
         */
    }

    /*
     * ========================================================
     * CONTROLLED RESIDENTIAL GENERATION
     * ========================================================
     *
     * May be called by a chunk generation worker.
     *
     * Does not access configuration, property ownership
     * storage or Bukkit world-loading methods.
     *
     * Uses only the scoped construction permit and
     * current geographic data readiness.
     *
     * All permit validation and residential generation
     * are serialized under CONSTRUCTION_LOCK.
     * ========================================================
     */

    public static void generate(
            UUID worldId,
            ChunkData chunkData,
            int chunkX,
            int chunkZ
    ) {

        if (!CONSTRUCTION_APPROVED

                || !EarthResidentialInstaller
                        .isPhysicalConstructionEnabled()

                || worldId == null

                || chunkData == null) {

            return;
        }

        synchronized (CONSTRUCTION_LOCK) {

            ConstructionPermit permit = activePermit;

            if (permit == null
                    || permit.failed

                    || !permit.worldId.equals(worldId)) {

                return;
            }

            long chunkKey =
                    getChunkKey(chunkX, chunkZ);

            /*
             * Reject any chunk outside the exact
             * authorized construction footprint.
             */

            if (!permit.requiredChunks.contains(chunkKey)) {

                return;
            }

            /*
             * A duplicate callback is unexpected.
             *
             * Suspend the permit rather than assume the
             * first callback was successfully committed.
             */

            if (permit.processedChunks.contains(chunkKey)) {

                permit.failed = true;

                return;
            }

            if (!EarthTerrainLoader.isLoaded()

                    || !EarthRoadData.isLoaded()

                    || !EarthRoadSpatialIndex.isLoaded()

                    || residentialSites.size() != MAX_ALPHA_HOMES

                    || getResidentialSite(
                            permit.site.getPropertyId()
                    ) != permit.site

                    || !permit.site.intersectsChunk(
                            chunkX,
                            chunkZ
                    )) {

                permit.failed = true;

                return;
            }

            /*
             * Guard the vertical construction envelope.
             */

            int groundY = permit.site.getGroundY();

            int maximumY =
                    groundY

                    + permit.site.getDesign()
                            .getMaximumHeight()

                    + 12;

            if (groundY - 12 < chunkData.getMinHeight()

                    || maximumY >= chunkData.getMaxHeight()) {

                permit.failed = true;

                return;
            }

            try {

                /*
                 * Generate only the individually
                 * permitted residential property.
                 */

                EarthAlphaResidentialGenerator.generate(
                        chunkData,
                        chunkX,
                        chunkZ,
                        permit.site
                );

                permit.processedChunks.add(chunkKey);

            } catch (RuntimeException | Error exception) {

                /*
                 * Suspend all further residential callbacks.
                 *
                 * Propagate the generation failure.
                 * Do not mark the installation COMPLETED.
                 */

                permit.failed = true;

                throw exception;
            }
        }
    }

    /*
     * ========================================================
     * CHUNK IDENTITY
     * ========================================================
     */

    private static long getChunkKey(
            int chunkX,
            int chunkZ
    ) {

        return ((long) chunkX << 32)

                | (chunkZ & 0xffffffffL);
    }

    /*
     * ========================================================
     * COMPARE EXACT RESIDENTIAL SITE IDENTITIES
     * ========================================================
     */

    private static boolean sameSite(
            ResidentialSite first,
            ResidentialSite second
    ) {

        return first != null

                && second != null

                && first.getPropertyId().equals(
                        second.getPropertyId()
                )

                && first.getDesign() == second.getDesign()

                && first.getFacing() == second.getFacing()

                && first.getGroundY()
                        == second.getGroundY()

                && first.getWorldX(0, 0)
                        == second.getWorldX(0, 0)

                && first.getWorldZ(0, 0)
                        == second.getWorldZ(0, 0)

                && first.getMinX()
                        == second.getMinX()

                && first.getMaxX()
                        == second.getMaxX()

                && first.getMinZ()
                        == second.getMinZ()

                && first.getMaxZ()
                        == second.getMaxZ();
    }

    /*
     * ========================================================
     * PROPERTY ID VALIDATION
     * ========================================================
     */

    private static boolean isValidPropertyId(
            String propertyId
    ) {

        return propertyId != null

                && propertyId.matches(
                        "guemes_home_0[1-6]"
                );
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
