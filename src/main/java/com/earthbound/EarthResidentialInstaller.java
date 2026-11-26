package com.earthbound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World;

import com.earthbound.EarthAlphaResidentialGenerator
        .ResidentialSite;

import com.earthbound.EarthResidentialInstallation
        .InstallationStatus;

/*
 * ============================================================
 * EARTHBOUND RESIDENTIAL INSTALLER
 * Friends Alpha
 * ============================================================
 *
 * Stage 1: Controlled construction planning.
 *
 * Responsibilities:
 *
 * - Calculate required construction chunks.
 * - Identify shared construction chunks.
 * - Verify approved installation identities.
 * - Prevent duplicate installation planning.
 * - Detect existing generated chunks.
 * - Protect neighboring residential properties.
 * - Prepare immutable construction plans.
 *
 * IMPORTANT:
 *
 * This stage does not generate chunks or modify blocks.
 *
 * Physical construction remains locked until
 * the generator integration is completed.
 * ============================================================
 */

public final class EarthResidentialInstaller {

    private static final String WORLD_NAME =
            "earthbound";

    private static final int REQUIRED_HOMES = 6;

    private static final int CHUNK_SIZE = 16;

    private static final int MAX_CHUNKS_PER_PROPERTY = 16;

    /*
     * Independent physical construction lock.
     */

    private static final boolean
            PHYSICAL_CONSTRUCTION_APPROVED = false;

    private EarthResidentialInstaller() {
    }

    /*
     * ========================================================
     * CONSTRUCTION PLAN STATUS
     * ========================================================
     */

    public enum PlanStatus {

        READY,

        INVALID_PLUGIN,

        MAIN_THREAD_REQUIRED,

        INVALID_WORLD,

        GEOGRAPHIC_DATA_UNAVAILABLE,

        INSTALLATION_STORAGE_UNAVAILABLE,

        ECONOMY_UNAVAILABLE,

        PROPERTY_SYSTEM_UNAVAILABLE,

        INVALID_PROPERTY_COUNT,

        INVALID_PROPERTY,

        DUPLICATE_PROPERTY,

        PROPERTY_ALREADY_OWNED,

        PROPERTY_BOUNDARY_MISMATCH,

        INVALID_INSTALLATION_STATUS,

        INSTALLATION_IDENTITY_MISMATCH,

        UNSAFE_SITE,

        INVALID_CHUNK_COUNT,

        SHARED_CONSTRUCTION_CHUNK,

        EXISTING_LOADED_CHUNK,

        EXISTING_GENERATED_CHUNK,

        PLANNING_FAILED
    }

    /*
     * ========================================================
     * CONSTRUCTION CHUNK
     * ========================================================
     */

    public static final class ConstructionChunk {

        private final int chunkX;

        private final int chunkZ;

        private ConstructionChunk(
                int chunkX,
                int chunkZ
        ) {

            this.chunkX = chunkX;

            this.chunkZ = chunkZ;
        }

        public int getChunkX() {

            return chunkX;
        }

        public int getChunkZ() {

            return chunkZ;
        }

        public long getKey() {

            return ((long) chunkX << 32)
                    | (chunkZ & 0xffffffffL);
        }

        @Override
        public String toString() {

            return chunkX + ", " + chunkZ;
        }
    }

    /*
     * ========================================================
     * INDIVIDUAL PROPERTY CONSTRUCTION PLAN
     * ========================================================
     */

    public static final class PropertyPlan {

        private final ResidentialSite site;

        private final List<ConstructionChunk> chunks;

        private PropertyPlan(
                ResidentialSite site,
                List<ConstructionChunk> chunks
        ) {

            this.site = site;

            this.chunks = List.copyOf(chunks);
        }

        public ResidentialSite getSite() {

            return site;
        }

        public String getPropertyId() {

            return site.getPropertyId();
        }

        public List<ConstructionChunk> getChunks() {

            return chunks;
        }

        public int getChunkCount() {

            return chunks.size();
        }
    }

    /*
     * ========================================================
     * COMPLETE DEVELOPMENT PLAN
     * ========================================================
     */

    public static final class DevelopmentPlan {

        private final UUID worldId;

        private final List<PropertyPlan> properties;

        private DevelopmentPlan(
                UUID worldId,
                List<PropertyPlan> properties
        ) {

            this.worldId = worldId;

            this.properties =
                    List.copyOf(properties);
        }

        public UUID getWorldId() {

            return worldId;
        }

        public List<PropertyPlan> getProperties() {

            return properties;
        }

        public int getPropertyCount() {

            return properties.size();
        }

        public int getTotalChunkCount() {

            int total = 0;

            for (PropertyPlan property : properties) {

                total += property.getChunkCount();
            }

            return total;
        }

        public PropertyPlan getProperty(
                String propertyId
        ) {

            if (propertyId == null) {

                return null;
            }

            for (PropertyPlan property : properties) {

                if (propertyId.equals(
                        property.getPropertyId()
                )) {

                    return property;
                }
            }

            return null;
        }
    }

    /*
     * ========================================================
     * CONSTRUCTION PLANNING RESULT
     * ========================================================
     */

    public static final class PlanResult {

        private final PlanStatus status;

        private final String message;

        private final DevelopmentPlan plan;

        private PlanResult(
                PlanStatus status,
                String message,
                DevelopmentPlan plan
        ) {

            this.status = status;

            this.message = message;

            this.plan = plan;
        }

        public PlanStatus getStatus() {

            return status;
        }

        public String getMessage() {

            return message;
        }

        public DevelopmentPlan getPlan() {

            return plan;
        }

        public boolean isReady() {

            return status == PlanStatus.READY
                    && plan != null;
        }
    }

    /*
     * ========================================================
     * PREPARE RESIDENTIAL DEVELOPMENT
     * ========================================================
     *
     * Read-only planning.
     *
     * Requires persistent approval for all six homes.
     *
     * No chunks are loaded or generated by this method.
     * ========================================================
     */

    public static PlanResult prepareDevelopment(
            EarthBound plugin,
            World world,
            boolean geographicDataReady
    ) {

        if (plugin == null || !plugin.isEnabled()) {

            return reject(
                    PlanStatus.INVALID_PLUGIN,
                    "EarthBound is unavailable."
            );
        }

        if (!Bukkit.isPrimaryThread()) {

            return reject(
                    PlanStatus.MAIN_THREAD_REQUIRED,
                    "Construction planning requires "
                    + "the server main thread."
            );
        }

        if (world == null
                || !WORLD_NAME.equals(world.getName())

                || Bukkit.getWorld(
                        world.getUID()
                ) != world) {

            return reject(
                    PlanStatus.INVALID_WORLD,
                    "Construction planning requires "
                    + "the loaded EarthBound world."
            );
        }

        if (!geographicDataReady) {

            return reject(
                    PlanStatus.GEOGRAPHIC_DATA_UNAVAILABLE,
                    "Geographic data is unavailable."
            );
        }

        if (!EarthResidentialInstallation.isReady()) {

            return reject(
                    PlanStatus
                            .INSTALLATION_STORAGE_UNAVAILABLE,
                    "Installation storage is unavailable."
            );
        }

        if (!EarthEconomy.isReady()) {

            return reject(
                    PlanStatus.ECONOMY_UNAVAILABLE,
                    "EarthBound economy is unavailable."
            );
        }

        if (!EarthPropertyManager.isReady()) {

            return reject(
                    PlanStatus.PROPERTY_SYSTEM_UNAVAILABLE,
                    "Property ownership information "
                    + "is unavailable."
            );
        }

        try {

            List<ResidentialSite> sites =
                    List.copyOf(
                            EarthGuemesGenerator
                                    .getResidentialSites()
                    );

            if (sites.size() != REQUIRED_HOMES) {

                return reject(
                        PlanStatus.INVALID_PROPERTY_COUNT,
                        "Exactly six residential sites "
                        + "are required."
                );
            }

            /*
             * Track every property ID and every
             * construction chunk in the development.
             */

            Set<String> propertyIds =
                    new HashSet<>();

            Map<Long, String> chunkOwners =
                    new HashMap<>();

            List<PropertyPlan> propertyPlans =
                    new ArrayList<>();

            /*
             * Validate the complete development
             * before preparing installation plans.
             */

            EarthResidentialSafety.SafetyResult safety =
                    EarthResidentialSafety
                            .inspectDevelopment(
                                    world,
                                    sites,
                                    geographicDataReady
                            );

            if (!safety.isSafe()) {

                return reject(
                        PlanStatus.UNSAFE_SITE,
                        safety.getMessage()
                );
            }

            for (ResidentialSite site : sites) {

                if (site == null
                        || !isValidPropertyId(
                                site.getPropertyId()
                        )) {

                    return reject(
                            PlanStatus.INVALID_PROPERTY,
                            "Invalid residential property."
                    );
                }

                String propertyId =
                        site.getPropertyId();

                if (!propertyIds.add(propertyId)) {

                    return reject(
                            PlanStatus.DUPLICATE_PROPERTY,
                            "Duplicate residential property: "
                            + propertyId
                    );
                }

                /*
                 * Require explicit persistent approval.
                 */

                InstallationStatus status =
                        EarthResidentialInstallation
                                .getStatus(propertyId);

                if (status != InstallationStatus.APPROVED) {

                    return reject(
                            PlanStatus
                                    .INVALID_INSTALLATION_STATUS,
                            "Property is not available for "
                            + "initial construction: "
                            + propertyId
                            + " ("
                            + status.name()
                            + ")"
                    );
                }

                /*
                 * Verify exact approved installation
                 * coordinates and world identity.
                 */

                if (!EarthResidentialInstallation
                        .matchesApprovedSite(
                                world,
                                site
                        )) {

                    return reject(
                            PlanStatus
                                    .INSTALLATION_IDENTITY_MISMATCH,
                            "Installation identity mismatch: "
                            + propertyId
                    );
                }

                /*
                 * Never plan construction on an
                 * already owned property.
                 */

                if (EarthEconomy
                        .getPropertyOwner(propertyId)
                        != null) {

                    return reject(
                            PlanStatus.PROPERTY_ALREADY_OWNED,
                            "Property already has an owner: "
                            + propertyId
                    );
                }

                /*
                 * Verify current ownership boundaries.
                 */

                PlanResult boundaryCheck =
                        verifyPropertyBoundaries(
                                world,
                                site
                        );

                if (boundaryCheck != null) {

                    return boundaryCheck;
                }

                /*
                 * Calculate every Minecraft chunk
                 * intersecting the property.
                 */

                List<ConstructionChunk> chunks =
                        calculateRequiredChunks(site);

                if (chunks.isEmpty()
                        || chunks.size()
                                > MAX_CHUNKS_PER_PROPERTY) {

                    return reject(
                            PlanStatus.INVALID_CHUNK_COUNT,
                            "Invalid construction chunk count: "
                            + propertyId
                    );
                }

                /*
                 * Detect chunks shared by more
                 * than one residential property.
                 */

                for (ConstructionChunk chunk : chunks) {

                    String previousProperty =
                            chunkOwners.putIfAbsent(
                                    chunk.getKey(),
                                    propertyId
                            );

                    if (previousProperty != null

                            && !previousProperty.equals(
                                    propertyId
                            )) {

                        return reject(
                                PlanStatus
                                        .SHARED_CONSTRUCTION_CHUNK,
                                "Properties "
                                + previousProperty
                                + " and "
                                + propertyId
                                + " share chunk "
                                + chunk
                                + ". Site review required."
                        );
                    }

                    /*
                     * Do not load existing chunks
                     * to inspect their contents.
                     */

                    if (world.isChunkLoaded(
                            chunk.getChunkX(),
                            chunk.getChunkZ()
                    )) {

                        return reject(
                                PlanStatus.EXISTING_LOADED_CHUNK,
                                "Construction chunk already loaded: "
                                + propertyId
                                + " at "
                                + chunk
                        );
                    }

                    if (world.isChunkGenerated(
                            chunk.getChunkX(),
                            chunk.getChunkZ()
                    )) {

                        return reject(
                                PlanStatus.EXISTING_GENERATED_CHUNK,
                                "Construction chunk already generated: "
                                + propertyId
                                + " at "
                                + chunk
                        );
                    }
                }

                propertyPlans.add(
                        new PropertyPlan(
                                site,
                                chunks
                        )
                );
            }

            DevelopmentPlan plan =
                    new DevelopmentPlan(
                            world.getUID(),
                            propertyPlans
                    );

            /*
             * Report the construction plan without
             * activating physical construction.
             */

            reportDevelopmentPlan(
                    plugin,
                    plan
            );

            return new PlanResult(
                    PlanStatus.READY,
                    "Residential construction plan prepared. "
                    + "Physical construction remains locked.",
                    plan
            );

        } catch (RuntimeException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Residential construction planning failed.",
                    exception
            );

            return reject(
                    PlanStatus.PLANNING_FAILED,
                    "Unexpected construction planning failure."
            );
        }
    }

    /*
     * ========================================================
     * VERIFY PROPERTY PROTECTION BOUNDARIES
     * ========================================================
     */

    private static PlanResult verifyPropertyBoundaries(
            World world,
            ResidentialSite site
    ) {

        String propertyId =
                site.getPropertyId();

        for (int x = site.getMinX();
                x <= site.getMaxX();
                x++) {

            for (int z = site.getMinZ();
                    z <= site.getMaxZ();
                    z++) {

                org.bukkit.Location location =
                        new org.bukkit.Location(
                                world,
                                x,
                                site.getGroundY(),
                                z
                        );

                var property =
                        EarthPropertyManager
                                .getPropertyAt(location);

                if (property == null
                        || !propertyId.equals(
                                property.getId()
                        )) {

                    return reject(
                            PlanStatus
                                    .PROPERTY_BOUNDARY_MISMATCH,
                            "Missing or conflicting property "
                            + "protection at "
                            + x
                            + ", "
                            + z
                    );
                }

                if (property.isOwned()) {

                    return reject(
                            PlanStatus.PROPERTY_ALREADY_OWNED,
                            "Property already owned: "
                            + propertyId
                    );
                }

                if (property.getMinX() != site.getMinX()
                        || property.getMaxX() != site.getMaxX()
                        || property.getMinZ() != site.getMinZ()
                        || property.getMaxZ() != site.getMaxZ()) {

                    return reject(
                            PlanStatus
                                    .PROPERTY_BOUNDARY_MISMATCH,
                            "Construction and ownership "
                            + "boundaries differ: "
                            + propertyId
                    );
                }
            }
        }

        /*
         * Null indicates that no boundary
         * or ownership conflict was detected.
         */

        return null;
    }

    /*
     * ========================================================
     * CALCULATE REQUIRED CONSTRUCTION CHUNKS
     * ========================================================
     *
     * Use floor division to handle negative
     * Minecraft coordinates correctly.
     * ========================================================
     */

    public static List<ConstructionChunk>
            calculateRequiredChunks(
                    ResidentialSite site
            ) {

        if (site == null) {

            return List.of();
        }

        long width =
                (long) site.getMaxX()
                        - site.getMinX()
                        + 1L;

        long length =
                (long) site.getMaxZ()
                        - site.getMinZ()
                        + 1L;

        if (width != 24L
                || length != 32L) {

            return List.of();
        }

        int minimumChunkX =
                Math.floorDiv(
                        site.getMinX(),
                        CHUNK_SIZE
                );

        int maximumChunkX =
                Math.floorDiv(
                        site.getMaxX(),
                        CHUNK_SIZE
                );

        int minimumChunkZ =
                Math.floorDiv(
                        site.getMinZ(),
                        CHUNK_SIZE
                );

        int maximumChunkZ =
                Math.floorDiv(
                        site.getMaxZ(),
                        CHUNK_SIZE
                );

        long chunkWidth =
                (long) maximumChunkX
                        - minimumChunkX
                        + 1L;

        long chunkLength =
                (long) maximumChunkZ
                        - minimumChunkZ
                        + 1L;

        long chunkCount =
                chunkWidth * chunkLength;

        if (chunkCount < 1
                || chunkCount
                        > MAX_CHUNKS_PER_PROPERTY) {

            return List.of();
        }

        List<ConstructionChunk> chunks =
                new ArrayList<>();

        for (int chunkX = minimumChunkX;
                chunkX <= maximumChunkX;
                chunkX++) {

            for (int chunkZ = minimumChunkZ;
                    chunkZ <= maximumChunkZ;
                    chunkZ++) {

                chunks.add(
                        new ConstructionChunk(
                                chunkX,
                                chunkZ
                        )
                );
            }
        }

        return List.copyOf(chunks);
    }

    /*
     * ========================================================
     * RECHECK DEVELOPMENT PLAN
     * ========================================================
     *
     * A saved in-memory plan is only a snapshot.
     *
     * Rebuild and compare the complete plan before
     * accepting it for any future construction.
     * ========================================================
     */

    public static boolean isPlanStillValid(
            EarthBound plugin,
            World world,
            DevelopmentPlan previousPlan,
            boolean geographicDataReady
    ) {

        if (world == null
                || !Bukkit.isPrimaryThread()
                || previousPlan == null

                || !world.getUID().equals(
                        previousPlan.getWorldId()
                )) {

            return false;
        }

        PlanResult current =
                prepareDevelopment(
                        plugin,
                        world,
                        geographicDataReady
                );

        if (!current.isReady()) {

            return false;
        }

        DevelopmentPlan updated =
                current.getPlan();

        if (previousPlan.getPropertyCount()
                    != updated.getPropertyCount()

                || previousPlan.getTotalChunkCount()
                    != updated.getTotalChunkCount()) {

            return false;
        }

        for (PropertyPlan previous
                : previousPlan.getProperties()) {

            PropertyPlan replacement =
                    updated.getProperty(
                            previous.getPropertyId()
                    );

            if (replacement == null) {

                return false;
            }

            if (!sameSite(
                    previous.getSite(),
                    replacement.getSite()
            )) {

                return false;
            }

            if (!sameChunks(
                    previous.getChunks(),
                    replacement.getChunks()
            )) {

                return false;
            }
        }

        return true;
    }

    /*
     * ========================================================
     * COMPARE RESIDENTIAL SITE IDENTITIES
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
     * COMPARE CONSTRUCTION CHUNKS
     * ========================================================
     */

    private static boolean sameChunks(
            List<ConstructionChunk> first,
            List<ConstructionChunk> second
    ) {

        if (first.size() != second.size()) {

            return false;
        }

        Set<Long> firstKeys =
                new HashSet<>();

        Set<Long> secondKeys =
                new HashSet<>();

        for (ConstructionChunk chunk : first) {

            firstKeys.add(
                    chunk.getKey()
            );
        }

        for (ConstructionChunk chunk : second) {

            secondKeys.add(
                    chunk.getKey()
            );
        }

        return firstKeys.equals(secondKeys);
    }

    /*
     * ========================================================
     * REPORT CONSTRUCTION PLAN
     * ========================================================
     */

    private static void reportDevelopmentPlan(
            EarthBound plugin,
            DevelopmentPlan plan
    ) {

        plugin.getLogger().info(
                "========================================"
        );

        plugin.getLogger().info(
                "EARTHBOUND RESIDENTIAL CONSTRUCTION PLAN"
        );

        plugin.getLogger().info(
                "World UUID: "
                + plan.getWorldId()
        );

        plugin.getLogger().info(
                "Residential properties: "
                + plan.getPropertyCount()
        );

        plugin.getLogger().info(
                "Required construction chunks: "
                + plan.getTotalChunkCount()
        );

        for (PropertyPlan property
                : plan.getProperties()) {

            plugin.getLogger().info(
                    "----------------------------------------"
            );

            plugin.getLogger().info(
                    "Property: "
                    + property.getPropertyId()
            );

            plugin.getLogger().info(
                    "House design: "
                    + property.getSite()
                            .getDesign().name()
            );

            plugin.getLogger().info(
                    "Required chunks: "
                    + property.getChunkCount()
            );

            for (ConstructionChunk chunk
                    : property.getChunks()) {

                plugin.getLogger().info(
                        "Construction chunk: "
                        + chunk
                );
            }
        }

        plugin.getLogger().info(
                "Shared construction chunks: NONE"
        );

        plugin.getLogger().info(
                "Physical construction: LOCKED"
        );

        plugin.getLogger().info(
                "No blocks were modified."
        );

        plugin.getLogger().info(
                "========================================"
        );
    }

    /*
     * ========================================================
     * PHYSICAL CONSTRUCTION STATUS
     * ========================================================
     */

    public static boolean
            isPhysicalConstructionEnabled() {

        return PHYSICAL_CONSTRUCTION_APPROVED;
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
     * REJECT CONSTRUCTION PLAN
     * ========================================================
     */

    private static PlanResult reject(
            PlanStatus status,
            String message
    ) {

        return new PlanResult(
                status,
                message,
                null
        );
    }
}
