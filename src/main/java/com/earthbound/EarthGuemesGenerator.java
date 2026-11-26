package com.earthbound;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;

import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import com.earthbound.water.EarthWaterData;
import com.earthbound.roads.EarthRoadData;
import com.earthbound.roads.EarthRoadSpatialIndex;
import com.earthbound.terrain.EarthTerrainLoader;
import com.earthbound.terrain.EarthElevation;

import com.earthbound.EarthAlphaResidentialGenerator.ResidentialSite;

public final class EarthGuemesGenerator {

    private static final int MINIMUM_BUILD_HEIGHT =
            EarthWaterData.SEA_LEVEL + 3;

    private static final int MAX_ALPHA_HOMES = 6;

    /*
     * Construction safety lock.
     *
     * Keep locked until residential locations and
     * existing world chunks have been reviewed.
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
     * LOAD RESIDENTIAL CONFIGURATION
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

            if (facing != BlockFace.NORTH
                    && facing != BlockFace.SOUTH
                    && facing != BlockFace.EAST
                    && facing != BlockFace.WEST) {

                return reject(
                        plugin,
                        propertyId,
                        "Facing must be cardinal."
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

            /*
             * The ownership system currently expects
             * world-aligned width and length.
             *
             * Reject rotated rectangular plots until
             * ownership dimensions support rotation.
             */

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
         * Friends Alpha requires six approved homes.
         */

        if (configuredSites.size() != MAX_ALPHA_HOMES) {

            plugin.getLogger().warning(
                    "Residential development requires "
                    + "six configured Alpha homes."
            );

            return false;
        }

        if (!registerResidentialSites(configuredSites)) {

            plugin.getLogger().warning(
                    "Residential site validation failed."
            );

            return false;
        }

        residentialEnabled = CONSTRUCTION_APPROVED;

        plugin.getLogger().info(
                "Validated residential properties: "
                + getResidentialSiteCount()
        );

        plugin.getLogger().info(
                "Residential construction enabled: "
                + residentialEnabled
        );

        return true;
    }

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
     * RESIDENTIAL SITE REGISTRATION
     */

    public static synchronized boolean registerResidentialSites(
            List<ResidentialSite> sites
    ) {

        if (sites == null
                || sites.size() > MAX_ALPHA_HOMES) {

            return false;
        }

        if (!EarthTerrainLoader.isLoaded()
                || !EarthRoadData.isLoaded()) {

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

            for (ResidentialSite approved
                    : approvedSites) {

                if (site.overlaps(approved)) {

                    return false;
                }
            }

            approvedSites.add(site);
        }

        residentialSites =
                List.copyOf(approvedSites);

        return true;
    }

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

        return residentialEnabled;
    }

    public static synchronized void clearResidentialSites() {

        residentialEnabled = false;

        residentialSites = List.of();
    }

    /*
     * GUEMES ISLAND GENERATION
     */

    public static void generate(
            ChunkData chunkData,
            int chunkX,
            int chunkZ
    ) {

        if (chunkData == null
                || !residentialEnabled
                || !EarthRoadSpatialIndex.isLoaded()) {

            return;
        }

        List<ResidentialSite> sites =
                residentialSites;

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
     * DEVELOPMENT ELEVATION CHECK
     */

    public static boolean isSuitableForDevelopment(
            double latitude,
            double longitude
    ) {

        if (!EarthTerrainLoader.isLoaded()) {
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

        return minecraftHeight >= MINIMUM_BUILD_HEIGHT;
    }
}
