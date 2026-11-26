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

public class EarthPropertySaleMarkers {

    private static EarthBound plugin;
    private static NamespacedKey markerKey;


    /*
     * INITIALIZE PROPERTY SALE SYSTEM
     */

    public static void setup(EarthBound earthBound) {

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

        int placed = 0;

        for (String propertyId : homes.getKeys(false)) {

            ConfigurationSection home =
                    homes.getConfigurationSection(propertyId);

            if (home == null
                    || !home.getBoolean("enabled", false)) {
                continue;
            }

            EarthProperty property =
                    EarthPropertyManager.getProperty(propertyId);

            if (property == null) {
                continue;
            }

            if (property.isOwned()) {

                removeMarkers(propertyId);
                continue;
            }

            Location bannerLocation =
                    EarthPropertyBannerRegistry.getBannerLocation(
                            propertyId
                    );

            Location signLocation =
                    getSignLocation(home);

            if (bannerLocation == null
                    || signLocation == null) {

                plugin.getLogger().warning(
                        "Missing sale marker coordinates: "
                        + propertyId
                );

                continue;
            }

            if (!property.contains(
                    signLocation.getWorld().getName(),
                    signLocation.getBlockX(),
                    signLocation.getBlockZ())) {

                continue;
            }

            if (bannerLocation.getBlock().equals(
                    signLocation.getBlock())) {

                continue;
            }

            BlockFace facing = getFacing(home);

            if (placeMarkers(
                    property,
                    bannerLocation,
                    signLocation,
                    facing)) {

                placed++;
            }
        }

        plugin.getLogger().info(
                "EarthBound sale marker sets ready: "
                + placed
        );
    }


    /*
     * READ ROAD-FACING DIRECTION
     */

    private static BlockFace getFacing(
            ConfigurationSection home
    ) {

        String direction =
                home.getString("marker-facing", "NORTH");

        try {

            BlockFace facing = BlockFace.valueOf(
                    direction.toUpperCase(Locale.ROOT)
            );

            if (facing == BlockFace.NORTH
                    || facing == BlockFace.SOUTH
                    || facing == BlockFace.EAST
                    || facing == BlockFace.WEST) {

                return facing;
            }

        } catch (IllegalArgumentException ignored) {
        }

        return BlockFace.NORTH;
    }


    /*
     * GET CONFIGURED SIGN LOCATION
     */

    private static Location getSignLocation(
            ConfigurationSection home
    ) {

        if (!home.isInt("sign-x")
                || !home.isInt("sign-y")
                || !home.isInt("sign-z")) {

            return null;
        }

        World world = Bukkit.getWorld(
                home.getString("world", "earthbound")
        );

        if (world == null) {
            return null;
        }

        int x = home.getInt("sign-x");
        int y = home.getInt("sign-y");
        int z = home.getInt("sign-z");

        if (y <= world.getMinHeight()
                || y >= world.getMaxHeight()) {

            return null;
        }

        return new Location(world, x, y, z);
    }


    /*
     * VERIFY OFFICIAL MARKER METADATA
     */

    private static boolean hasMarker(
            Block block,
            String propertyId
    ) {

        if (markerKey == null
                || block == null
                || propertyId == null) {

            return false;
        }

        if (!(block.getState() instanceof TileState state)) {
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
     * VERIFY OFFICIAL BLUE BANNER
     */

    public static boolean isOfficialBanner(
            Block block,
            String propertyId
    ) {

        if (block == null) {
            return false;
        }

        if (block.getType() != Material.BLUE_BANNER) {
            return false;
        }

        return EarthPropertyBannerRegistry.isOfficialBanner(
                propertyId,
                block.getLocation()
        ) && hasMarker(block, propertyId);
    }


    /*
     * SET ROAD-FACING ROTATION
     */

    private static void setRotation(
            Block block,
            BlockFace facing
    ) {

        BlockData data = block.getBlockData();

        if (data instanceof Rotatable rotatable) {

            rotatable.setRotation(facing);

            block.setBlockData(rotatable, false);
        }
    }


    /*
     * PLACE PROPERTY SALE MARKERS
     */

    private static boolean placeMarkers(
            EarthProperty property,
            Location bannerLocation,
            Location signLocation,
            BlockFace facing
    ) {

        String propertyId = property.getId();

        Block bannerBlock = bannerLocation.getBlock();
        Block signBlock = signLocation.getBlock();

        boolean existingBanner =
                bannerBlock.getType() == Material.BLUE_BANNER
                && hasMarker(bannerBlock, propertyId);

        boolean existingSign =
                signBlock.getType() == Material.OAK_SIGN
                && hasMarker(signBlock, propertyId);


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

        if (bannerBlock.getY()
                <= bannerBlock.getWorld().getMinHeight()) {

            return false;
        }

        if (!bannerBlock.getRelative(0, -1, 0)
                .getType().isSolid()) {

            return false;
        }

        if (!signBlock.getRelative(0, -1, 0)
                .getType().isSolid()) {

            return false;
        }


        /*
         * PLACE BLUE BANNER
         */

        if (!existingBanner) {

            bannerBlock.setType(
                    Material.BLUE_BANNER,
                    false
            );
        }

        setRotation(bannerBlock, facing);

        if (!(bannerBlock.getState()
                instanceof Banner banner)) {

            return false;
        }

        banner.getPersistentDataContainer().set(
                markerKey,
                PersistentDataType.STRING,
                propertyId
        );

        if (!banner.update(true, false)) {
            return false;
        }


        /*
         * PLACE PRICE SIGN
         */

        if (!existingSign) {

            signBlock.setType(
                    Material.OAK_SIGN,
                    false
            );
        }

        setRotation(signBlock, facing);

        if (!(signBlock.getState() instanceof Sign sign)) {
            return false;
        }

        sign.setWaxed(false);

        sign.setLine(0, "FOR SALE");

        sign.setLine(
                1,
                shorten(property.getName(), 15)
        );

        sign.setLine(
                2,
                String.format(
                        Locale.US,
                        "$%,.0f",
                        property.getPrice()
                )
        );

        sign.setLine(3, "Click Banner");

        sign.getPersistentDataContainer().set(
                markerKey,
                PersistentDataType.STRING,
                propertyId
        );

        sign.setWaxed(true);

        if (!sign.update(true, false)) {
            return false;
        }

        plugin.getLogger().info(
                "Sale markers ready: "
                + propertyId
        );

        return true;
    }


    /*
     * REMOVE OFFICIAL MARKERS AFTER PURCHASE
     */

    public static void removeMarkers(String propertyId) {

        if (plugin == null || propertyId == null) {
            return;
        }

        EarthProperty property =
                EarthPropertyManager.getProperty(propertyId);

        if (property == null || !property.isOwned()) {
            return;
        }

        Location bannerLocation =
                EarthPropertyBannerRegistry.getBannerLocation(
                        propertyId
                );

        if (bannerLocation != null) {

            Block block = bannerLocation.getBlock();

            if (isOfficialBanner(block, propertyId)) {

                block.setType(Material.AIR, false);
            }
        }

        ConfigurationSection home =
                plugin.getConfig().getConfigurationSection(
                        "properties.alpha-homes." + propertyId
                );

        if (home != null) {

            Location signLocation = getSignLocation(home);

            if (signLocation != null
                    && property.contains(
                            signLocation.getWorld().getName(),
                            signLocation.getBlockX(),
                            signLocation.getBlockZ())) {

                Block sign = signLocation.getBlock();

                if (sign.getType() == Material.OAK_SIGN
                        && hasMarker(sign, propertyId)) {

                    sign.setType(Material.AIR, false);
                }
            }
        }

        plugin.getLogger().info(
                "Sale marker cleanup finished: "
                + propertyId
        );
    }


    /*
     * SHORTEN SIGN TEXT
     */

    private static String shorten(
            String text,
            int maximumLength
    ) {

        if (text == null) {
            return "";
        }

        if (text.length() <= maximumLength) {
            return text;
        }

        return text.substring(0, maximumLength);
    }

}
