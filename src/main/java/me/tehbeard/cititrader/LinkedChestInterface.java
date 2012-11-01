/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.tehbeard.cititrader;

import java.util.Map;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author tenowg
 */
public interface LinkedChestInterface {
    /**
     * Does the trader have this stock.
     * @param locate
     * @param checkAmount
     * @return boolean
     */
    public boolean hasStock(ItemStack locate);
    
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
     * Returns is the StockRoom is Empty.
     */
    public boolean isStockRoomEmpty();
    
    /**
     * Returns if stock if static.
     * @return boolean isStatic
     */
    public boolean isStatic();
    
    /**
     * Sets the isStatic value.
     * @param isStatic
     */
    public void setStatic(boolean isStatic);
    
    /**
     * Adds a Chest to the Linked Chest List.
     * @param loc
     * @param catagory
     * @return boolean success
     */
    public boolean setLinkedChest(Location loc, String catagory);
    
    /**
     * Adds a Chest to the Linked Chest List.
     * @param loc
     * @return boolean success
     */
    public boolean setLinkedChest(Location loc);
    
    /**
     * Removes a Chest from the Linked Chest list.
     * @param loc
     * @return 
     */
    public boolean removeLinkedChest(Location loc);
    
    /**
     * Gets the full list (Map) of linked chests.
     * @return Map<Location, String>
     */
    public Map<Location, String> getLinkedChests();
    
    /**
     * Checks if there are any Chests linked to this
     * NPC.
     * @return 
     */
    public boolean hasLinkedChest();
}
