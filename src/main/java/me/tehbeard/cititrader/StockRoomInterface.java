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
     * @return
     */
    public boolean hasStock(ItemStack locate,boolean checkAmount);
    
    /**
     * Opens the stock room inventory to 
     * the supplied player.
     */
    public void openStockRoom(Player player);
    
    /**
     * Returns is the StockRoom is Empty.
     */
    public boolean isStockRoomEmpty();
}
