package com.earthbound;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;

public class EarthPropertyListener implements Listener {

    /*
     * ==========================================================
     * CLICK BLUE FOR SALE BANNER
     * ==========================================================
     */

    @EventHandler
    public void onBannerClick(PlayerInteractEvent event) {

        if (event.getAction()
                != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getHand()
                != EquipmentSlot.HAND) {
            return;
        }

        Block block = event.getClickedBlock();

        if (block == null) {
            return;
        }

        Material material = block.getType();

        if (material != Material.BLUE_BANNER
                && material != Material.BLUE_WALL_BANNER) {
            return;
        }

        /*
         * Only recognize blue banners that are
         * explicitly registered as For Sale markers.
         */

        EarthProperty property =
                EarthPropertyManager.getPropertyAt(
                        block.getLocation()
                );

        if (property == null) {
            return;
        }

        /*
         * Until official banner coordinates are
         * registered, do not treat every blue banner
         * inside a plot as a For Sale sign.
         */

        if (!EarthPropertyBannerRegistry.isOfficialBanner(
                property.getId(),
                block.getLocation()
        )) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();

        if (property.isOwned()) {

            player.sendMessage(
                    ChatColor.YELLOW
                    + "This home has already been sold."
            );

            return;
        }

        EarthPropertyMenu.open(
                player,
                property
        );
    }

    /*
     * ==========================================================
     * PROPERTY PURCHASE MENU CLICKS
     * ==========================================================
     */

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {

        Inventory topInventory =
                event.getView().getTopInventory();

        if (!EarthPropertyMenu.isPropertyMenu(
                topInventory
        )) {
            return;
        }

        /*
         * Protect the menu from item movement,
         * shift-clicking and other inventory actions.
         */

        event.setCancelled(true);

        if (!(event.getWhoClicked()
                instanceof Player)) {
            return;
        }

        Player player =
                (Player) event.getWhoClicked();

        int rawSlot = event.getRawSlot();

        if (rawSlot < 0
                || rawSlot >= topInventory.getSize()) {
            return;
        }

        /*
         * CANCEL BUTTON
         */

        if (EarthPropertyMenu.isCancelSlot(
                rawSlot
        )) {

            player.closeInventory();
            return;
        }

        /*
         * BUY HOME BUTTON
         */

        if (!EarthPropertyMenu.isBuySlot(
                rawSlot
        )) {
            return;
        }

        String propertyId =
                EarthPropertyMenu.getPropertyId(
                        topInventory
                );

        if (propertyId == null) {
            player.closeInventory();
            return;
        }

        EarthProperty property =
                EarthPropertyManager.getProperty(
                        propertyId
                );

        if (property == null
                || property.isOwned()) {

            player.sendMessage(
                    ChatColor.RED
                    + "This home is no longer available."
            );

            player.closeInventory();
            return;
        }

        /*
         * The property manager handles:
         *
         * - Balance validation
         * - One-home-per-player rule
         * - Government treasury distribution
         * - Ownership assignment
         * - Ownership saving
         */

        boolean purchased =
                EarthPropertyManager.purchaseProperty(
                        player,
                        propertyId
                );

        player.closeInventory();

        if (!purchased) {
            return;
        }

        /*
         * REMOVE FOR SALE BANNER
         *
         * Only remove a banner if it is still
         * the official marker for this property.
         */

        Location bannerLocation =
                EarthPropertyBannerRegistry.getBannerLocation(
                        propertyId
                );

        if (bannerLocation != null) {

            Block banner =
                    bannerLocation.getBlock();

            if (EarthPropertyBannerRegistry.isOfficialBanner(
                    propertyId,
                    bannerLocation
            )) {

                Material bannerType =
                        banner.getType();

                if (bannerType == Material.BLUE_BANNER
                        || bannerType == Material.BLUE_WALL_BANNER) {

                    banner.setType(
                            Material.AIR,
                            false
                    );
                }
            }
        }

        player.sendMessage(
                ChatColor.GREEN
                + "Your new home is now registered "
                + "in your name!"
        );
    }

    /*
     * ==========================================================
     * PREVENT DRAGGING ITEMS INTO PROPERTY MENU
     * ==========================================================
     */

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {

        if (EarthPropertyMenu.isPropertyMenu(
                event.getView().getTopInventory()
        )) {

            event.setCancelled(true);
        }
    }
}
