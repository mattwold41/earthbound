package com.earthbound;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class EarthPropertyMenu {

    private static final int MENU_SIZE = 27;

    private static final int INFO_SLOT = 11;
    private static final int BUY_SLOT = 13;
    private static final int CANCEL_SLOT = 15;

    /*
     * CUSTOM INVENTORY HOLDER
     *
     * This identifies EarthBound property menus
     * without relying on inventory title text.
     */

    public static class PropertyMenuHolder
            implements InventoryHolder {

        private final String propertyId;
        private Inventory inventory;

        public PropertyMenuHolder(String propertyId) {
            this.propertyId = propertyId;
        }

        public String getPropertyId() {
            return propertyId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }
    }

    /*
     * OPEN HOME PURCHASE MENU
     */

    public static void open(
            Player player,
            EarthProperty property
    ) {

        if (player == null || property == null) {
            return;
        }

        if (property.isOwned()) {

            player.sendMessage(
                    ChatColor.RED
                    + "This home is no longer for sale."
            );

            return;
        }

        PropertyMenuHolder holder =
                new PropertyMenuHolder(
                        property.getId()
                );

        Inventory inventory =
                Bukkit.createInventory(
                        holder,
                        MENU_SIZE,
                        ChatColor.DARK_BLUE
                        + "EarthBound Real Estate"
                );

        holder.setInventory(inventory);

        /*
         * DECORATIVE BACKGROUND
         */

        ItemStack background =
                createItem(
                        Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                        " ",
                        new ArrayList<>()
                );

        for (int slot = 0; slot < MENU_SIZE; slot++) {
            inventory.setItem(slot, background);
        }

        /*
         * PROPERTY INFORMATION
         */

        List<String> infoLore =
                new ArrayList<>();

        infoLore.add(
                ChatColor.GRAY
                + "Location: Guemes Island"
        );

        infoLore.add(
                ChatColor.GRAY
                + "Plot Size: "
                + property.getWidth()
                + " x "
                + property.getLength()
        );

        infoLore.add(
                ChatColor.GRAY
                + "Property ID: "
                + property.getId()
        );

        infoLore.add("");

        infoLore.add(
                ChatColor.GOLD
                + "Price: "
                + formatMoney(property.getPrice())
        );

        infoLore.add(
                ChatColor.GREEN
                + "Status: For Sale"
        );

        inventory.setItem(
                INFO_SLOT,
                createItem(
                        Material.OAK_DOOR,
                        ChatColor.AQUA
                        + property.getName(),
                        infoLore
                )
        );

        /*
         * BUY HOME BUTTON
         */

        List<String> buyLore =
                new ArrayList<>();

        buyLore.add(
                ChatColor.GRAY
                + "Purchase this home for:"
        );

        buyLore.add(
                ChatColor.GOLD
                + formatMoney(property.getPrice())
        );

        buyLore.add("");

        buyLore.add(
                ChatColor.YELLOW
                + "Click to purchase"
        );

        inventory.setItem(
                BUY_SLOT,
                createItem(
                        Material.EMERALD_BLOCK,
                        ChatColor.GREEN
                        + "BUY HOME",
                        buyLore
                )
        );

        /*
         * CANCEL BUTTON
         */

        List<String> cancelLore =
                new ArrayList<>();

        cancelLore.add(
                ChatColor.GRAY
                + "Close this window"
        );

        inventory.setItem(
                CANCEL_SLOT,
                createItem(
                        Material.BARRIER,
                        ChatColor.RED
                        + "CANCEL",
                        cancelLore
                )
        );

        player.openInventory(inventory);
    }

    /*
     * CREATE MENU ITEM
     */

    private static ItemStack createItem(
            Material material,
            String name,
            List<String> lore
    ) {

        ItemStack item =
                new ItemStack(material);

        ItemMeta meta =
                item.getItemMeta();

        if (meta != null) {

            meta.setDisplayName(name);
            meta.setLore(lore);

            item.setItemMeta(meta);
        }

        return item;
    }

    /*
     * IDENTIFY PROPERTY MENU
     */

    public static boolean isPropertyMenu(
            Inventory inventory
    ) {

        return inventory != null
                && inventory.getHolder()
                instanceof PropertyMenuHolder;
    }

    /*
     * GET PROPERTY ID FROM MENU
     */

    public static String getPropertyId(
            Inventory inventory
    ) {

        if (!isPropertyMenu(inventory)) {
            return null;
        }

        PropertyMenuHolder holder =
                (PropertyMenuHolder)
                inventory.getHolder();

        return holder.getPropertyId();
    }

    /*
     * MENU BUTTON POSITIONS
     */

    public static boolean isBuySlot(int slot) {
        return slot == BUY_SLOT;
    }

    public static boolean isCancelSlot(int slot) {
        return slot == CANCEL_SLOT;
    }

    /*
     * FORMAT MONEY
     */

    private static String formatMoney(
            double amount
    ) {

        return String.format(
                Locale.US,
                "$%,.2f",
                amount
        );
    }
}
