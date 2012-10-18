package me.tehbeard.cititrader;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Represents an interface for interacting with the trader aspect of an NPC
 * @author James
 *
 */
public interface TraderInterface {

    /**
     * Get the price this trader will sell an item for
     * @param is ItemStack to check
     * @return
     */
    public double getSellPrice(ItemStack is);

    /**
     * set the selling price for an item
     * @param is ItemStack to set price for
     * @param price
     */
    public void setSellPrice(ItemStack is,double price);
    
    /**
     * Get the price an npc will buy for.
     * @param is
     * @return
     */
    public double getBuyPrice(ItemStack is);

    /**
     * Set the price an npc will buy for.
     * @param is
     * @param price
     */
    public void setBuyPrice(ItemStack is,double price);
    
    /**
     * Opens window and sets up player to purchase from this NPC
     * @param player
     */
    public void openSalesWindow(Player player);
    
    /**
     * Opens window and sets up player to sell to this NPC
     * @param player
     */
    public void openBuyWindow(Player player);

    /**
     * Process an inventory click
     * @param event
     */
    public void processInventoryClick(InventoryClickEvent event);
    
    /**
     * inventory closing logic
     * @param event
     */
    public void processInventoryClose(InventoryCloseEvent event);
    
    /**
     * Does the trader have this stock.
     * @param locate
     * @param checkAmount
     * @return
     */
    public boolean hasStock(ItemStack locate,boolean checkAmount);
}