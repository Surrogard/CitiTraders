package me.tehbeard.cititrader.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public class TraderUtils {

    private TraderUtils() {
    }

    public static boolean isTopInventory(InventoryClickEvent event) {
        return (event.getRawSlot() < event.getView().getTopInventory().getSize() && event.getRawSlot() != InventoryView.OUTSIDE);
    }

    public static boolean isBottomInventory(InventoryClickEvent event) {

        return (event.getRawSlot() >= event.getView().getTopInventory().getSize()
                && event.getRawSlot() < (event.getView().getTopInventory().getSize() + event.getView().getBottomInventory().getSize())
                && event.getRawSlot() != InventoryView.OUTSIDE);
    }

    /**
     * Checks if inventory has enough room in inventory for ItemStack
     *
     * @param inventory
     * @param is
     * @return
     */
    public static boolean hasInventorySpace(Inventory inventory, ItemStack is) {
        //Inventory chkr = Bukkit.createInventory(null, 9 * 4);
        Inventory chkr = Bukkit.createInventory(null, inventory.getSize());

        for (ItemStack item : inventory.getContents()) {
            try {
                if (item != null) {
                    ItemStack newItem = item.clone();
                    chkr.addItem(newItem);
                }
            } catch (Exception e) {
            }
        }
        //chkr.setContents(playerInv.getContents());
        if (chkr.addItem(is.clone()).size() > 0) {
            return false;
        }
        
        return true;
    }
}
