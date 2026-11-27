package com.earthbound;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.Location;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import com.earthbound.EarthAlphaResidentialGenerator
        .ResidentialSite;

import com.earthbound.EarthResidentialInstallation
        .InstallationStatus;

import com.earthbound.roads.EarthRoadData;
import com.earthbound.roads.EarthRoadSpatialIndex;
import com.earthbound.terrain.EarthTerrainLoader;

/*
 * ============================================================
 * EARTHBOUND RESIDENTIAL AUTHORIZATION
 * Friends Alpha
 * ============================================================
 *
 * Controlled authorization for residential development.
 *
 * Responsibilities:
 *
 * - Require explicit server administrator approval.
 * - Verify geographic data availability.
 * - Verify persistent installation storage.
 * - Verify world identity.
 * - Verify registered residential property locations.
 * - Reject construction on owned properties.
 * - Verify existing chunk safety.
 * - Prevent duplicate installation approvals.
 * - Enforce temporary Alpha construction chunk separation.
 * - Prevent conflicting development authorization.
 * - Require a reserved execution session before installation.
 * - Enforce physical construction and execution locks.
 * - Validate coordinate limits before property scanning.
 *
 * This class does not generate chunks,
 * place blocks or enable property sales.
 * ============================================================
 */

public final class EarthResidentialAuthorization {

    private static final String WORLD_NAME =
            "earthbound";

    private static final int REQUIRED_HOMES = 6;

    private static final int MAX_HORIZONTAL_COORDINATE =
            29_999_900;

    private static final int MIN_RESIDENTIAL_GROUND = 66;

    private static final int MAX_RESIDENTIAL_GROUND = 280;

    /*
     * Independent authorization configuration.
     * Missing configuration defaults to false.
     */

    private static final String AUTHORIZATION_CONFIG =
            "residential.allow-installation-approval";

    private static final String APPROVAL_CONFIRMATION =
            "APPROVE-GUEMES-ALPHA";

    private static final String START_CONFIRMATION =
            "START-GUEMES-INSTALLATION";

    private EarthResidentialAuthorization() {
    }

    /*
     * ========================================================
     * AUTHORIZATION RESULTS
     * ========================================================
     */

    public enum AuthorizationStatus {

        AUTHORIZED,

        INVALID_PLUGIN,
        MAIN_THREAD_REQUIRED,
        ADMINISTRATOR_REQUIRED,
        CONFIRMATION_REQUIRED,
        AUTHORIZATION_DISABLED,
        RESIDENTIAL_DISABLED,
        INVALID_WORLD,
        GEOGRAPHIC_DATA_UNAVAILABLE,
        INSTALLATION_STORAGE_UNAVAILABLE,
        ECONOMY_UNAVAILABLE,
        PROPERTY_SYSTEM_UNAVAILABLE,
        INVALID_PROPERTY_COUNT,
        INVALID_PROPERTY,
        DUPLICATE_PROPERTY,
        PROPERTY_NOT_REGISTERED,
        PROPERTY_ALREADY_OWNED,
        PROPERTY_BOUNDARY_MISMATCH,
        EXISTING_INSTALLATION_RECORD,
        INSTALLATION_IDENTITY_MISMATCH,
        INVALID_INSTALLATION_STATUS,

        PHYSICAL_CONSTRUCTION_LOCKED,
        EXECUTION_LOCKED,
        ACTIVE_EXECUTION,
        INVALID_EXECUTION_SESSION,
        INVALID_PROPERTY_COORDINATES,

        UNSAFE_CHUNK_SEPARATION,

        UNSAFE_DEVELOPMENT,
        UNSAFE_SITE,
        PERSISTENCE_FAILED,
        AUTHORIZATION_FAILED
    }

    public static final class AuthorizationResult {

        private final AuthorizationStatus status;

        private final String message;

        private AuthorizationResult(
                AuthorizationStatus status,
                String message
        ) {

            this.status = status;
            this.message = message;
        }

        public AuthorizationStatus getStatus() {

            return status;
        }

        public String getMessage() {

            return message;
        }

        public boolean isAuthorized() {

            return status == AuthorizationStatus.AUTHORIZED;
        }
    }

    /*
     * ========================================================
     * APPROVE RESIDENTIAL DEVELOPMENT
     * ========================================================
     *
     * Approves all six residential installation
     * records together.
     *
     * Approval does not activate construction.
     *
     * Development approval must not occur during
     * an active or unresolved construction execution.
     * ========================================================
     */

    public static AuthorizationResult approveDevelopment(
            EarthBound plugin,
            CommandSender sender,
            World world,
            boolean geographicDataReady,
            String confirmation
    ) {

        AuthorizationResult access =
                validateAuthorizationAccess(
                        plugin,
                        sender,
                        world,
                        geographicDataReady,
                        confirmation,
                        APPROVAL_CONFIRMATION
                );

        if (!access.isAuthorized()) {

            return access;
        }

        try {

            /*
             * Prevent conflicting development approval.
             */

            if (EarthResidentialExecution
                    .hasActiveExecution()) {

                return result(
                        AuthorizationStatus.ACTIVE_EXECUTION,
                        "Development approval is unavailable "
                        + "while an installation execution "
                        + "is active or unresolved."
                );
            }

            List<ResidentialSite> sites =
                    List.copyOf(
                            EarthGuemesGenerator
                                    .getResidentialSites()
                    );

            AuthorizationResult siteValidation =
                    validateRegisteredSites(
                            world,
                            sites
                    );

            if (!siteValidation.isAuthorized()) {

                return siteValidation;
            }

            /*
             * FRIENDS ALPHA CHUNK SAFETY
             *
             * Validate the entire development before
             * allowing persistent installation approval.
             *
             * Manually configured locations must meet
             * the same separation requirements as
             * automatically discovered locations.
             */

            AuthorizationResult chunkValidation =
                    validateDevelopmentChunkClearance(
                            sites
                    );

            if (!chunkValidation.isAuthorized()) {

                return chunkValidation;
            }

            /*
             * All six properties must be new
             * installations without previous records.
             */

            for (ResidentialSite site : sites) {

                String propertyId =
                        site.getPropertyId();

                InstallationStatus status =
                        EarthResidentialInstallation
                                .getStatus(propertyId);

                if (status != InstallationStatus.NOT_STARTED) {

                    return result(
                            AuthorizationStatus
                                    .EXISTING_INSTALLATION_RECORD,
                            "Installation record already exists: "
                            + propertyId
                            + " (" + status.name() + ")"
                    );
                }
            }

            /*
             * Final read-only development safety
             * inspection before persistent approval.
             */

            EarthResidentialSafety.SafetyResult safety =
                    EarthResidentialSafety
                            .inspectDevelopment(
                                    world,
                                    sites,
                                    geographicDataReady
                            );

            if (!safety.isSafe()) {

                return result(
                        AuthorizationStatus.UNSAFE_DEVELOPMENT,
                        safety.getMessage()
                );
            }

            /*
             * Repeat authorization checks immediately
             * before changing persistent records.
             */

            AuthorizationResult finalAccess =
                    validateAuthorizationAccess(
                            plugin,
                            sender,
                            world,
                            geographicDataReady,
                            confirmation,
                            APPROVAL_CONFIRMATION
                    );

            if (!finalAccess.isAuthorized()) {

                return finalAccess;
            }

            if (EarthResidentialExecution
                    .hasActiveExecution()) {

                return result(
                        AuthorizationStatus.ACTIVE_EXECUTION,
                        "Development approval suspended because "
                        + "an installation execution is active."
                );
            }

            /*
             * Persist approval for all six homes.
             *
             * The installation ledger independently
             * repeats its existing safety inspection.
             */

            boolean approved =
                    EarthResidentialInstallation
                            .approveDevelopment(
                                    world,
                                    sites,
                                    geographicDataReady,
                                    true
                            );

            if (!approved) {

                return result(
                        AuthorizationStatus.PERSISTENCE_FAILED,
                        "Residential approval could not be recorded."
                );
            }

            /*
             * Audit successful approval.
             */

            plugin.getLogger().info(
                    "========================================"
            );

            plugin.getLogger().info(
                    "EARTHBOUND RESIDENTIAL DEVELOPMENT APPROVAL"
            );

            plugin.getLogger().info(
                    "Authorized by: " + sender.getName()
            );

            plugin.getLogger().info(
                    "World: " + world.getName()
            );

            plugin.getLogger().info(
                    "World UUID: " + world.getUID()
            );

            plugin.getLogger().info(
                    "Approved residential properties: "
                    + sites.size()
            );

            plugin.getLogger().info(
                    "Alpha construction chunk separation: VERIFIED"
            );

            plugin.getLogger().info(
                    "Shared residential construction chunks: NONE"
            );

            plugin.getLogger().info(
                    "Adjacent residential construction chunks: NONE"
            );

            plugin.getLogger().info(
                    "Construction has not started."
            );

            plugin.getLogger().info(
                    "Residential generation remains "
                    + "under its existing safety lock."
            );

            plugin.getLogger().info(
                    "========================================"
            );

            return result(
                    AuthorizationStatus.AUTHORIZED,
                    "All six residential installation "
                    + "approvals were recorded successfully."
            );

        } catch (RuntimeException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Residential development authorization failed.",
                    exception
            );

            return result(
                    AuthorizationStatus.AUTHORIZATION_FAILED,
                    "Unexpected residential authorization failure."
            );
        }
    }

    /*
     * ========================================================
     * AUTHORIZE AN INDIVIDUAL INSTALLATION START
     * ========================================================
     *
     * Records IN_PROGRESS before construction.
     *
     * This method does not modify Minecraft blocks.
     *
     * IMPORTANT:
     *
     * Only the controlled residential execution system
     * may authorize an individual installation start.
     *
     * A matching reserved execution session must exist,
     * and its restricted authorization window must be open.
     *
     * Do not call this method as a preview or test.
     * ========================================================
     */

    public static AuthorizationResult authorizeInstallationStart(
            EarthBound plugin,
            CommandSender sender,
            World world,
            String propertyId,
            boolean geographicDataReady,
            String confirmation
    ) {

        AuthorizationResult access =
                validateAuthorizationAccess(
                        plugin,
                        sender,
                        world,
                        geographicDataReady,
                        confirmation,
                        START_CONFIRMATION
                );

        if (!access.isAuthorized()) {

            return access;
        }

        if (!isValidPropertyId(propertyId)) {

            return result(
                    AuthorizationStatus.INVALID_PROPERTY,
                    "Invalid residential property ID."
            );
        }

        try {

            /*
             * Require physical construction approval
             * and the matching execution startup session
             * before inspecting or modifying records.
             */

            AuthorizationResult executionValidation =
                    validateInstallationExecutionAccess(
                            plugin,
                            world,
                            propertyId
                    );

            if (!executionValidation.isAuthorized()) {

                return executionValidation;
            }

            List<ResidentialSite> sites =
                    List.copyOf(
                            EarthGuemesGenerator
                                    .getResidentialSites()
                    );

            if (sites.size() != REQUIRED_HOMES) {

                return result(
                        AuthorizationStatus.INVALID_PROPERTY_COUNT,
                        "Exactly six registered residential "
                        + "properties are required."
                );
            }

            /*
             * Resolve the requested site from the same
             * development snapshot used for validation.
             */

            ResidentialSite site = null;

            for (ResidentialSite registeredSite : sites) {

                if (propertyId.equals(
                        registeredSite.getPropertyId()
                )) {

                    site = registeredSite;

                    break;
                }
            }

            if (site == null) {

                return result(
                        AuthorizationStatus.PROPERTY_NOT_REGISTERED,
                        "Residential site not registered: "
                        + propertyId
                );
            }

            /*
             * Verify current ownership and configuration.
             */

            AuthorizationResult propertyValidation =
                    validateProperty(
                            world,
                            site
                    );

            if (!propertyValidation.isAuthorized()) {

                return propertyValidation;
            }

            /*
             * Reject a changed or incomplete development
             * plan and any unresolved installations.
             */

            Set<String> checkedIds = new HashSet<>();

            for (ResidentialSite registeredSite : sites) {

                if (registeredSite == null
                        || !isValidPropertyId(
                                registeredSite.getPropertyId()
                        )) {

                    return result(
                            AuthorizationStatus.INVALID_PROPERTY,
                            "Invalid registered installation site."
                    );
                }

                String registeredId =
                        registeredSite.getPropertyId();

                if (!checkedIds.add(registeredId)) {

                    return result(
                            AuthorizationStatus.DUPLICATE_PROPERTY,
                            "Duplicate residential property: "
                            + registeredId
                    );
                }

                InstallationStatus registeredStatus =
                        EarthResidentialInstallation
                                .getStatus(registeredId);

                /*
                 * Only approved or previously completed
                 * installations may exist in this plan.
                 */

                if (registeredStatus
                            != InstallationStatus.APPROVED

                        && registeredStatus
                            != InstallationStatus.COMPLETED) {

                    return result(
                            AuthorizationStatus
                                    .INVALID_INSTALLATION_STATUS,
                            "Development contains an unresolved "
                            + "installation: "
                            + registeredId
                            + " ("
                            + registeredStatus.name()
                            + ")"
                    );
                }

                if (!EarthResidentialInstallation
                        .matchesApprovedSite(
                                world,
                                registeredSite
                        )) {

                    return result(
                            AuthorizationStatus
                                    .INSTALLATION_IDENTITY_MISMATCH,
                            "Saved installation identity mismatch: "
                            + registeredId
                    );
                }

                /*
                 * Revalidate ownership and boundaries
                 * for all installations that are still
                 * awaiting construction.
                 *
                 * Completed installations are not
                 * treated as new construction sites.
                 */

                if (registeredStatus
                        == InstallationStatus.APPROVED) {

                    AuthorizationResult registeredValidation =
                            validateProperty(
                                    world,
                                    registeredSite
                            );

                    if (!registeredValidation.isAuthorized()) {

                        return registeredValidation;
                    }
                }
            }

            /*
             * FRIENDS ALPHA CHUNK SAFETY
             *
             * Repeat the separation check for all six
             * properties, including completed properties.
             *
             * This check is geometric and does not
             * require completed chunks to be ungenerated.
             */

            AuthorizationResult chunkValidation =
                    validateDevelopmentChunkClearance(
                            sites
                    );

            if (!chunkValidation.isAuthorized()) {

                return chunkValidation;
            }

            /*
             * The requested property must be approved,
             * not previously started or completed.
             */

            InstallationStatus status =
                    EarthResidentialInstallation
                            .getStatus(propertyId);

            if (status != InstallationStatus.APPROVED) {

                return result(
                        AuthorizationStatus.INVALID_INSTALLATION_STATUS,
                        "Installation cannot start from status: "
                        + status.name()
                );
            }

            /*
             * Verify the exact approved construction site.
             */

            if (!EarthResidentialInstallation
                    .matchesApprovedSite(
                            world,
                            site
                    )) {

                return result(
                        AuthorizationStatus
                                .INSTALLATION_IDENTITY_MISMATCH,
                        "Approved property location or "
                        + "world identity has changed."
                );
            }

            /*
             * Repeat current geographic and existing
             * chunk safety checks.
             *
             * Every unfinished approved property must
             * still pass the ungenerated-site inspection.
             *
             * Completed properties are excluded from
             * this particular inspection.
             */

            for (ResidentialSite registeredSite : sites) {

                InstallationStatus registeredStatus =
                        EarthResidentialInstallation
                                .getStatus(
                                        registeredSite.getPropertyId()
                                );

                if (registeredStatus
                        != InstallationStatus.APPROVED) {

                    continue;
                }

                EarthResidentialSafety.SafetyResult safety =
                        EarthResidentialSafety.inspectSite(
                                world,
                                registeredSite,
                                geographicDataReady
                        );

                if (!safety.isSafe()) {

                    return result(
                            AuthorizationStatus.UNSAFE_SITE,
                            registeredSite.getPropertyId()
                            + ": "
                            + safety.getMessage()
                    );
                }
            }

            /*
             * FINAL AUTHORIZATION CHECK
             *
             * Repeat general access and execution session
             * checks immediately before persistent mutation.
             */

            AuthorizationResult finalAccess =
                    validateAuthorizationAccess(
                            plugin,
                            sender,
                            world,
                            geographicDataReady,
                            confirmation,
                            START_CONFIRMATION
                    );

            if (!finalAccess.isAuthorized()) {

                return finalAccess;
            }

            if (EarthResidentialInstallation
                    .getStatus(propertyId)
                    != InstallationStatus.APPROVED) {

                return result(
                        AuthorizationStatus.INVALID_INSTALLATION_STATUS,
                        "Installation status changed "
                        + "before persistent authorization."
                );
            }

            if (!EarthResidentialInstallation
                    .matchesApprovedSite(
                            world,
                            site
                    )) {

                return result(
                        AuthorizationStatus
                                .INSTALLATION_IDENTITY_MISMATCH,
                        "Approved installation identity changed "
                        + "before persistent authorization."
                );
            }

            AuthorizationResult finalPropertyValidation =
                    validateProperty(
                            world,
                            site
                    );

            if (!finalPropertyValidation.isAuthorized()) {

                return finalPropertyValidation;
            }

            AuthorizationResult finalExecutionValidation =
                    validateInstallationExecutionAccess(
                            plugin,
                            world,
                            propertyId
                    );

            if (!finalExecutionValidation.isAuthorized()) {

                return finalExecutionValidation;
            }

            /*
             * Persist the installation start BEFORE
             * any future block modification.
             *
             * The reserved execution session remains
             * responsible for issuing the construction permit.
             */

            boolean started =
                    EarthResidentialInstallation
                            .beginInstallation(
                                    world,
                                    site,
                                    geographicDataReady,
                                    true
                            );

            if (!started) {

                return result(
                        AuthorizationStatus.PERSISTENCE_FAILED,
                        "Unable to persist installation start."
                );
            }

            /*
             * Confirm the persistent installation state.
             */

            if (!EarthResidentialInstallation.isReady()

                    || EarthResidentialInstallation
                            .getStatus(propertyId)
                            != InstallationStatus.IN_PROGRESS

                    || !EarthResidentialInstallation
                            .matchesApprovedSite(
                                    world,
                                    site
                            )) {

                return result(
                        AuthorizationStatus.PERSISTENCE_FAILED,
                        "Persistent installation start "
                        + "could not be confirmed."
                );
            }

            plugin.getLogger().info(
                    "Residential installation start recorded: "
                    + propertyId
            );

            plugin.getLogger().info(
                    "Authorized by: " + sender.getName()
            );

            plugin.getLogger().info(
                    "Reserved execution session: VERIFIED"
            );

            plugin.getLogger().info(
                    "Physical construction authorization: VERIFIED"
            );

            plugin.getLogger().info(
                    "Alpha construction chunk separation: VERIFIED"
            );

            plugin.getLogger().info(
                    "Installation status: IN_PROGRESS"
            );

            plugin.getLogger().info(
                    "No blocks have been modified "
                    + "by the authorization system."
            );

            return result(
                    AuthorizationStatus.AUTHORIZED,
                    "Installation start recorded: "
                    + propertyId
            );

        } catch (RuntimeException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Residential installation "
                    + "start authorization failed.",
                    exception
            );

            return result(
                    AuthorizationStatus.AUTHORIZATION_FAILED,
                    "Unexpected installation authorization failure."
            );
        }
    }

    /*
     * ========================================================
     * VALIDATE INSTALLATION EXECUTION ACCESS
     * ========================================================
     *
     * Individual installation authorization must never
     * operate independently of controlled execution.
     *
     * Development approval remains separate and does
     * not require these physical construction locks.
     *
     * This method does not issue construction permits
     * or modify persistent installation records.
     * ========================================================
     */

    private static AuthorizationResult
            validateInstallationExecutionAccess(
                    EarthBound plugin,
                    World world,
                    String propertyId
            ) {

        if (!Bukkit.isPrimaryThread()) {

            return result(
                    AuthorizationStatus.MAIN_THREAD_REQUIRED,
                    "Installation execution authorization "
                    + "requires the server main thread."
            );
        }

        if (!EarthResidentialInstaller
                .isPhysicalConstructionEnabled()) {

            return result(
                    AuthorizationStatus.PHYSICAL_CONSTRUCTION_LOCKED,
                    "Physical residential construction is locked."
            );
        }

        if (!EarthResidentialExecution
                .isExecutionEnabled()) {

            return result(
                    AuthorizationStatus.EXECUTION_LOCKED,
                    "Residential construction execution is locked."
            );
        }

        if (!EarthResidentialExecution
                .isInstallationAuthorizationPending(
                        plugin,
                        world,
                        propertyId
                )) {

            return result(
                    AuthorizationStatus.INVALID_EXECUTION_SESSION,
                    "Installation authorization requires "
                    + "a matching reserved execution session "
                    + "with an open authorization window."
            );
        }

        return result(
                AuthorizationStatus.AUTHORIZED,
                "Reserved installation execution "
                + "authorization verified."
        );
    }

    /*
     * ========================================================
     * VALIDATE FRIENDS ALPHA CHUNK CLEARANCE
     * ========================================================
     *
     * Central authorization check for our temporary
     * Friends Alpha property and chunk separation.
     *
     * Uses the same separation requirements as
     * EarthAlphaSiteFinder.
     *
     * This method does not inspect, load or
     * generate actual Minecraft chunks.
     *
     * The rule is temporary and must not become
     * a permanent EarthBound ownership restriction.
     * ========================================================
     */

    private static AuthorizationResult
            validateDevelopmentChunkClearance(
                    List<ResidentialSite> sites
            ) {

        if (sites == null
                || sites.size() != REQUIRED_HOMES) {

            return result(
                    AuthorizationStatus.INVALID_PROPERTY_COUNT,
                    "Exactly six residential properties "
                    + "are required for chunk clearance verification."
            );
        }

        if (!EarthAlphaSiteFinder
                .hasDevelopmentChunkClearance(sites)) {

            return result(
                    AuthorizationStatus.UNSAFE_CHUNK_SEPARATION,
                    "Residential development violates the "
                    + "temporary Friends Alpha property, access "
                    + "corridor or construction chunk separation "
                    + "requirements. Authorization denied."
            );
        }

        return result(
                AuthorizationStatus.AUTHORIZED,
                "Friends Alpha construction chunk "
                + "separation verified."
        );
    }

    /*
     * ========================================================
     * VALIDATE AUTHORIZATION ACCESS
     * ========================================================
     */

    private static AuthorizationResult validateAuthorizationAccess(
            EarthBound plugin,
            CommandSender sender,
            World world,
            boolean geographicDataReady,
            String confirmation,
            String requiredConfirmation
    ) {

        if (plugin == null || !plugin.isEnabled()) {

            return result(
                    AuthorizationStatus.INVALID_PLUGIN,
                    "EarthBound is unavailable."
            );
        }

        /*
         * All authorization operations must run on
         * the Minecraft server main thread.
         */

        if (!Bukkit.isPrimaryThread()) {

            return result(
                    AuthorizationStatus.MAIN_THREAD_REQUIRED,
                    "Authorization requires the server main thread."
            );
        }

        /*
         * Friends Alpha authorization is restricted
         * to the server console.
         *
         * Player operator status alone is insufficient.
         */

        if (!(sender instanceof ConsoleCommandSender)) {

            return result(
                    AuthorizationStatus.ADMINISTRATOR_REQUIRED,
                    "Residential authorization requires "
                    + "the server console."
            );
        }

        if (!requiredConfirmation.equals(confirmation)) {

            return result(
                    AuthorizationStatus.CONFIRMATION_REQUIRED,
                    "Explicit residential installation "
                    + "confirmation is required."
            );
        }

        /*
         * Independent authorization lock.
         */

        if (!plugin.getConfig().getBoolean(
                AUTHORIZATION_CONFIG,
                false
        )) {

            return result(
                    AuthorizationStatus.AUTHORIZATION_DISABLED,
                    "Residential installation authorization "
                    + "is disabled."
            );
        }

        if (!plugin.getConfig().getBoolean(
                "residential.enabled",
                false
        )) {

            return result(
                    AuthorizationStatus.RESIDENTIAL_DISABLED,
                    "Residential development is disabled."
            );
        }

        /*
         * Verify the intended loaded Minecraft world.
         */

        if (world == null
                || !WORLD_NAME.equals(world.getName())
                || Bukkit.getWorld(world.getUID()) != world

                || !WORLD_NAME.equals(
                        plugin.getConfig().getString(
                                "residential.world",
                                ""
                        )
                )) {

            return result(
                    AuthorizationStatus.INVALID_WORLD,
                    "Residential authorization requires "
                    + "the configured EarthBound world."
            );
        }

        if (!geographicDataReady
                || !EarthTerrainLoader.isLoaded()
                || !EarthRoadData.isLoaded()
                || !EarthRoadSpatialIndex.isLoaded()) {

            return result(
                    AuthorizationStatus.GEOGRAPHIC_DATA_UNAVAILABLE,
                    "Required geographic data is unavailable."
            );
        }

        if (!EarthResidentialInstallation.isReady()) {

            return result(
                    AuthorizationStatus
                            .INSTALLATION_STORAGE_UNAVAILABLE,
                    "Residential installation storage "
                    + "is unavailable."
            );
        }

        if (!EarthEconomy.isReady()) {

            return result(
                    AuthorizationStatus.ECONOMY_UNAVAILABLE,
                    "EarthBound economy is unavailable."
            );
        }

        if (!EarthPropertyManager.isReady()) {

            return result(
                    AuthorizationStatus.PROPERTY_SYSTEM_UNAVAILABLE,
                    "Property ownership information "
                    + "is unavailable."
            );
        }

        return result(
                AuthorizationStatus.AUTHORIZED,
                "Authorization access checks passed."
        );
    }

    /*
     * ========================================================
     * VALIDATE ALL REGISTERED RESIDENTIAL SITES
     * ========================================================
     */

    private static AuthorizationResult validateRegisteredSites(
            World world,
            List<ResidentialSite> sites
    ) {

        if (sites == null
                || sites.size() != REQUIRED_HOMES) {

            return result(
                    AuthorizationStatus.INVALID_PROPERTY_COUNT,
                    "Exactly six residential properties "
                    + "must be registered."
            );
        }

        Set<String> propertyIds = new HashSet<>();

        for (ResidentialSite site : sites) {

            if (site == null
                    || !isValidPropertyId(
                            site.getPropertyId()
                    )) {

                return result(
                        AuthorizationStatus.INVALID_PROPERTY,
                        "Invalid residential property."
                );
            }

            if (!propertyIds.add(site.getPropertyId())) {

                return result(
                        AuthorizationStatus.DUPLICATE_PROPERTY,
                        "Duplicate property ID: "
                        + site.getPropertyId()
                );
            }

            AuthorizationResult validation =
                    validateProperty(
                            world,
                            site
                    );

            if (!validation.isAuthorized()) {

                return validation;
            }
        }

        return result(
                AuthorizationStatus.AUTHORIZED,
                "All six residential properties validated."
        );
    }

    /*
     * ========================================================
     * VALIDATE PROPERTY COORDINATES
     * ========================================================
     *
     * Reject invalid horizontal and vertical coordinates
     * before entering property boundary scanning loops.
     *
     * Horizontal bounds remain inside Minecraft's
     * supported coordinate limits.
     *
     * Long arithmetic is used for plot dimensions.
     * ========================================================
     */

    private static AuthorizationResult
            validatePropertyCoordinates(
                    World world,
                    ResidentialSite site
            ) {

        if (world == null || site == null) {

            return result(
                    AuthorizationStatus.INVALID_PROPERTY,
                    "Invalid residential property coordinates."
            );
        }

        String propertyId =
                site.getPropertyId();

        int minX = site.getMinX();

        int maxX = site.getMaxX();

        int minZ = site.getMinZ();

        int maxZ = site.getMaxZ();

        int groundY = site.getGroundY();

        if (minX > maxX
                || minZ > maxZ

                || minX < -MAX_HORIZONTAL_COORDINATE
                || maxX > MAX_HORIZONTAL_COORDINATE

                || minZ < -MAX_HORIZONTAL_COORDINATE
                || maxZ > MAX_HORIZONTAL_COORDINATE) {

            return result(
                    AuthorizationStatus.INVALID_PROPERTY_COORDINATES,
                    "Residential property exceeds safe "
                    + "horizontal coordinate limits: "
                    + propertyId
            );
        }

        if (groundY < MIN_RESIDENTIAL_GROUND
                || groundY > MAX_RESIDENTIAL_GROUND

                || groundY < world.getMinHeight()
                || groundY >= world.getMaxHeight()) {

            return result(
                    AuthorizationStatus.INVALID_PROPERTY_COORDINATES,
                    "Residential property exceeds safe "
                    + "ground height limits: "
                    + propertyId
            );
        }

        long width =
                (long) maxX
                - minX
                + 1L;

        long length =
                (long) maxZ
                - minZ
                + 1L;

        if (width != EarthAlphaResidentialGenerator.PLOT_WIDTH
                || length
                    != EarthAlphaResidentialGenerator.PLOT_LENGTH) {

            return result(
                    AuthorizationStatus.PROPERTY_BOUNDARY_MISMATCH,
                    "Invalid Alpha residential property dimensions: "
                    + propertyId
            );
        }

        return result(
                AuthorizationStatus.AUTHORIZED,
                "Residential property coordinates verified: "
                + propertyId
        );
    }

    /*
     * ========================================================
     * VALIDATE PROPERTY OWNERSHIP AND BOUNDARIES
     * ========================================================
     */

    private static AuthorizationResult validateProperty(
            World world,
            ResidentialSite site
    ) {

        if (world == null
                || site == null
                || !isValidPropertyId(
                        site.getPropertyId()
                )) {

            return result(
                    AuthorizationStatus.INVALID_PROPERTY,
                    "Invalid residential property."
            );
        }

        String propertyId =
                site.getPropertyId();

        /*
         * Validate all scanning coordinates and
         * established Alpha plot dimensions.
         */

        AuthorizationResult coordinateValidation =
                validatePropertyCoordinates(
                        world,
                        site
                );

        if (!coordinateValidation.isAuthorized()) {

            return coordinateValidation;
        }

        /*
         * Check authoritative persistent ownership.
         *
         * An existing owner must never have their
         * property reconstructed automatically.
         */

        if (EarthEconomy.getPropertyOwner(propertyId)
                != null) {

            return result(
                    AuthorizationStatus.PROPERTY_ALREADY_OWNED,
                    "Property already has a recorded owner: "
                    + propertyId
            );
        }

        /*
         * Snapshot validated coordinates before scanning.
         */

        int minX = site.getMinX();

        int maxX = site.getMaxX();

        int minZ = site.getMinZ();

        int maxZ = site.getMaxZ();

        int groundY = site.getGroundY();

        /*
         * Verify every horizontal block in the
         * proposed residential plot against the
         * registered property protection system.
         *
         * Bounded local offsets prevent integer overflow
         * in the scanning loop counters.
         */

        for (int localX = 0;
                localX < EarthAlphaResidentialGenerator.PLOT_WIDTH;
                localX++) {

            int x = minX + localX;

            for (int localZ = 0;
                    localZ < EarthAlphaResidentialGenerator.PLOT_LENGTH;
                    localZ++) {

                int z = minZ + localZ;

                Location location =
                        new Location(
                                world,
                                x,
                                groundY,
                                z
                        );

                var property =
                        EarthPropertyManager
                                .getPropertyAt(location);

                if (property == null) {

                    return result(
                            AuthorizationStatus.PROPERTY_NOT_REGISTERED,
                            "Property protection is missing at "
                            + x
                            + ", "
                            + z
                    );
                }

                if (!propertyId.equals(property.getId())) {

                    return result(
                            AuthorizationStatus
                                    .PROPERTY_BOUNDARY_MISMATCH,
                            "Conflicting property registration at "
                            + x
                            + ", "
                            + z
                    );
                }

                if (property.isOwned()) {

                    return result(
                            AuthorizationStatus.PROPERTY_ALREADY_OWNED,
                            "Registered property already has an owner: "
                            + propertyId
                    );
                }

                /*
                 * Confirm the registered property belongs
                 * to the intended EarthBound world.
                 */

                if (!world.getName().equals(
                        property.getWorldName()
                )) {

                    return result(
                            AuthorizationStatus.INVALID_WORLD,
                            "Registered property belongs to "
                            + "an unexpected world: "
                            + propertyId
                    );
                }

                /*
                 * Require exact ownership boundaries,
                 * not merely a larger containing plot.
                 */

                if (property.getMinX() != minX
                        || property.getMaxX() != maxX
                        || property.getMinZ() != minZ
                        || property.getMaxZ() != maxZ) {

                    return result(
                            AuthorizationStatus
                                    .PROPERTY_BOUNDARY_MISMATCH,
                            "Registered ownership boundaries "
                            + "do not match construction boundaries: "
                            + propertyId
                    );
                }
            }
        }

        return result(
                AuthorizationStatus.AUTHORIZED,
                "Property ownership and boundaries verified: "
                + propertyId
        );
    }

    /*
     * ========================================================
     * AUTHORIZATION CONFIGURATION STATUS
     * ========================================================
     */

    public static boolean isAuthorizationEnabled(
            EarthBound plugin
    ) {

        return plugin != null

                && plugin.isEnabled()

                && plugin.getConfig().getBoolean(
                        AUTHORIZATION_CONFIG,
                        false
                )

                && plugin.getConfig().getBoolean(
                        "residential.enabled",
                        false
                );
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
     * RESULT HELPER
     * ========================================================
     */

    private static AuthorizationResult result(
            AuthorizationStatus status,
            String message
    ) {

        return new AuthorizationResult(
                status,
                message
        );
    }
}
