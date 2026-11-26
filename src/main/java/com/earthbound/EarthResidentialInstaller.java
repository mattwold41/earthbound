
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
import org.bukkit.Location;
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
 * Controlled construction planning and execution preflight.
 *
 * Responsibilities:
 *
 * - Calculate required construction chunks.
 * - Prevent shared construction chunks.
 * - Enforce temporary Alpha chunk separation.
 * - Verify approved installation identities.
 * - Verify current ownership and property boundaries.
 * - Detect existing generated chunks.
 * - Protect neighboring residential properties.
 * - Prepare immutable construction plans.
 * - Revalidate plans before future execution.
 * - Inspect individual installation readiness.
 *
 * IMPORTANT:
 *
 * No methods in this class currently generate chunks,
 * modify blocks or change installation records.
 *
 * Physical construction remains locked.
 * ============================================================
 */

public final class EarthResidentialInstaller {

    private static final String WORLD_NAME = "earthbound";

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

        UNSAFE_CHUNK_SEPARATION,
        INVALID_DEVELOPMENT_PLAN,
        STALE_DEVELOPMENT_PLAN,
        PROPERTY_NOT_IN_PLAN,
        AUTHORIZATION_DISABLED,
        PHYSICAL_CONSTRUCTION_LOCKED,

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

            this.properties = List.copyOf(properties);
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
     * INDIVIDUAL INSTALLATION PREFLIGHT RESULT
     * ========================================================
     */

    public static final class PreflightResult {

        private final PlanStatus status;

        private final String message;

        private final PropertyPlan property;

        private PreflightResult(
                PlanStatus status,
                String message,
                PropertyPlan property
        ) {

            this.status = status;
            this.message = message;
            this.property = property;
        }

        public PlanStatus getStatus() {

            return status;
        }

        public String getMessage() {

            return message;
        }

        public PropertyPlan getProperty() {

            return property;
        }

        /*
         * A successful preflight is not construction
         * authorization and does not reserve chunks.
         */

        public boolean isReady() {

            return status == PlanStatus.READY
                    && property != null;
        }
    }

    /*
     * ========================================================
     * PREPARE RESIDENTIAL DEVELOPMENT
     * ========================================================
     *
     * Read-only initial planning.
     *
     * Requires persistent approval for all six homes.
     *
     * No chunks are loaded or generated.
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
                    PlanStatus.INSTALLATION_STORAGE_UNAVAILABLE,
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
             * Validate all geographic and existing
             * chunk safety requirements.
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

            /*
             * Track every property and construction chunk.
             */

            Set<String> propertyIds = new HashSet<>();

            Map<Long, String> chunkOwners = new HashMap<>();

            List<PropertyPlan> propertyPlans =
                    new ArrayList<>();

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
                            PlanStatus.INVALID_INSTALLATION_STATUS,
                            "Property is not available for "
                            + "initial construction: "
                            + propertyId
                            + " ("
                            + status.name()
                            + ")"
                    );
                }

                /*
                 * Verify exact approved location
                 * and world identity.
                 */

                if (!EarthResidentialInstallation
                        .matchesApprovedSite(
                                world,
                                site
                        )) {

                    return reject(
                            PlanStatus.INSTALLATION_IDENTITY_MISMATCH,
                            "Installation identity mismatch: "
                            + propertyId
                    );
                }

                /*
                 * Never construct on an owned property.
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

                PlanResult boundaryCheck =
                        verifyPropertyBoundaries(
                                world,
                                site
                        );

                if (boundaryCheck != null) {

                    return boundaryCheck;
                }

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
                                PlanStatus.SHARED_CONSTRUCTION_CHUNK,
                                "Properties "
                                + previousProperty
                                + " and "
                                + propertyId
                                + " share chunk "
                                + chunk
                                + ". Site review required."
                        );
                    }

                    PlanResult chunkCheck =
                            inspectConstructionChunk(
                                    world,
                                    propertyId,
                                    chunk
                            );

                    if (chunkCheck != null) {

                        return chunkCheck;
                    }
                }

                propertyPlans.add(
                        new PropertyPlan(
                                site,
                                chunks
                        )
                );
            }

            /*
             * INDEPENDENT FRIENDS ALPHA SAFETY CHECK
             *
             * Do not rely on the authorization system
             * having previously checked chunk separation.
             *
             * Enforce the same temporary requirements
             * directly within construction planning.
             */

            if (!EarthAlphaSiteFinder
                    .hasDevelopmentChunkClearance(sites)) {

                return reject(
                        PlanStatus.UNSAFE_CHUNK_SEPARATION,
                        "Development violates temporary Alpha "
                        + "property or construction chunk separation."
                );
            }

            DevelopmentPlan plan =
                    new DevelopmentPlan(
                            world.getUID(),
                            propertyPlans
                    );

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
     * INSPECT CONSTRUCTION CHUNK
     * ========================================================
     *
     * Read-only inspection.
     *
     * Never load chunks to determine whether
     * they are safe for construction.
     * ========================================================
     */

    private static PlanResult inspectConstructionChunk(
            World world,
            String propertyId,
            ConstructionChunk chunk
    ) {

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

        return null;
    }

    /*
     * ========================================================
     * INDIVIDUAL PROPERTY EXECUTION PREFLIGHT
     * ========================================================
     *
     * Inspects a requested property against the original
     * approved development plan.
     *
     * Supports sequential installation planning:
     *
     * - APPROVED properties must remain ungenerated.
     * - COMPLETED properties retain their original identities.
     * - IN_PROGRESS and FAILED properties suspend execution.
     *
     * Does not authorize construction.
     * Does not change installation records.
     * Does not load chunks or place blocks.
     * ========================================================
     */

    public static PreflightResult preflightInstallation(
            EarthBound plugin,
            World world,
            DevelopmentPlan developmentPlan,
            String propertyId,
            boolean geographicDataReady
    ) {

        if (plugin == null || !plugin.isEnabled()) {

            return rejectPreflight(
                    PlanStatus.INVALID_PLUGIN,
                    "EarthBound is unavailable."
            );
        }

        if (!Bukkit.isPrimaryThread()) {

            return rejectPreflight(
                    PlanStatus.MAIN_THREAD_REQUIRED,
                    "Installation preflight requires "
                    + "the server main thread."
            );
        }

        if (world == null
                || !WORLD_NAME.equals(world.getName())
                || Bukkit.getWorld(world.getUID()) != world) {

            return rejectPreflight(
                    PlanStatus.INVALID_WORLD,
                    "Invalid EarthBound world."
            );
        }

        if (developmentPlan == null
                || developmentPlan.getPropertyCount()
                        != REQUIRED_HOMES

                || !world.getUID().equals(
                        developmentPlan.getWorldId()
                )) {

            return rejectPreflight(
                    PlanStatus.INVALID_DEVELOPMENT_PLAN,
                    "Missing or invalid development plan."
            );
        }

        if (!isValidPropertyId(propertyId)) {

            return rejectPreflight(
                    PlanStatus.INVALID_PROPERTY,
                    "Invalid residential property ID."
            );
        }

        if (!geographicDataReady) {

            return rejectPreflight(
                    PlanStatus.GEOGRAPHIC_DATA_UNAVAILABLE,
                    "Geographic data is unavailable."
            );
        }

        if (!EarthResidentialInstallation.isReady()) {

            return rejectPreflight(
                    PlanStatus.INSTALLATION_STORAGE_UNAVAILABLE,
                    "Installation storage is unavailable."
            );
        }

        if (!EarthEconomy.isReady()) {

            return rejectPreflight(
                    PlanStatus.ECONOMY_UNAVAILABLE,
                    "EarthBound economy is unavailable."
            );
        }

        if (!EarthPropertyManager.isReady()) {

            return rejectPreflight(
                    PlanStatus.PROPERTY_SYSTEM_UNAVAILABLE,
                    "Property ownership information "
                    + "is unavailable."
            );
        }

        try {

            PropertyPlan requested =
                    developmentPlan.getProperty(
                            propertyId
                    );

            if (requested == null) {

                return rejectPreflight(
                        PlanStatus.PROPERTY_NOT_IN_PLAN,
                        "Requested property is not in "
                        + "the development plan."
                );
            }

            List<ResidentialSite> sites =
                    List.copyOf(
                            EarthGuemesGenerator
                                    .getResidentialSites()
                    );

            if (sites.size() != REQUIRED_HOMES) {

                return rejectPreflight(
                        PlanStatus.INVALID_PROPERTY_COUNT,
                        "Exactly six residential sites "
                        + "must remain registered."
                );
            }

            /*
             * Recheck temporary Alpha chunk separation
             * across the entire development.
             */

            if (!EarthAlphaSiteFinder
                    .hasDevelopmentChunkClearance(sites)) {

                return rejectPreflight(
                        PlanStatus.UNSAFE_CHUNK_SEPARATION,
                        "Development chunk separation "
                        + "verification failed."
                );
            }

            Set<String> checkedIds = new HashSet<>();

            for (ResidentialSite site : sites) {

                if (site == null
                        || !isValidPropertyId(
                                site.getPropertyId()
                        )) {

                    return rejectPreflight(
                            PlanStatus.INVALID_PROPERTY,
                            "Invalid registered residential site."
                    );
                }

                String currentId =
                        site.getPropertyId();

                if (!checkedIds.add(currentId)) {

                    return rejectPreflight(
                            PlanStatus.DUPLICATE_PROPERTY,
                            "Duplicate residential property: "
                            + currentId
                    );
                }

                PropertyPlan original =
                        developmentPlan.getProperty(
                                currentId
                        );

                /*
                 * Compare the current configuration
                 * against the original construction plan.
                 */

                if (original == null
                        || !sameSite(
                                original.getSite(),
                                site
                        )

                        || !sameChunks(
                                original.getChunks(),
                                calculateRequiredChunks(site)
                        )) {

                    return rejectPreflight(
                            PlanStatus.STALE_DEVELOPMENT_PLAN,
                            "Construction plan has changed: "
                            + currentId
                    );
                }

                if (!EarthResidentialInstallation
                        .matchesApprovedSite(
                                world,
                                site
                        )) {

                    return rejectPreflight(
                            PlanStatus.INSTALLATION_IDENTITY_MISMATCH,
                            "Saved installation identity mismatch: "
                            + currentId
                    );
                }

                InstallationStatus status =
                        EarthResidentialInstallation
                                .getStatus(currentId);

                /*
                 * Previously completed properties
                 * must never be constructed again.
                 */

                if (status == InstallationStatus.COMPLETED) {

                    if (propertyId.equals(currentId)) {

                        return rejectPreflight(
                                PlanStatus.INVALID_INSTALLATION_STATUS,
                                "Requested property is already completed."
                        );
                    }

                    continue;
                }

                /*
                 * Interrupted, failed or unapproved
                 * installations suspend the development.
                 */

                if (status != InstallationStatus.APPROVED) {

                    return rejectPreflight(
                            PlanStatus.INVALID_INSTALLATION_STATUS,
                            "Unresolved installation: "
                            + currentId
                            + " ("
                            + status.name()
                            + ")"
                    );
                }

                /*
                 * Every unfinished property must
                 * remain unowned and safe.
                 */

                if (EarthEconomy
                        .getPropertyOwner(currentId)
                        != null) {

                    return rejectPreflight(
                            PlanStatus.PROPERTY_ALREADY_OWNED,
                            "Unfinished property has an owner: "
                            + currentId
                    );
                }

                PlanResult boundaryCheck =
                        verifyPropertyBoundaries(
                                world,
                                site
                        );

                if (boundaryCheck != null) {

                    return rejectPreflight(
                            boundaryCheck.getStatus(),
                            boundaryCheck.getMessage()
                    );
                }

                EarthResidentialSafety.SafetyResult safety =
                        EarthResidentialSafety.inspectSite(
                                world,
                                site,
                                geographicDataReady
                        );

                if (!safety.isSafe()) {

                    return rejectPreflight(
                            PlanStatus.UNSAFE_SITE,
                            currentId
                            + ": "
                            + safety.getMessage()
                    );
                }

                for (ConstructionChunk chunk
                        : original.getChunks()) {

                    PlanResult chunkCheck =
                            inspectConstructionChunk(
                                    world,
                                    currentId,
                                    chunk
                            );

                    if (chunkCheck != null) {

                        return rejectPreflight(
                                chunkCheck.getStatus(),
                                chunkCheck.getMessage()
                        );
                    }
                }
            }

            return new PreflightResult(
                    PlanStatus.READY,
                    "Read-only installation preflight passed: "
                    + propertyId
                    + ". No construction was authorized.",
                    requested
            );

        } catch (RuntimeException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Residential installation preflight failed.",
                    exception
            );

            return rejectPreflight(
                    PlanStatus.PLANNING_FAILED,
                    "Unexpected installation preflight failure."
            );
        }
    }

    /*
     * ========================================================
     * CHECK PHYSICAL EXECUTION READINESS
     * ========================================================
     *
     * Separate physical construction safety gate.
     *
     * Passing this method would not replace explicit
     * administrator authorization or persistent
     * installation tracking.
     *
     * No construction is performed by this method.
     * ========================================================
     */

    public static PreflightResult checkExecutionReadiness(
            EarthBound plugin,
            World world,
            DevelopmentPlan developmentPlan,
            String propertyId,
            boolean geographicDataReady
    ) {

        if (!PHYSICAL_CONSTRUCTION_APPROVED) {

            return rejectPreflight(
                    PlanStatus.PHYSICAL_CONSTRUCTION_LOCKED,
                    "Physical residential construction "
                    + "remains locked."
            );
        }

        if (!EarthResidentialAuthorization
                .isAuthorizationEnabled(plugin)) {

            return rejectPreflight(
                    PlanStatus.AUTHORIZATION_DISABLED,
                    "Residential installation authorization "
                    + "is disabled."
            );
        }

        return preflightInstallation(
                plugin,
                world,
                developmentPlan,
                propertyId,
                geographicDataReady
        );
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

        String propertyId = site.getPropertyId();

        if (calculateRequiredChunks(site).isEmpty()) {

            return reject(
                    PlanStatus.PROPERTY_BOUNDARY_MISMATCH,
                    "Invalid residential property dimensions: "
                    + propertyId
            );
        }

        /*
         * Protect against invalid world coordinates
         * before scanning property boundaries.
         */

        if (site.getMinX() < -29999900
                || site.getMaxX() > 29999900
                || site.getMinZ() < -29999900
                || site.getMaxZ() > 29999900) {

            return reject(
                    PlanStatus.INVALID_PROPERTY,
                    "Residential property coordinates "
                    + "are outside safe world limits."
            );
        }

        for (int x = site.getMinX();
                x <= site.getMaxX();
                x++) {

            for (int z = site.getMinZ();
                    z <= site.getMaxZ();
                    z++) {

                Location location =
                        new Location(
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
                            PlanStatus.PROPERTY_BOUNDARY_MISMATCH,
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
                            PlanStatus.PROPERTY_BOUNDARY_MISMATCH,
                            "Construction and ownership "
                            + "boundaries differ: "
                            + propertyId
                    );
                }
            }
        }

        return null;
    }

    /*
     * ========================================================
     * CALCULATE REQUIRED CONSTRUCTION CHUNKS
     * ========================================================
     *
     * Floor division handles negative coordinates.
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

        if (width != 24L || length != 32L) {

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
     * RECHECK INITIAL DEVELOPMENT PLAN
     * ========================================================
     *
     * This method is for the initial development,
     * before any installation has started.
     *
     * Once individual houses have been completed,
     * use preflightInstallation instead.
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

        if (first == null
                || second == null
                || first.isEmpty()
                || first.size() != second.size()) {

            return false;
        }

        Set<Long> firstKeys = new HashSet<>();

        Set<Long> secondKeys = new HashSet<>();

        for (ConstructionChunk chunk : first) {

            firstKeys.add(chunk.getKey());
        }

        for (ConstructionChunk chunk : second) {

            secondKeys.add(chunk.getKey());
        }

        return firstKeys.size() == first.size()
                && secondKeys.size() == second.size()
                && firstKeys.equals(secondKeys);
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
                    + property.getSite().getDesign().name()
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
                "Adjacent construction chunks: NONE"
        );

        plugin.getLogger().info(
                "Temporary Alpha chunk separation: VERIFIED"
        );

        plugin.getLogger().info(
                "Physical construction: "
                + (PHYSICAL_CONSTRUCTION_APPROVED
                    ? "ENABLED"
                    : "LOCKED")
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
     * RESULT HELPERS
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

    private static PreflightResult rejectPreflight(
            PlanStatus status,
            String message
    ) {

        return new PreflightResult(
                status,
                message,
                null
        );
    }
}
