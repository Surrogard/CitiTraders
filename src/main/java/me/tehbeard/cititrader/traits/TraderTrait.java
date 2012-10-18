package me.tehbeard.cititrader.traits;

import net.citizensnpcs.api.trait.Trait;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 *
 * @author tenowg
 */
public class TraderTrait extends Trait implements InventoryHolder {
    
    Inventory minv;
    
    public TraderTrait() {
        super("villagetrader");
    }
    
    public void setupTrader() {
        
    }
    
    public void openInventory(Player player) {
    }

    public Inventory getInventory() {
        return minv;
    }
}
