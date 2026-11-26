package com.earthbound;

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

public class EarthBound extends JavaPlugin {

    /*
     * GUEMES ISLAND SPAWN
     */

    private static final double GUEMES_SPAWN_X = -654.0;
    private static final double GUEMES_SPAWN_Y = 86.0;
    private static final double GUEMES_SPAWN_Z = -355.0;

    /*
     * RESIDENTIAL SITE SEARCH DIAGNOSTICS
     */

    private static final String SITE_SEARCH_CONFIG =
            "diagnostics.residential-site-search";

    /*
     * ENABLE EARTHBOUND
     */

    @Override
    public void onEnable() {

        getLogger().info("EarthBound starting...");

        saveDefaultConfig();

        EarthGuemesGenerator.clearResidentialSites();

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
         */

        if (geographicDataReady) {

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

            getLogger().warning(
                    "Residential configuration skipped: "
                    + "geographic data is not ready."
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
         *
         * Safety inspection must occur before any
         * residential sale marker initialization.
         */

        Bukkit.getScheduler().runTask(
                this,
                () -> initializeResidentialWorld(
                        geographicDataReady
                )
        );

        /*
         * STARTUP COMPLETE
         */

        getLogger().info(
                "EarthBound enabled!"
        );
    }

    /*
     * ========================================================
     * RESIDENTIAL WORLD INITIALIZATION
     * ========================================================
     *
     * Read-only residential world inspection.
     *
     * Does not load or generate property chunks.
     * Does not authorize residential construction.
     * Does not install property sale markers.
     */

    private void initializeResidentialWorld(
            boolean geographicDataReady
    ) {

        /*
         * Clear any previously registered official
         * sale banner locations.
         */

        EarthPropertyBannerRegistry.clear();

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

        if (registeredSites != 6) {

            getLogger().warning(
                    "Residential world inspection cancelled: "
                    + "six validated properties are required."
            );

            getLogger().warning(
                    "Registered residential properties: "
                    + registeredSites
                    + " / 6"
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
                    java.util.logging.Level.WARNING,
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

        /*
         * Fail closed if any inspection fails.
         */

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
         * A successful safety inspection is not
         * construction authorization.
         *
         * It is only a snapshot of current conditions.
         *
         * Persistent installation tracking and final
         * authorization are still required.
         */

        getLogger().info(
                "Residential world safety inspection passed."
        );

        getLogger().info(
                "All six properties passed the current "
                + "read-only inspection."
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
         * Existing real estate initialization methods
         * remain available for future controlled use:
         *
         * EarthPropertyBannerRegistry.setup(this);
         * EarthPropertySaleMarkers.setup(this);
         *
         * Do not invoke either method until installation
         * tracking confirms that homes are completed.
         */
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
         * approved residential sites are registered.
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
                    + " / 6"
            );

            if (candidates.size() == 6) {

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
                    java.util.logging.Level.WARNING,
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
     * GEOGRAPHIC DATA INITIALIZATION
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
     * SAVE EARTHBOUND DATA ON SHUTDOWN
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
     * EARTH WORLD GENERATOR
     */

    @Override
    public ChunkGenerator getDefaultWorldGenerator(
            String worldName,
            String id
    ) {

        return new EarthGenerator();
    }
}
