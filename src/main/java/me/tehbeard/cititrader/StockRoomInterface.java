/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.tehbeard.cititrader;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author tenowg
 */
public interface StockRoomInterface {
    /**
     * Does the trader have this stock.
     * @param locate
     * @param checkAmount
     * @return boolean
     */
    public boolean hasStock(ItemStack locate,boolean checkAmount);
    
    /**
     * Removes an item from the Traders stock.
     * @param is
     */
    public boolean removeItem(ItemStack is);
    
    /**
     * Adds an item to the Traders stock.
     * @param is
     */
    public boolean addItem(ItemStack is);
    
    /**
     * Opens the stock room inventory to 
     * the supplied player.
     */
    public void openStockRoom(Player player);
    
    /**
     * Returns is the StockRoom is Empty.
     */
    public boolean isStockRoomEmpty();
    
    /**
     * Checks if there is space in the Inventory or this item.
     * @param is
     * @return 
     */
    public boolean hasSpace(ItemStack is);
}
