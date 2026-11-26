package com.earthbound;

import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

public class EarthPropertySaleMarkers {

    /*
     * ==========================================================
     * INITIALIZE PROPERTY SALE MARKERS
     * ==========================================================
     */

    public static void setup(EarthBound plugin) {

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

            if (property == null || property.isOwned()) {
                continue;
            }

            Location bannerLocation =
                    EarthPropertyBannerRegistry.getBannerLocation(
                            propertyId
                    );

            if (bannerLocation == null) {
                continue;
            }

            Location signLocation =
                    getSignLocation(home);

            if (signLocation == null) {

                plugin.getLogger().warning(
                        "Missing property sign coordinates: "
                        + propertyId
                );

                continue;
            }

            if (!property.contains(
                    signLocation.getWorld().getName(),
                    signLocation.getBlockX(),
                    signLocation.getBlockZ())) {

                plugin.getLogger().warning(
                        "Sign is outside property: "
                        + propertyId
                );

                continue;
            }

            if (bannerLocation.getBlock().equals(
                    signLocation.getBlock())) {

                plugin.getLogger().warning(
                        "Banner and sign locations overlap: "
                        + propertyId
                );

                continue;
            }

            if (placeMarkers(
                    plugin,
                    property,
                    bannerLocation,
                    signLocation)) {

                placed++;
            }
        }

        plugin.getLogger().info(
                "EarthBound placed or verified "
                + placed
                + " property sale marker sets."
        );
    }


    /*
     * ==========================================================
     * GET PROPERTY SIGN LOCATION
     * ==========================================================
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
     * ==========================================================
     * PLACE BLUE BANNER AND PRICE SIGN
     * ==========================================================
     */

    private static boolean placeMarkers(
            EarthBound plugin,
            EarthProperty property,
            Location bannerLocation,
            Location signLocation
    ) {

        Block bannerBlock = bannerLocation.getBlock();
        Block signBlock = signLocation.getBlock();

        NamespacedKey markerKey =
                new NamespacedKey(plugin, "sale_property");

        String propertyId = property.getId();

        boolean existingBanner =
                bannerBlock.getType() == Material.BLUE_BANNER
                && bannerBlock.getState()
                        instanceof org.bukkit.block.Banner banner
                && propertyId.equals(
                        banner.getPersistentDataContainer().get(
                                markerKey,
                                PersistentDataType.STRING
                        )
                );

        boolean existingSign =
                signBlock.getType() == Material.OAK_SIGN
                && signBlock.getState() instanceof Sign sign
                && propertyId.equals(
                        sign.getPersistentDataContainer().get(
                                markerKey,
                                PersistentDataType.STRING
                        )
                );


        /*
         * NEVER REPLACE UNRELATED BLOCKS
         */

        if (!bannerBlock.getType().isAir()
                && !existingBanner) {

            plugin.getLogger().warning(
                    "Property banner location obstructed: "
                    + propertyId
            );

            return false;
        }

        if (!signBlock.getType().isAir()
                && !existingSign) {

            plugin.getLogger().warning(
                    "Property sign location obstructed: "
                    + propertyId
            );

            return false;
        }


        /*
         * VERIFY SOLID GROUND
         */

        if (!bannerBlock.getRelative(0, -1, 0)
                .getType().isSolid()) {

            return false;
        }

        if (!signBlock.getRelative(0, -1, 0)
                .getType().isSolid()) {

            return false;
        }


        /*
         * PLACE BLUE FOR SALE BANNER
         */

        if (!existingBanner) {

            bannerBlock.setType(
                    Material.BLUE_BANNER,
                    false
            );

            if (bannerBlock.getState()
                    instanceof org.bukkit.block.Banner banner) {

                banner.getPersistentDataContainer().set(
                        markerKey,
                        PersistentDataType.STRING,
                        propertyId
                );

                banner.update(true, false);
            }
        }


        /*
         * PLACE PROPERTY PRICE SIGN
         */

        if (!existingSign) {

            signBlock.setType(
                    Material.OAK_SIGN,
                    false
            );
        }

        if (signBlock.getState() instanceof Sign sign) {

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

            sign.setLine(3, "Click Blue Banner");

            sign.getPersistentDataContainer().set(
                    markerKey,
                    PersistentDataType.STRING,
                    propertyId
            );

            sign.setWaxed(true);

            sign.update(true, false);
        }

        plugin.getLogger().info(
                "Property sale markers ready: "
                + propertyId
        );

        return true;
    }


    /*
     * ==========================================================
     * SHORTEN SIGN TEXT
     * ==========================================================
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
