package com.earthbound;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import com.earthbound.terrain.EarthTerrainDownloader;
import com.earthbound.terrain.EarthTerrainLoader;

import com.earthbound.water.EarthWaterData;

import com.earthbound.roads.EarthRoadData;
import com.earthbound.roads.EarthRoadElevationProfiles;
import com.earthbound.roads.EarthRoadSpatialIndex;

import com.earthbound.EarthResidentialInstallation
        .InstallationStatus;

public class EarthBound extends JavaPlugin {

    /*
     * GUEMES ISLAND SPAWN
     */

    private static final double GUEMES_SPAWN_X = -654.0;
    private static final double GUEMES_SPAWN_Y = 86.0;
    private static final double GUEMES_SPAWN_Z = -355.0;

    /*
     * FRIENDS ALPHA RESIDENTIAL DEVELOPMENT
     */

    private static final int REQUIRED_ALPHA_HOMES = 6;

    private static final String SITE_SEARCH_CONFIG =
            "diagnostics.residential-site-search";

    /*
     * ========================================================
     * ENABLE EARTHBOUND
     * ========================================================
     */

    @Override
    public void onEnable() {

        getLogger().info("EarthBound starting...");

        saveDefaultConfig();

        EarthGuemesGenerator.clearResidentialSites();

        /*
         * INITIALIZE RESIDENTIAL INSTALLATION TRACKING
         *
         * Load persistent installation records before
         * residential development is evaluated.
         */

        boolean installationStorageReady =
                initializeResidentialInstallation();

        /*
         * WORLD RULES
         */

        Bukkit.getPluginManager().registerEvents(
                new EarthBoundWorldRules(this),
                this
        );

        /*
         * ECONOMY
         */

        EarthEconomy.setup(this);

        if (getCommand("balance") != null) {

            getCommand("balance").setExecutor(
                    new EarthEconomyCommand()
            );

        } else {

            getLogger().warning(
                    "Balance command missing from plugin.yml"
            );
        }

        /*
         * PLAYER PAYMENTS
         */

        if (getCommand("pay") != null) {

            getCommand("pay").setExecutor(
                    new EarthPayCommand()
            );

            getLogger().info(
                    "Player payment command registered!"
            );

        } else {

            getLogger().warning(
                    "Pay command missing from plugin.yml"
            );
        }

        /*
         * GOVERNMENT BANKING
         */

        if (getCommand("earthbank") != null) {

            getCommand("earthbank").setExecutor(
                    new EarthBankCommand()
            );

            getLogger().info(
                    "Government banking command registered!"
            );

        } else {

            getLogger().warning(
                    "Earthbank command missing from plugin.yml"
            );
        }

        /*
         * GENERAL STORE
         */

        EarthStore.setup();

        if (getCommand("store") != null) {

            getCommand("store").setExecutor(
                    new EarthStoreCommand()
            );

            getLogger().info(
                    "General Store command registered!"
            );

        } else {

            getLogger().warning(
                    "Store command missing from plugin.yml"
            );
        }

        /*
         * PROPERTY OWNERSHIP SYSTEM
         *
         * Government property sale distribution:
         *
         * 60% Anacortes
         * 20% Washington
         * 20% USA
         */

        EarthPropertyManager.setup(this);

        getLogger().info(
                "EarthBound property system initialized!"
        );

        /*
         * PROPERTY PURCHASE LISTENER
         */

        EarthPropertyBannerRegistry.clear();

        Bukkit.getPluginManager().registerEvents(
                new EarthPropertyListener(this),
                this
        );

        getLogger().info(
                "EarthBound property purchase listener registered!"
        );

        /*
         * PROPERTY SECURITY SYSTEM
         */

        Bukkit.getPluginManager().registerEvents(
                new EarthPropertyProtection(),
                this
        );

        getLogger().info(
                "EarthBound property protection enabled!"
        );

        /*
         * GEOGRAPHIC DATA
         */

        boolean geographicDataReady =
                initializeGeographicData();

        /*
         * FRIENDS ALPHA RESIDENTIAL CONFIGURATION
         *
         * Require both geographic data and valid
         * persistent installation storage.
         */

        if (geographicDataReady
                && installationStorageReady
                && EarthResidentialInstallation.isReady()) {

            boolean residentialConfigLoaded =
                    EarthGuemesGenerator
                            .loadResidentialSites(this);

            getLogger().info(
                    "Residential configuration loaded: "
                    + residentialConfigLoaded
            );

            if (!residentialConfigLoaded) {

                EarthGuemesGenerator
                        .clearResidentialSites();

                getLogger().warning(
                        "Residential development remains disabled."
                );
            }

        } else {

            EarthGuemesGenerator.clearResidentialSites();

            getLogger().warning(
                    "Residential configuration skipped: "
                    + "required safety systems are unavailable."
            );

            getLogger().warning(
                    "Geographic data ready: "
                    + geographicDataReady
            );

            getLogger().warning(
                    "Installation storage ready: "
                    + EarthResidentialInstallation.isReady()
            );
        }

        getLogger().info(
                "Geographically validated residential sites: "
                + EarthGuemesGenerator
                        .getResidentialSiteCount()
        );

        getLogger().info(
                "Residential construction enabled: "
                + EarthGuemesGenerator
                        .isResidentialGenerationEnabled()
        );

        /*
         * OPTIONAL RESIDENTIAL SITE SEARCH
         */

        runResidentialSiteDiagnostics(
                geographicDataReady
        );

        /*
         * DISABLE NORMAL MOB SPAWNING
         */

        Bukkit.getScheduler().runTask(
                this,
                () -> {

                    for (World world : Bukkit.getWorlds()) {

                        world.setGameRule(
                                org.bukkit.GameRule.DO_MOB_SPAWNING,
                                false
                        );
                    }
                }
        );

        /*
         * SET GUEMES SPAWN
         */

        Bukkit.getScheduler().runTask(
                this,
                () -> {

                    World world =
                            Bukkit.getWorld("earthbound");

                    if (world == null) {

                        getLogger().warning(
                                "EarthBound world not found. "
                                + "Guemes spawn was not set."
                        );

                        return;
                    }

                    world.setSpawnLocation(
                            new Location(
                                    world,
                                    GUEMES_SPAWN_X,
                                    GUEMES_SPAWN_Y,
                                    GUEMES_SPAWN_Z
                            )
                    );

                    getLogger().info(
                            "Guemes spawn set!"
                    );
                }
        );

        /*
         * RESIDENTIAL WORLD SAFETY AND REAL ESTATE
         *
         * Run on the server main thread after startup
         * world initialization.
         */

        Bukkit.getScheduler().runTask(
                this,
                () -> initializeResidentialWorld(
                        geographicDataReady
                )
        );

        /*
         * STARTUP SUMMARY
         */

        getLogger().info(
                "Residential installation storage ready: "
                + EarthResidentialInstallation.isReady()
        );

        getLogger().info(
                "Residential construction authorization: LOCKED"
        );

        getLogger().info(
                "Residential automatic sale markers: DISABLED"
        );

        getLogger().info(
                "EarthBound enabled!"
        );
    }

    /*
     * ========================================================
     * RESIDENTIAL INSTALLATION STORAGE
     * ========================================================
     */

    private boolean initializeResidentialInstallation() {

        getLogger().info(
                "========================================"
        );

        getLogger().info(
                "EARTHBOUND RESIDENTIAL INSTALLATION STORAGE"
        );

        try {

            boolean initialized =
                    EarthResidentialInstallation.setup(this);

            if (!initialized
                    || !EarthResidentialInstallation.isReady()) {

                EarthResidentialInstallation.shutdown();

                getLogger().severe(
                        "Residential installation storage "
                        + "could not be initialized."
                );

                getLogger().warning(
                        "Residential construction remains locked."
                );

                getLogger().warning(
                        "Residential sale markers remain disabled."
                );

                return false;
            }

            getLogger().info(
                    "Residential installation storage initialized."
            );

            reportResidentialInstallationStatus();

            getLogger().info(
                    "Installation records will not "
                    + "automatically authorize construction."
            );

            return true;

        } catch (RuntimeException exception) {

            EarthResidentialInstallation.shutdown();

            getLogger().log(
                    Level.SEVERE,
                    "Residential installation initialization failed.",
                    exception
            );

            getLogger().warning(
                    "Residential construction remains locked."
            );

            return false;

        } finally {

            getLogger().info(
                    "========================================"
            );
        }
    }

    /*
     * ========================================================
     * REPORT RESIDENTIAL INSTALLATION STATUS
     * ========================================================
     */

    private void reportResidentialInstallationStatus() {

        if (!EarthResidentialInstallation.isReady()) {

            getLogger().warning(
                    "Residential installation status unavailable."
            );

            return;
        }

        int notStarted = 0;
        int approved = 0;
        int inProgress = 0;
        int completed = 0;
        int failed = 0;

        /*
         * Query all six expected property IDs.
         *
         * NOT_STARTED represents a property without
         * a persistent installation record.
         */

        for (int homeNumber = 1;
                homeNumber <= REQUIRED_ALPHA_HOMES;
                homeNumber++) {

            String propertyId =
                    "guemes_home_0" + homeNumber;

            InstallationStatus status =
                    EarthResidentialInstallation
                            .getStatus(propertyId);

            switch (status) {

                case NOT_STARTED:
                    notStarted++;
                    break;

                case APPROVED:
                    approved++;
                    break;

                case IN_PROGRESS:
                    inProgress++;
                    break;

                case COMPLETED:
                    completed++;
                    break;

                case FAILED:
                    failed++;
                    break;
            }

            getLogger().info(
                    propertyId + ": " + status.name()
            );
        }

        getLogger().info(
                "----------------------------------------"
        );

        getLogger().info(
                "Residential properties: "
                + REQUIRED_ALPHA_HOMES
        );

        getLogger().info(
                "Not started: " + notStarted
        );

        getLogger().info(
                "Approved: " + approved
        );

        getLogger().info(
                "In progress: " + inProgress
        );

        getLogger().info(
                "Completed: " + completed
        );

        getLogger().info(
                "Failed: " + failed
        );

        if (failed > 0 || inProgress > 0) {

            getLogger().warning(
                    "Residential installations require "
                    + "manual administrator review."
            );

            getLogger().warning(
                    "Automatic construction recovery is disabled."
            );
        }

        if (completed > 0) {

            getLogger().info(
                    "Completed installation records detected."
            );

            getLogger().info(
                    "Recorded completion is not a new "
                    + "physical structure verification."
            );

            getLogger().info(
                    "Completed homes must never be "
                    + "automatically reconstructed."
            );
        }
    }

    /*
     * ========================================================
     * RESIDENTIAL WORLD INITIALIZATION
     * ========================================================
     *
     * Read-only world inspection.
     *
     * Does not generate residential chunks.
     * Does not install homes.
     * Does not authorize construction.
     * Does not install sale markers.
     */

    private void initializeResidentialWorld(
            boolean geographicDataReady
    ) {

        EarthPropertyBannerRegistry.clear();

        /*
         * Require persistent installation storage.
         */

        if (!EarthResidentialInstallation.isReady()) {

            getLogger().severe(
                    "Residential world initialization cancelled: "
                    + "installation storage is unavailable."
            );

            EarthGuemesGenerator.clearResidentialSites();

            return;
        }

        World world =
                Bukkit.getWorld("earthbound");

        if (world == null) {

            getLogger().warning(
                    "EarthBound world not found. "
                    + "Residential world inspection skipped."
            );

            return;
        }

        /*
         * Require successful geographic initialization.
         */

        if (!geographicDataReady) {

            getLogger().warning(
                    "Residential world inspection skipped: "
                    + "geographic data is unavailable."
            );

            return;
        }

        /*
         * Respect the residential configuration lock.
         */

        if (!getConfig().getBoolean(
                "residential.enabled",
                false
        )) {

            getLogger().info(
                    "Residential world inspection skipped: "
                    + "residential development is disabled."
            );

            return;
        }

        /*
         * Require exactly six geographically
         * validated Alpha residential properties.
         */

        int registeredSites =
                EarthGuemesGenerator
                        .getResidentialSiteCount();

        if (registeredSites != REQUIRED_ALPHA_HOMES) {

            getLogger().warning(
                    "Residential world inspection cancelled: "
                    + "six validated properties are required."
            );

            getLogger().warning(
                    "Registered residential properties: "
                    + registeredSites
                    + " / "
                    + REQUIRED_ALPHA_HOMES
            );

            return;
        }

        getLogger().info(
                "========================================"
        );

        getLogger().info(
                "EARTHBOUND RESIDENTIAL WORLD SAFETY"
        );

        getLogger().info(
                "World: " + world.getName()
        );

        getLogger().info(
                "World UUID: " + world.getUID()
        );

        getLogger().info(
                "Minimum world height: "
                + world.getMinHeight()
        );

        getLogger().info(
                "Maximum world height (exclusive): "
                + world.getMaxHeight()
        );

        getLogger().info(
                "Registered residential properties: "
                + registeredSites
        );

        /*
         * Check persistent installation identities
         * before evaluating undeveloped land.
         */

        if (!validateResidentialInstallationRecords(world)) {

            getLogger().warning(
                    "Residential world initialization rejected."
            );

            getLogger().warning(
                    "Manual installation review required."
            );

            getLogger().warning(
                    "Construction and sale markers remain locked."
            );

            getLogger().info(
                    "========================================"
            );

            return;
        }

        /*
         * Inspect existing chunks, property boundaries,
         * geographic suitability and build heights.
         */

        boolean inspectionPassed;

        long startTime = System.nanoTime();

        try {

            inspectionPassed =
                    EarthGuemesGenerator
                            .inspectRegisteredSites(
                                    this,
                                    world,
                                    geographicDataReady
                            );

        } catch (RuntimeException exception) {

            getLogger().log(
                    Level.WARNING,
                    "Residential world inspection failed.",
                    exception
            );

            inspectionPassed = false;
        }

        long elapsedMilliseconds =
                (System.nanoTime() - startTime)
                        / 1000000L;

        getLogger().info(
                "Residential inspection duration: "
                + elapsedMilliseconds
                + " ms"
        );

        getLogger().info(
                "Residential world safety passed: "
                + inspectionPassed
        );

        if (!inspectionPassed) {

            getLogger().warning(
                    "Residential development rejected "
                    + "by world safety inspection."
            );

            getLogger().warning(
                    "No residential construction authorized."
            );

            getLogger().warning(
                    "Residential sale markers remain disabled."
            );

            getLogger().info(
                    "========================================"
            );

            return;
        }

        /*
         * IMPORTANT:
         *
         * A successful inspection is only a snapshot
         * of current world conditions.
         *
         * It is not construction authorization.
         */

        getLogger().info(
                "Residential world safety inspection passed."
        );

        getLogger().info(
                "All six properties passed the current "
                + "read-only inspection."
        );

        getLogger().info(
                "Persistent installation storage validated."
        );

        getLogger().info(
                "Construction authorization remains locked."
        );

        getLogger().info(
                "Residential sale markers remain disabled "
                + "until completed installations are verified."
        );

        getLogger().info(
                "========================================"
        );

        /*
         * FUTURE CONTROLLED REAL ESTATE INITIALIZATION
         *
         * EarthPropertyBannerRegistry.setup(this);
         * EarthPropertySaleMarkers.setup(this);
         *
         * Do not invoke either method until actual
         * completed installations are verified.
         */
    }

    /*
     * ========================================================
     * VALIDATE PERSISTENT INSTALLATION IDENTITIES
     * ========================================================
     *
     * Existing records must match the current world
     * UUID, property coordinates and house design.
     *
     * Previously started or completed properties
     * must not be treated as undeveloped land.
     */

    private boolean validateResidentialInstallationRecords(
            World world
    ) {

        if (world == null
                || !Bukkit.isPrimaryThread()
                || !EarthResidentialInstallation.isReady()) {

            return false;
        }

        try {

            var sites =
                    EarthGuemesGenerator
                            .getResidentialSites();

            if (sites.size() != REQUIRED_ALPHA_HOMES) {

                return false;
            }

            boolean valid = true;

            boolean existingConstructionDetected = false;

            for (var site : sites) {

                String propertyId =
                        site.getPropertyId();

                InstallationStatus status =
                        EarthResidentialInstallation
                                .getStatus(propertyId);

                getLogger().info(
                        "Installation inspection: "
                        + propertyId
                        + " = "
                        + status.name()
                );

                /*
                 * A new site has no saved record.
                 */

                if (status == InstallationStatus.NOT_STARTED) {

                    continue;
                }

                /*
                 * Any existing record must match
                 * the current installation identity.
                 */

                if (!EarthResidentialInstallation
                        .matchesApprovedSite(world, site)) {

                    getLogger().severe(
                            "Persistent installation identity mismatch: "
                            + propertyId
                    );

                    valid = false;

                    continue;
                }

                /*
                 * Approved records still require
                 * undeveloped-land safety inspection.
                 */

                if (status == InstallationStatus.APPROVED) {

                    getLogger().info(
                            "Saved approval identity verified: "
                            + propertyId
                    );

                    continue;
                }

                /*
                 * Never automatically inspect an existing
                 * installation as a new construction site.
                 */

                existingConstructionDetected = true;

                if (status == InstallationStatus.COMPLETED) {

                    getLogger().info(
                            "Existing completed installation: "
                            + propertyId
                    );

                    getLogger().info(
                            "Automatic reconstruction prohibited."
                    );

                } else {

                    getLogger().warning(
                            "Unresolved residential installation: "
                            + propertyId
                    );

                    getLogger().warning(
                            "Manual administrator review required."
                    );
                }
            }

            if (!valid) {

                return false;
            }

            if (existingConstructionDetected) {

                getLogger().warning(
                        "New-development inspection suspended."
                );

                getLogger().warning(
                        "Existing installation records require "
                        + "the future completed-structure "
                        + "verification system."
                );

                return false;
            }

            return true;

        } catch (RuntimeException exception) {

            getLogger().log(
                    Level.SEVERE,
                    "Residential installation identity "
                    + "validation failed.",
                    exception
            );

            return false;
        }
    }

    /*
     * ========================================================
     * RESIDENTIAL SITE SEARCH DIAGNOSTICS
     * ========================================================
     *
     * Read-only geographic search.
     *
     * Does not construct homes, generate chunks,
     * register properties or modify configuration.
     *
     * Disabled by default.
     */

    private void runResidentialSiteDiagnostics(
            boolean geographicDataReady
    ) {

        if (!getConfig().getBoolean(
                SITE_SEARCH_CONFIG,
                false
        )) {

            getLogger().info(
                    "Residential site search diagnostics disabled."
            );

            return;
        }

        /*
         * Require all geographic systems.
         */

        if (!geographicDataReady
                || !EarthTerrainLoader.isLoaded()
                || !EarthRoadData.isLoaded()
                || !EarthRoadSpatialIndex.isLoaded()) {

            getLogger().warning(
                    "Residential site search cancelled: "
                    + "geographic data is not ready."
            );

            return;
        }

        /*
         * Require reliable persistent installation data.
         */

        if (!EarthResidentialInstallation.isReady()) {

            getLogger().warning(
                    "Residential site search cancelled: "
                    + "installation storage is unavailable."
            );

            return;
        }

        /*
         * Do not propose replacement development
         * while any saved installation records exist.
         */

        try {

            for (int homeNumber = 1;
                    homeNumber <= REQUIRED_ALPHA_HOMES;
                    homeNumber++) {

                String propertyId =
                        "guemes_home_0" + homeNumber;

                InstallationStatus status =
                        EarthResidentialInstallation
                                .getStatus(propertyId);

                if (status != InstallationStatus.NOT_STARTED) {

                    getLogger().warning(
                            "Residential site search cancelled: "
                            + "existing installation record for "
                            + propertyId
                    );

                    return;
                }
            }

        } catch (RuntimeException exception) {

            getLogger().log(
                    Level.WARNING,
                    "Residential site search cancelled: "
                    + "installation status could not be verified.",
                    exception
            );

            return;
        }

        /*
         * Diagnostic search must not run while
         * residential construction is enabled.
         */

        if (EarthGuemesGenerator
                .isResidentialGenerationEnabled()) {

            getLogger().warning(
                    "Residential site search cancelled: "
                    + "construction is already enabled."
            );

            return;
        }

        /*
         * Do not search for new locations while
         * residential sites are registered.
         */

        if (EarthGuemesGenerator
                .getResidentialSiteCount() > 0) {

            getLogger().warning(
                    "Residential site search cancelled: "
                    + "residential sites are already registered."
            );

            return;
        }

        /*
         * Require residential configuration to
         * remain disabled during diagnostic searches.
         */

        if (getConfig().getBoolean(
                "residential.enabled",
                false
        )) {

            getLogger().warning(
                    "Residential site search cancelled: "
                    + "disable residential development first."
            );

            return;
        }

        getLogger().info(
                "========================================"
        );

        getLogger().info(
                "EARTHBOUND RESIDENTIAL SITE SEARCH"
        );

        getLogger().info(
                "Diagnostic mode: enabled"
        );

        getLogger().info(
                "Residential construction: disabled"
        );

        getLogger().info(
                "Searching for six Alpha home locations..."
        );

        getLogger().info(
                "Diagnostic search may extend startup time."
        );

        long startTime = System.nanoTime();

        try {

            var candidates =
                    EarthAlphaSiteFinder.findSites();

            getLogger().info(
                    "Residential candidates located: "
                    + candidates.size()
                    + " / "
                    + REQUIRED_ALPHA_HOMES
            );

            if (candidates.size() == REQUIRED_ALPHA_HOMES) {

                getLogger().info(
                        "Six candidate residential sites found."
                );

                getLogger().info(
                        "Review all reported coordinates "
                        + "before approving development."
                );

            } else {

                getLogger().warning(
                        "Residential search did not locate "
                        + "all six required properties."
                );

                getLogger().warning(
                        "Residential construction remains locked."
                );
            }

        } catch (RuntimeException exception) {

            getLogger().log(
                    Level.WARNING,
                    "Residential diagnostic search failed.",
                    exception
            );

        } finally {

            long elapsedMilliseconds =
                    (System.nanoTime() - startTime)
                            / 1000000L;

            getLogger().info(
                    "Residential search duration: "
                    + elapsedMilliseconds
                    + " ms"
            );

            getLogger().info(
                    "Residential site search finished."
            );

            getLogger().info(
                    "Diagnostic results were not "
                    + "automatically registered."
            );

            getLogger().info(
                    "========================================"
            );
        }
    }

    /*
     * ========================================================
     * GEOGRAPHIC DATA INITIALIZATION
     * ========================================================
     */

    private boolean initializeGeographicData() {

        /*
         * LOAD GUEMES TERRAIN
         */

        EarthTerrainDownloader.loadGuemesIsland();

        boolean terrainLoaded =
                EarthTerrainDownloader.isLoaded()
                && EarthTerrainLoader.isLoaded();

        getLogger().info(
                "Guemes terrain loaded: " + terrainLoaded
        );

        /*
         * LOAD GUEMES WATER
         */

        boolean waterLoaded =
                EarthWaterData.loadGuemesWaterMask();

        getLogger().info(
                "Water loaded: " + waterLoaded
        );

        /*
         * LOAD GUEMES ROADS
         */

        boolean roadsLoaded =
                EarthRoadData.loadGuemesRoadMask();

        getLogger().info(
                "Roads loaded: " + roadsLoaded
        );

        /*
         * REQUIRE GEOGRAPHIC DATA
         */

        if (!terrainLoaded
                || !waterLoaded
                || !roadsLoaded) {

            getLogger().warning(
                    "Geographic data loading incomplete."
            );

            getLogger().warning(
                    "Road smoothing and residential "
                    + "development will remain disabled."
            );

            return false;
        }

        /*
         * BUILD SMOOTHED ROAD ELEVATION PROFILES
         */

        boolean profilesLoaded =
                EarthRoadElevationProfiles.load();

        getLogger().info(
                "Road elevation profiles loaded: "
                + profilesLoaded
        );

        if (!profilesLoaded) {

            getLogger().warning(
                    "Road smoothing profiles unavailable. "
                    + "Using original road generation."
            );

            return false;
        }

        getLogger().info(
                "Road elevation profile count: "
                + EarthRoadElevationProfiles
                        .getProfileCount()
        );

        /*
         * BUILD ROAD SPATIAL INDEX
         */

        boolean spatialIndexLoaded =
                EarthRoadSpatialIndex.load();

        getLogger().info(
                "Road spatial index loaded: "
                + spatialIndexLoaded
        );

        if (!spatialIndexLoaded) {

            getLogger().warning(
                    "Road spatial index unavailable. "
                    + "Using original road generation."
            );

            return false;
        }

        getLogger().info(
                "Indexed road cells: "
                + EarthRoadSpatialIndex
                        .getIndexedCellCount()
        );

        getLogger().info(
                "EarthBound geographic systems ready!"
        );

        return true;
    }

    /*
     * ========================================================
     * SAVE EARTHBOUND DATA ON SHUTDOWN
     * ========================================================
     */

    @Override
    public void onDisable() {

        if (EarthPropertyManager.save()) {

            getLogger().info(
                    "EarthBound properties saved!"
            );

        } else {

            getLogger().warning(
                    "EarthBound property data could not be saved."
            );
        }

        EarthEconomy.save();

        getLogger().info(
                "EarthBound economy saved!"
        );

        EarthPropertyBannerRegistry.clear();

        getLogger().info(
                "EarthBound banner registry cleared!"
        );

        EarthGuemesGenerator.clearResidentialSites();

        getLogger().info(
                "EarthBound residential registry cleared!"
        );

        /*
         * RESIDENTIAL INSTALLATION SHUTDOWN
         *
         * Every successful installation state change
         * is already saved immediately.
         *
         * Shutdown only releases the in-memory ledger.
         * Persistent installation records are retained.
         */

        EarthResidentialInstallation.shutdown();

        getLogger().info(
                "EarthBound residential installation "
                + "tracking closed."
        );

        EarthRoadSpatialIndex.clear();

        EarthRoadElevationProfiles.clear();

        getLogger().info(
                "EarthBound road systems cleared!"
        );

        getLogger().info(
                "EarthBound shutdown complete."
        );
    }

    /*
     * ========================================================
     * EARTH WORLD GENERATOR
     * ========================================================
     */

    @Override
    public ChunkGenerator getDefaultWorldGenerator(
            String worldName,
            String id
    ) {

        return new EarthGenerator();
    }
}
