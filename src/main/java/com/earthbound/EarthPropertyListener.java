package com.earthbound;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

    private final EarthBound plugin;

    private final Set<UUID> pendingPurchases =
            new HashSet<>();


    /*
     * INITIALIZE LISTENER
     */

    public EarthPropertyListener(EarthBound plugin) {

        this.plugin = plugin;
    }


    /*
     * CLICK BLUE FOR SALE BANNER
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

        EarthProperty property =
                EarthPropertyManager.getPropertyAt(
                        block.getLocation()
                );

        if (property == null) {
            return;
        }

        if (!EarthPropertySaleMarkers.isOfficialBanner(
                block,
                property.getId())) {

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

        if (pendingPurchases.contains(
                player.getUniqueId())) {

            return;
        }

        EarthPropertyMenu.open(
                player,
                property
        );
    }


    /*
     * PROPERTY PURCHASE MENU
     */

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {

        Inventory topInventory =
                event.getView().getTopInventory();

        if (!EarthPropertyMenu.isPropertyMenu(
                topInventory)) {

            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked()
                instanceof Player player)) {

            return;
        }

        int rawSlot = event.getRawSlot();

        if (rawSlot < 0
                || rawSlot >= topInventory.getSize()) {

            return;
        }

        UUID playerId = player.getUniqueId();

        if (pendingPurchases.contains(playerId)) {
            return;
        }


        /*
         * CANCEL PURCHASE
         */

        if (EarthPropertyMenu.isCancelSlot(rawSlot)) {

            closeMenuSafely(player, topInventory);

            return;
        }


        /*
         * BUY HOME
         */

        if (!EarthPropertyMenu.isBuySlot(rawSlot)) {
            return;
        }

        String propertyId =
                EarthPropertyMenu.getPropertyId(
                        topInventory
                );

        if (propertyId == null) {

            closeMenuSafely(player, topInventory);

            return;
        }

        EarthProperty property =
                EarthPropertyManager.getProperty(propertyId);

        if (property == null
                || property.isOwned()) {

            player.sendMessage(
                    ChatColor.RED
                    + "This home is no longer available."
            );

            closeMenuSafely(player, topInventory);

            return;
        }


        /*
         * PREVENT REPEATED PURCHASE CLICKS
         */

        if (!pendingPurchases.add(playerId)) {
            return;
        }


        /*
         * PROCESS PURCHASE ON NEXT SERVER TICK
         */

        Bukkit.getScheduler().runTask(
                plugin,
                () -> {

                    try {

                        if (!player.isOnline()) {
                            return;
                        }

                        if (player.getOpenInventory()
                                .getTopInventory()
                                != topInventory) {

                            return;
                        }

                        player.closeInventory();

                        completePurchase(
                                player,
                                propertyId
                        );

                    } finally {

                        pendingPurchases.remove(playerId);
                    }
                }
        );
    }


    /*
     * COMPLETE PROPERTY PURCHASE
     */

    private void completePurchase(
            Player player,
            String propertyId
    ) {

        EarthProperty property =
                EarthPropertyManager.getProperty(propertyId);

        if (property == null
                || property.isOwned()) {

            player.sendMessage(
                    ChatColor.RED
                    + "This home is no longer available."
            );

            return;
        }

        org.bukkit.Location bannerLocation =
                EarthPropertyBannerRegistry.getBannerLocation(
                        propertyId
                );

        if (bannerLocation == null
                || !EarthPropertySaleMarkers.isOfficialBanner(
                        bannerLocation.getBlock(),
                        propertyId)) {

            player.sendMessage(
                    ChatColor.RED
                    + "This property is not currently for sale."
            );

            return;
        }


        /*
         * PROPERTY MANAGER HANDLES PAYMENT,
         * GOVERNMENT REVENUE AND OWNERSHIP
         */

        boolean purchased =
                EarthPropertyManager.purchaseProperty(
                        player,
                        propertyId
                );

        if (!purchased) {
            return;
        }


        /*
         * REMOVE BLUE BANNER AND PRICE SIGN
         */

        try {

            EarthPropertySaleMarkers.removeMarkers(
                    propertyId
            );

        } catch (RuntimeException exception) {

            plugin.getLogger().warning(
                    "Property purchased, but marker cleanup failed: "
                    + propertyId
                    + " - "
                    + exception.getMessage()
            );
        }

        player.sendMessage(
                ChatColor.GREEN
                + "Congratulations! Your new home "
                + "is registered in your name."
        );
    }


    /*
     * CLOSE INVENTORY SAFELY
     */

    private void closeMenuSafely(
            Player player,
            Inventory inventory
    ) {

        Bukkit.getScheduler().runTask(
                plugin,
                () -> {

                    if (player.isOnline()
                            && player.getOpenInventory()
                                    .getTopInventory()
                                    == inventory) {

                        player.closeInventory();
                    }
                }
        );
    }


    /*
     * PREVENT DRAGGING ITEMS INTO PURCHASE MENU
     */

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {

        if (EarthPropertyMenu.isPropertyMenu(
                event.getView().getTopInventory())) {

            event.setCancelled(true);
        }
    }

}
