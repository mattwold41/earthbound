package com.earthbound;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World;

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
 *
 * This class does not generate chunks,
 * place blocks or enable property sales.
 * ============================================================
 */

public final class EarthResidentialAuthorization {

    private static final String WORLD_NAME =
            "earthbound";

    private static final int REQUIRED_HOMES = 6;

    /*
     * Independent authorization configuration.
     *
     * Missing configuration defaults to false.
     */

    private static final String AUTHORIZATION_CONFIG =
            "residential.allow-installation-approval";

    /*
     * Explicit confirmation required from the
     * future server console administration command.
     */

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

        UNSAFE_DEVELOPMENT,

        UNSAFE_SITE,

        PERSISTENCE_FAILED,

        AUTHORIZATION_FAILED
    }

    /*
     * ========================================================
     * AUTHORIZATION RESULT
     * ========================================================
     */

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

            return status
                    == AuthorizationStatus.AUTHORIZED;
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
             * Persist approval for all six homes.
             *
             * The installation ledger independently
             * repeats the geographic safety inspection.
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
     * A future controlled installer must call this
     * method immediately before beginning installation.
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

            ResidentialSite site =
                    EarthGuemesGenerator
                            .getResidentialSite(propertyId);

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

            Set<String> checkedIds =
                    new HashSet<>();

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
             */

            EarthResidentialSafety.SafetyResult safety =
                    EarthResidentialSafety.inspectSite(
                            world,
                            site,
                            geographicDataReady
                    );

            if (!safety.isSafe()) {

                return result(
                        AuthorizationStatus.UNSAFE_SITE,
                        safety.getMessage()
                );
            }

            /*
             * Persist the installation start BEFORE
             * any future block modification.
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

            plugin.getLogger().info(
                    "Residential installation start recorded: "
                    + propertyId
            );

            plugin.getLogger().info(
                    "Authorized by: " + sender.getName()
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

        /*
         * Require an exact confirmation phrase.
         */

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

        /*
         * Residential configuration must also
         * be explicitly enabled.
         */

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

        /*
         * Require geographic data readiness.
         */

        if (!geographicDataReady
                || !EarthTerrainLoader.isLoaded()
                || !EarthRoadData.isLoaded()
                || !EarthRoadSpatialIndex.isLoaded()) {

            return result(
                    AuthorizationStatus.GEOGRAPHIC_DATA_UNAVAILABLE,
                    "Required geographic data is unavailable."
            );
        }

        /*
         * Require persistent installation storage.
         */

        if (!EarthResidentialInstallation.isReady()) {

            return result(
                    AuthorizationStatus
                            .INSTALLATION_STORAGE_UNAVAILABLE,
                    "Residential installation storage "
                    + "is unavailable."
            );
        }

        /*
         * Require the authoritative economic ledger.
         */

        if (!EarthEconomy.isReady()) {

            return result(
                    AuthorizationStatus.ECONOMY_UNAVAILABLE,
                    "EarthBound economy is unavailable."
            );
        }

        /*
         * Require initialized property ownership.
         */

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

        Set<String> propertyIds =
                new HashSet<>();

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
         * Verify every horizontal block in the
         * proposed residential plot against the
         * registered property protection system.
         *
         * This also detects missing registrations
         * and conflicting registered property IDs.
         */

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
                 * Require exact ownership boundaries,
                 * not merely a larger containing plot.
                 */

                if (property.getMinX() != site.getMinX()
                        || property.getMaxX() != site.getMaxX()
                        || property.getMinZ() != site.getMinZ()
                        || property.getMaxZ() != site.getMaxZ()) {

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
