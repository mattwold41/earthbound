
package com.earthbound;

import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Banner;
import org.bukkit.block.Sign;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rotatable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.persistence.PersistentDataType;

import com.earthbound.EarthAlphaResidentialGenerator.ResidentialSite;
import com.earthbound.EarthResidentialInstallation.InstallationStatus;

public class EarthPropertySaleMarkers {

    private static EarthBound plugin;

    private static NamespacedKey markerKey;

    private static final String WORLD_NAME =
            "earthbound";

    /*
     * ============================================================
     * EXPLICIT PROPERTY SALE ACTIVATION LOCK
     * ============================================================
     *
     * Keep this false until residential construction,
     * verification, persistence and sale authorization
     * have received final approval.
     *
     * This lock does not authorize construction.
     *
     * Changing residential.enabled or enabling individual
     * properties must not independently activate sales.
     * ============================================================
     */

    private static final boolean SALE_ACTIVATION_APPROVED =
            false;

    private static final String SALE_AUTHORIZATION_CONFIG =
            "residential.allow-property-sales";


    /*
     * ============================================================
     * VALIDATE SALE SYSTEM ACCESS
     * ============================================================
     */

    public static boolean isSaleActivationEnabled(
            EarthBound earthBound
    ) {

        if (!SALE_ACTIVATION_APPROVED) {

            return false;
        }

        if (earthBound == null
                || !earthBound.isEnabled()
                || !Bukkit.isPrimaryThread()) {

            return false;
        }

        return earthBound.getConfig().getBoolean(
                "residential.enabled",
                false

        ) && earthBound.getConfig().getBoolean(
                SALE_AUTHORIZATION_CONFIG,
                false
        );
    }


    private static boolean validPropertyId(
            String propertyId
    ) {

        return propertyId != null
                && propertyId.matches(
                        "guemes_home_0[1-6]"
                );
    }


    /*
     * ============================================================
     * INITIALIZE PROPERTY SALE SYSTEM
     * ============================================================
     */

    public static void setup(
            EarthBound earthBound
    ) {

        if (earthBound == null
                || !earthBound.isEnabled()
                || !Bukkit.isPrimaryThread()) {

            return;
        }

        plugin = earthBound;

        markerKey = new NamespacedKey(
                plugin,
                "sale_property"
        );

        ConfigurationSection homes =
                plugin.getConfig().getConfigurationSection(
                        "properties.alpha-homes"
                );

        if (homes == null) {

            return;
        }

        boolean salesEnabled =
                isSaleActivationEnabled(plugin);

        if (!salesEnabled) {

            plugin.getLogger().info(
                    "Residential property sale activation "
                    + "remains locked."
            );
        }

        int placed = 0;

        int suspended = 0;

        int failed = 0;

        for (String propertyId : homes.getKeys(false)) {

            if (!validPropertyId(propertyId)) {

                continue;
            }

            ConfigurationSection home =
                    homes.getConfigurationSection(
                            propertyId
                    );

            if (home == null) {

                continue;
            }

            try {

                /*
                 * Clean up stale official markers when
                 * sales are disabled or the property
                 * no longer qualifies for sale.
                 *
                 * Never generate or load chunks here.
                 */

                if (!salesEnabled

                        || !home.getBoolean(
                                "enabled",
                                false
                        )

                        || !isPropertySaleEligible(
                                propertyId
                        )) {

                    cleanupConfiguredMarkers(
                            propertyId
                    );

                    suspended++;

                    continue;
                }

                EarthProperty property =
                        EarthPropertyManager.getProperty(
                                propertyId
                        );

                Location bannerLocation =
                        EarthPropertyBannerRegistry
                                .getBannerLocation(
                                        propertyId
                                );

                Location signLocation =
                        getSignLocation(home);

                BlockFace facing =
                        getFacing(home);

                if (!validMarkerLocations(
                        propertyId,
                        property,
                        bannerLocation,
                        signLocation,
                        facing
                )) {

                    cleanupConfiguredMarkers(
                            propertyId
                    );

                    failed++;

                    continue;
                }

                if (placeMarkers(
                        property,
                        bannerLocation,
                        signLocation,
                        facing
                )) {

                    placed++;

                } else {

                    cleanupConfiguredMarkers(
                            propertyId
                    );

                    failed++;
                }

            } catch (RuntimeException exception) {

                failed++;

                plugin.getLogger().severe(
                        "Sale marker processing failed: "
                        + propertyId
                        + " - "
                        + exception.getMessage()
                );

                try {

                    cleanupConfiguredMarkers(
                            propertyId
                    );

                } catch (RuntimeException cleanupFailure) {

                    plugin.getLogger().severe(
                            "Sale marker cleanup requires "
                            + "manual review: "
                            + propertyId
                    );
                }
            }
        }

        plugin.getLogger().info(
                "EarthBound sale marker sets ready: "
                + placed
        );

        plugin.getLogger().info(
                "Suspended residential sale marker sets: "
                + suspended
        );

        plugin.getLogger().info(
                "Residential sale marker failures: "
                + failed
        );
    }


    /*
     * ============================================================
     * VERIFIED PROPERTY SALE ELIGIBILITY
     * ============================================================
     *
     * Sale markers must not independently authorize
     * property purchases.
     *
     * Property management and economy transaction
     * protection remain separate security layers.
     * ============================================================
     */

    public static boolean isPropertySaleEligible(
            String propertyId
    ) {

        if (!isSaleActivationEnabled(plugin)

                || !validPropertyId(propertyId)

                || markerKey == null

                || !Bukkit.isPrimaryThread()) {

            return false;
        }

        try {

            if (!EarthResidentialInstallation.isReady()

                    || !EarthEconomy.isReady()

                    || !EarthPropertyManager.isReady()) {

                return false;
            }

            if (EarthResidentialExecution
                    .hasActiveExecution()) {

                return false;
            }

            if (EarthResidentialInstallation.getStatus(
                    propertyId
            ) != InstallationStatus.COMPLETED) {

                return false;
            }

            EarthProperty property =
                    EarthPropertyManager.getProperty(
                            propertyId
                    );

            if (property == null

                    || property.isOwned()

                    || !propertyId.equals(
                            property.getId()
                    )

                    || !WORLD_NAME.equals(
                            property.getWorldName()
                    )) {

                return false;
            }

            if (EarthEconomy.getPropertyOwner(
                    propertyId
            ) != null) {

                return false;
            }

            World world =
                    Bukkit.getWorld(WORLD_NAME);

            if (world == null

                    || !WORLD_NAME.equals(
                            world.getName()
                    )

                    || Bukkit.getWorld(
                            world.getUID()
                    ) != world) {

                return false;
            }

            ResidentialSite site =
                    EarthGuemesGenerator
                            .getResidentialSite(
                                    propertyId
                            );

            if (site == null

                    || site.getDesign() == null

                    || !propertyId.equals(
                            site.getPropertyId()
                    )) {

                return false;
            }

            if (!EarthResidentialInstallation
                    .matchesApprovedSite(
                            world,
                            site
                    )) {

                return false;
            }

            if (!EarthPropertyManager
                    .isPropertyPurchaseEligible(
                            propertyId
                    )) {

                return false;
            }

            return EarthResidentialInstallation.isReady()

                    && EarthResidentialInstallation
                            .getStatus(propertyId)
                            == InstallationStatus.COMPLETED

                    && !EarthResidentialExecution
                            .hasActiveExecution();

        } catch (RuntimeException exception) {

            if (plugin != null) {

                plugin.getLogger().warning(
                        "Residential sale eligibility denied: "
                        + propertyId
                        + " - "
                        + exception.getMessage()
                );
            }

            return false;
        }
    }


    /*
     * ============================================================
     * READ ROAD-FACING DIRECTION
     * ============================================================
     *
     * Reject missing or invalid directions.
     * Do not silently select a replacement direction.
     * ============================================================
     */

    private static BlockFace getFacing(
            ConfigurationSection home
    ) {

        if (home == null) {

            return null;
        }

        String direction =
                home.getString("marker-facing");

        if (direction == null
                || direction.isBlank()) {

            return null;
        }

        try {

            BlockFace facing =
                    BlockFace.valueOf(
                            direction.toUpperCase(
                                    Locale.ROOT
                            )
                    );

            if (facing == BlockFace.NORTH
                    || facing == BlockFace.SOUTH
                    || facing == BlockFace.EAST
                    || facing == BlockFace.WEST) {

                return facing;
            }

        } catch (IllegalArgumentException ignored) {

            return null;
        }

        return null;
    }


    /*
     * ============================================================
     * READ CONFIGURED MARKER LOCATIONS
     * ============================================================
     */

    private static Location getConfiguredLocation(
            ConfigurationSection home,
            String prefix
    ) {

        if (home == null
                || prefix == null
                || !Bukkit.isPrimaryThread()) {

            return null;
        }

        if (!home.isInt(prefix + "-x")
                || !home.isInt(prefix + "-y")
                || !home.isInt(prefix + "-z")) {

            return null;
        }

        String worldName =
                home.getString(
                        "world",
                        WORLD_NAME
                );

        if (!WORLD_NAME.equals(worldName)) {

            return null;
        }

        World world =
                Bukkit.getWorld(worldName);

        if (world == null

                || Bukkit.getWorld(
                        world.getUID()
                ) != world) {

            return null;
        }

        int x = home.getInt(prefix + "-x");

        int y = home.getInt(prefix + "-y");

        int z = home.getInt(prefix + "-z");

        if (Math.abs((long) x) > 29_999_900L

                || Math.abs((long) z) > 29_999_900L

                || y <= world.getMinHeight()

                || y >= world.getMaxHeight()) {

            return null;
        }

        return new Location(
                world,
                x,
                y,
                z
        );
    }


    private static Location getSignLocation(
            ConfigurationSection home
    ) {

        return getConfiguredLocation(
                home,
                "sign"
        );
    }


    /*
     * ============================================================
     * SAFE EXISTING CHUNK ACCESS
     * ============================================================
     *
     * Sale marker operations must never create chunks.
     *
     * Unloaded chunks must be handled later through
     * an explicitly approved marker processing system.
     * ============================================================
     */

    private static boolean isAvailableLocation(
            Location location
    ) {

        if (location == null
                || !Bukkit.isPrimaryThread()) {

            return false;
        }

        World world =
                location.getWorld();

        if (world == null

                || !WORLD_NAME.equals(
                        world.getName()
                )

                || Bukkit.getWorld(
                        world.getUID()
                ) != world) {

            return false;
        }

        if (location.getY()
                <= world.getMinHeight()

                || location.getY()
                >= world.getMaxHeight()) {

            return false;
        }

        int chunkX = Math.floorDiv(
                location.getBlockX(),
                16
        );

        int chunkZ = Math.floorDiv(
                location.getBlockZ(),
                16
        );

        return world.isChunkLoaded(
                chunkX,
                chunkZ

        ) && world.isChunkGenerated(
                chunkX,
                chunkZ
        );
    }


    private static boolean sameBlock(
            Location first,
            Location second
    ) {

        if (first == null
                || second == null
                || first.getWorld() == null
                || second.getWorld() == null) {

            return false;
        }

        return first.getWorld().getUID().equals(
                second.getWorld().getUID()

        ) && first.getBlockX()
                == second.getBlockX()

        && first.getBlockY()
                == second.getBlockY()

        && first.getBlockZ()
                == second.getBlockZ();
    }


    /*
     * ============================================================
     * VALIDATE APPROVED MARKER COORDINATES
     * ============================================================
     */

    private static boolean validMarkerLocations(
            String propertyId,
            EarthProperty property,
            Location bannerLocation,
            Location signLocation,
            BlockFace facing
    ) {

        if (!validPropertyId(propertyId)

                || property == null

                || !propertyId.equals(
                        property.getId()
                )

                || bannerLocation == null

                || signLocation == null

                || facing == null) {

            return false;
        }

        ResidentialSite site =
                EarthGuemesGenerator
                        .getResidentialSite(
                                propertyId
                        );

        if (site == null

                || facing != site.getFacing()) {

            return false;
        }

        World world =
                Bukkit.getWorld(WORLD_NAME);

        if (world == null

                || bannerLocation.getWorld() == null

                || signLocation.getWorld() == null

                || !world.getUID().equals(
                        bannerLocation
                                .getWorld()
                                .getUID()
                )

                || !world.getUID().equals(
                        signLocation
                                .getWorld()
                                .getUID()
                )) {

            return false;
        }

        if (bannerLocation.getBlockX()
                != site.getBannerX()

                || bannerLocation.getBlockY()
                != site.getBannerY()

                || bannerLocation.getBlockZ()
                != site.getBannerZ()) {

            return false;
        }

        if (signLocation.getBlockX()
                != site.getSignX()

                || signLocation.getBlockY()
                != site.getSignY()

                || signLocation.getBlockZ()
                != site.getSignZ()) {

            return false;
        }

        if (!property.contains(
                WORLD_NAME,
                bannerLocation.getBlockX(),
                bannerLocation.getBlockZ()

        ) || !property.contains(
                WORLD_NAME,
                signLocation.getBlockX(),
                signLocation.getBlockZ()
        )) {

            return false;
        }

        if (sameBlock(
                bannerLocation,
                signLocation
        )) {

            return false;
        }

        ConfigurationSection home =
                plugin.getConfig().getConfigurationSection(
                        "properties.alpha-homes."
                        + propertyId
                );

        Location configuredBanner =
                getConfiguredLocation(
                        home,
                        "banner"
                );

        if (!sameBlock(
                configuredBanner,
                bannerLocation
        )) {

            return false;
        }

        if (!EarthPropertyBannerRegistry
                .isOfficialBanner(
                        propertyId,
                        bannerLocation
                )) {

            return false;
        }

        return isAvailableLocation(
                bannerLocation

        ) && isAvailableLocation(
                signLocation
        );
    }


    /*
     * ============================================================
     * VERIFY OFFICIAL MARKER METADATA
     * ============================================================
     */

    private static boolean hasMarker(
            Block block,
            String propertyId
    ) {

        if (markerKey == null

                || block == null

                || !validPropertyId(propertyId)

                || !isAvailableLocation(
                        block.getLocation()
                )) {

            return false;
        }

        if (!(block.getState()
                instanceof TileState state)) {

            return false;
        }

        String storedId =
                state.getPersistentDataContainer().get(
                        markerKey,
                        PersistentDataType.STRING
                );

        return propertyId.equals(storedId);
    }


    /*
     * ============================================================
     * VERIFY PHYSICAL BLUE BANNER IDENTITY
     * ============================================================
     *
     * This helper only identifies the physical marker.
     *
     * It does not authorize the marker to advertise
     * a property or initiate a purchase.
     * ============================================================
     */

    private static boolean hasOfficialBannerIdentity(
            Block block,
            String propertyId
    ) {

        if (block == null

                || !validPropertyId(propertyId)

                || !isAvailableLocation(
                        block.getLocation()
                )) {

            return false;
        }

        if (block.getType()
                != Material.BLUE_BANNER) {

            return false;
        }

        return EarthPropertyBannerRegistry
                .isOfficialBanner(
                        propertyId,
                        block.getLocation()

        ) && hasMarker(
                block,
                propertyId
        );
    }


    /*
     * ============================================================
     * VERIFY ACTIVE OFFICIAL SALE BANNER
     * ============================================================
     *
     * An old banner or matching metadata alone
     * must never authorize a property sale.
     * ============================================================
     */

    public static boolean isOfficialBanner(
            Block block,
            String propertyId
    ) {

        if (!isPropertySaleEligible(propertyId)) {

            return false;
        }

        if (!hasOfficialBannerIdentity(
                block,
                propertyId
        )) {

            return false;
        }

        try {

            EarthProperty property =
                    EarthPropertyManager.getProperty(
                            propertyId
                    );

            ConfigurationSection home =
                    plugin.getConfig()
                            .getConfigurationSection(
                                    "properties.alpha-homes."
                                    + propertyId
                            );

            Location bannerLocation =
                    block.getLocation();

            Location signLocation =
                    getSignLocation(home);

            BlockFace facing =
                    getFacing(home);

            if (!validMarkerLocations(
                    propertyId,
                    property,
                    bannerLocation,
                    signLocation,
                    facing
            )) {

                return false;
            }

            return verifyMarkerPair(
                    property,
                    block,
                    signLocation.getBlock(),
                    facing
            );

        } catch (RuntimeException exception) {

            return false;
        }
    }


    /*
     * ============================================================
     * SET ROAD-FACING ROTATION
     * ============================================================
     */

    private static boolean setRotation(
            Block block,
            BlockFace facing
    ) {

        if (block == null
                || facing == null) {

            return false;
        }

        BlockData data =
                block.getBlockData();

        if (!(data instanceof Rotatable rotatable)) {

            return false;
        }

        rotatable.setRotation(facing);

        block.setBlockData(
                rotatable,
                false
        );

        return hasRotation(
                block,
                facing
        );
    }


    private static boolean hasRotation(
            Block block,
            BlockFace facing
    ) {

        if (block == null
                || facing == null) {

            return false;
        }

        BlockData data =
                block.getBlockData();

        return data instanceof Rotatable rotatable

                && rotatable.getRotation()
                        == facing;
    }


    /*
     * ============================================================
     * FORMAT PROPERTY PRICE
     * ============================================================
     */

    private static String formatPrice(
            double price
    ) {

        if (!Double.isFinite(price)
                || price <= 0.0) {

            return "";
        }

        if (price == Math.rint(price)) {

            return String.format(
                    Locale.US,
                    "$%,.0f",
                    price
            );
        }

        return String.format(
                Locale.US,
                "$%,.2f",
                price
        );
    }


    /*
     * ============================================================
     * VERIFY COMPLETE PHYSICAL MARKER PAIR
     * ============================================================
     */

    private static boolean verifyMarkerPair(
            EarthProperty property,
            Block bannerBlock,
            Block signBlock,
            BlockFace facing
    ) {

        if (property == null

                || bannerBlock == null

                || signBlock == null

                || facing == null) {

            return false;
        }

        String propertyId =
                property.getId();

        if (!hasOfficialBannerIdentity(
                bannerBlock,
                propertyId
        )) {

            return false;
        }

        if (!isAvailableLocation(
                signBlock.getLocation()
        )) {

            return false;
        }

        if (signBlock.getType()
                != Material.OAK_SIGN

                || !hasMarker(
                        signBlock,
                        propertyId
                )) {

            return false;
        }

        if (!hasRotation(
                bannerBlock,
                facing

        ) || !hasRotation(
                signBlock,
                facing
        )) {

            return false;
        }

        if (!bannerBlock.getRelative(
                0,
                -1,
                0
        ).getType().isSolid()

                || !signBlock.getRelative(
                        0,
                        -1,
                        0
                ).getType().isSolid()) {

            return false;
        }

        if (!(signBlock.getState()
                instanceof Sign sign)) {

            return false;
        }

        if (!sign.isWaxed()) {

            return false;
        }

        String expectedPrice =
                formatPrice(
                        property.getPrice()
                );

        if (expectedPrice.isEmpty()) {

            return false;
        }

        return "FOR SALE".equals(
                sign.getLine(0)

        ) && shorten(
                property.getName(),
                15

        ).equals(
                sign.getLine(1)

        ) && expectedPrice.equals(
                sign.getLine(2)

        ) && "Click Banner".equals(
                sign.getLine(3)
        );
    }


    /*
     * ============================================================
     * PLACE VERIFIED PROPERTY SALE MARKERS
     * ============================================================
     */

    private static boolean placeMarkers(
            EarthProperty property,
            Location bannerLocation,
            Location signLocation,
            BlockFace facing
    ) {

        if (property == null) {

            return false;
        }

        String propertyId =
                property.getId();

        if (!isPropertySaleEligible(
                propertyId
        )) {

            return false;
        }

        if (!validMarkerLocations(
                propertyId,
                property,
                bannerLocation,
                signLocation,
                facing
        )) {

            return false;
        }

        Block bannerBlock =
                bannerLocation.getBlock();

        Block signBlock =
                signLocation.getBlock();

        boolean existingBanner =
                hasOfficialBannerIdentity(
                        bannerBlock,
                        propertyId
                );

        boolean existingSign =
                signBlock.getType()
                        == Material.OAK_SIGN

                && hasMarker(
                        signBlock,
                        propertyId
                );

        /*
         * NEVER OVERWRITE UNRELATED BLOCKS
         */

        if (!bannerBlock.getType().isAir()
                && !existingBanner) {

            plugin.getLogger().warning(
                    "Banner location obstructed: "
                    + propertyId
            );

            return false;
        }

        if (!signBlock.getType().isAir()
                && !existingSign) {

            plugin.getLogger().warning(
                    "Sign location obstructed: "
                    + propertyId
            );

            return false;
        }

        if (!bannerBlock.getRelative(
                0,
                -1,
                0
        ).getType().isSolid()) {

            return false;
        }

        if (!signBlock.getRelative(
                0,
                -1,
                0
        ).getType().isSolid()) {

            return false;
        }

        String priceText =
                formatPrice(
                        property.getPrice()
                );

        if (priceText.isEmpty()) {

            return false;
        }

        /*
         * Record original empty block data so newly
         * placed markers can be rolled back if needed.
         */

        BlockData originalBannerData =
                bannerBlock.getBlockData().clone();

        BlockData originalSignData =
                signBlock.getBlockData().clone();

        boolean createdBanner = false;

        boolean createdSign = false;

        boolean completed = false;

        try {

            /*
             * PLACE BLUE BANNER
             */

            if (!existingBanner) {

                createdBanner = true;

                bannerBlock.setType(
                        Material.BLUE_BANNER,
                        false
                );
            }

            if (!setRotation(
                    bannerBlock,
                    facing
            )) {

                return false;
            }

            if (!(bannerBlock.getState()
                    instanceof Banner banner)) {

                return false;
            }

            banner.getPersistentDataContainer().set(
                    markerKey,
                    PersistentDataType.STRING,
                    propertyId
            );

            if (!banner.update(
                    true,
                    false
            )) {

                return false;
            }

            /*
             * PLACE PRICE SIGN
             */

            if (!existingSign) {

                createdSign = true;

                signBlock.setType(
                        Material.OAK_SIGN,
                        false
                );
            }

            if (!setRotation(
                    signBlock,
                    facing
            )) {

                return false;
            }

            if (!(signBlock.getState()
                    instanceof Sign sign)) {

                return false;
            }

            sign.setWaxed(false);

            sign.setLine(
                    0,
                    "FOR SALE"
            );

            sign.setLine(
                    1,
                    shorten(
                            property.getName(),
                            15
                    )
            );

            sign.setLine(
                    2,
                    priceText
            );

            sign.setLine(
                    3,
                    "Click Banner"
            );

            sign.getPersistentDataContainer().set(
                    markerKey,
                    PersistentDataType.STRING,
                    propertyId
            );

            sign.setWaxed(true);

            if (!sign.update(
                    true,
                    false
            )) {

                return false;
            }

            /*
             * Final eligibility and physical verification.
             */

            if (!isPropertySaleEligible(
                    propertyId
            )) {

                return false;
            }

            if (!verifyMarkerPair(
                    property,
                    bannerBlock,
                    signBlock,
                    facing
            )) {

                return false;
            }

            completed = true;

            plugin.getLogger().info(
                    "Verified sale markers ready: "
                    + propertyId
            );

            return true;

        } catch (RuntimeException exception) {

            plugin.getLogger().severe(
                    "Sale marker placement failed: "
                    + propertyId
                    + " - "
                    + exception.getMessage()
            );

            return false;

        } finally {

            if (!completed) {

                /*
                 * Remove only markers created by this
                 * attempt or previously identified as
                 * official markers for this property.
                 */

                rollbackMarker(
                        signBlock,
                        propertyId,
                        Material.OAK_SIGN,
                        createdSign,
                        originalSignData
                );

                rollbackMarker(
                        bannerBlock,
                        propertyId,
                        Material.BLUE_BANNER,
                        createdBanner,
                        originalBannerData
                );
            }
        }
    }


    /*
     * ============================================================
     * ROLLBACK FAILED MARKER PLACEMENT
     * ============================================================
     */

    private static void rollbackMarker(
            Block block,
            String propertyId,
            Material expectedMaterial,
            boolean created,
            BlockData originalData
    ) {

        if (block == null

                || originalData == null

                || !isAvailableLocation(
                        block.getLocation()
                )) {

            return;
        }

        try {

            if (block.getType()
                    != expectedMaterial) {

                return;
            }

            if (created) {

                block.setBlockData(
                        originalData,
                        false
                );

                return;
            }

            /*
             * Previously existing official markers
             * must not remain active following
             * an unsuccessful placement attempt.
             */

            if (hasMarker(
                    block,
                    propertyId
            )) {

                block.setType(
                        Material.AIR,
                        false
                );
            }

        } catch (RuntimeException exception) {

            plugin.getLogger().severe(
                    "Sale marker rollback failed: "
                    + propertyId
                    + " - "
                    + exception.getMessage()
            );
        }
    }


    /*
     * ============================================================
     * REMOVE OFFICIAL MARKER AT CONFIGURED LOCATION
     * ============================================================
     *
     * Cleanup does not require sale activation.
     *
     * Do not remove unrelated blocks, load missing
     * chunks or scan arbitrary world locations.
     * ============================================================
     */

    private static boolean removeTaggedMarker(
            Location location,
            String propertyId,
            Material expectedMaterial
    ) {

        if (!validPropertyId(propertyId)

                || markerKey == null

                || !isAvailableLocation(
                        location
                )) {

            return false;
        }

        EarthProperty property =
                EarthPropertyManager.getProperty(
                        propertyId
                );

        if (property == null

                || !propertyId.equals(
                        property.getId()
                )

                || !property.contains(
                        WORLD_NAME,
                        location.getBlockX(),
                        location.getBlockZ()
                )) {

            return false;
        }

        Block block =
                location.getBlock();

        if (block.getType()
                != expectedMaterial) {

            return false;
        }

        if (!hasMarker(
                block,
                propertyId
        )) {

            return false;
        }

        block.setType(
                Material.AIR,
                false
        );

        return block.getType().isAir();
    }


    /*
     * ============================================================
     * CLEAN UP SUSPENDED OR OUTDATED MARKERS
     * ============================================================
     */

    private static void cleanupConfiguredMarkers(
            String propertyId
    ) {

        if (plugin == null

                || markerKey == null

                || !validPropertyId(propertyId)

                || !Bukkit.isPrimaryThread()) {

            return;
        }

        ConfigurationSection home =
                plugin.getConfig()
                        .getConfigurationSection(
                                "properties.alpha-homes."
                                + propertyId
                        );

        if (home == null) {

            return;
        }

        Location configuredBanner =
                getConfiguredLocation(
                        home,
                        "banner"
                );

        Location registeredBanner =
                EarthPropertyBannerRegistry
                        .getBannerLocation(
                                propertyId
                        );

        Location signLocation =
                getSignLocation(home);

        int removed = 0;

        if (removeTaggedMarker(
                configuredBanner,
                propertyId,
                Material.BLUE_BANNER
        )) {

            removed++;
        }

        if (!sameBlock(
                configuredBanner,
                registeredBanner
        )) {

            if (removeTaggedMarker(
                    registeredBanner,
                    propertyId,
                    Material.BLUE_BANNER
            )) {

                removed++;
            }
        }

        if (removeTaggedMarker(
                signLocation,
                propertyId,
                Material.OAK_SIGN
        )) {

            removed++;
        }

        if (removed > 0) {

            plugin.getLogger().info(
                    "Inactive official sale markers removed: "
                    + propertyId
                    + " - "
                    + removed
            );
        }
    }


    /*
     * ============================================================
     * REMOVE OFFICIAL MARKERS AFTER PURCHASE
     * ============================================================
     *
     * Preserve the existing public cleanup API.
     *
     * Persistent ownership is authoritative.
     *
     * This operation must still work when
     * property sale activation is disabled.
     * ============================================================
     */

    public static void removeMarkers(
            String propertyId
    ) {

        if (plugin == null

                || markerKey == null

                || !validPropertyId(propertyId)

                || !Bukkit.isPrimaryThread()) {

            return;
        }

        try {

            if (!EarthEconomy.isReady()) {

                return;
            }

            if (EarthEconomy.getPropertyOwner(
                    propertyId
            ) == null) {

                return;
            }

            cleanupConfiguredMarkers(
                    propertyId
            );

        } catch (RuntimeException exception) {

            plugin.getLogger().warning(
                    "Purchased property marker cleanup "
                    + "requires review: "
                    + propertyId
                    + " - "
                    + exception.getMessage()
            );
        }
    }


    /*
     * ============================================================
     * SHORTEN SIGN TEXT
     * ============================================================
     */

    private static String shorten(
            String text,
            int maximumLength
    ) {

        if (text == null) {

            return "";
        }

        if (text.length()
                <= maximumLength) {

            return text;
        }

        return text.substring(
                0,
                maximumLength
        );
    }

}
