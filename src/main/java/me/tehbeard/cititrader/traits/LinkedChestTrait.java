/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.tehbeard.cititrader.traits;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import me.tehbeard.cititrader.CitiTrader;
import me.tehbeard.cititrader.LinkedChestInterface;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author tenowg
 */
public class LinkedChestTrait extends Trait implements LinkedChestInterface {

    private Map<Location, String> linkedChests;

    public LinkedChestTrait() {
        super("linkedchest");
        linkedChests = new HashMap<Location, String>();
    }

    @Override
    public void load(DataKey data) {
        //load the linked Chests
        for (DataKey chestKey : data.getRelative("chests").getIntegerSubKeys()) {
            int x = chestKey.getRelative("location").getInt("X");
            int y = chestKey.getRelative("location").getInt("Y");
            int z = chestKey.getRelative("location").getInt("Z");
            World world = CitiTrader.self.getServer().getWorld(chestKey.getRelative("location").getString("world"));
            String catagory = chestKey.getString("catagory");

            Location loc = new Location(world, x, y, z);

            linkedChests.put(loc, catagory);
        }
    }

    @Override
    public void save(DataKey data) {
        int i;

        //Save Linked chests
        data.removeKey("chests");
        DataKey chestsKey = data.getRelative("chests");
        i = 0;
        for (Map.Entry<Location, String> chest : linkedChests.entrySet()) {
            Material type = chest.getKey().getBlock().getType();
            if (type.equals(Material.CHEST) || type.equals(Material.ENDER_CHEST)) {
                DataKey chestdata = chestsKey.getRelative("" + i).getRelative("location");
                chestdata.setInt("X", chest.getKey().getBlockX());
                chestdata.setInt("Y", chest.getKey().getBlockY());
                chestdata.setInt("Z", chest.getKey().getBlockZ());
                chestdata.setString("world", chest.getKey().getWorld().getName());
                chestsKey.getRelative("" + i).setString("catagory", chest.getValue());
                i++;
            }
        }

    }

    public boolean hasStock(ItemStack locate) {
        ItemStack is = locate.clone();
        Material material = locate.getType();
        int amount = locate.getAmount();
        boolean checkAmount = true;

        int amountFound = 0;

        for (Map.Entry<Location, String> loc : linkedChests.entrySet()) {
            if (loc.getKey().getBlock().getType().equals(Material.CHEST)) {
                if (loc.getKey().distance(npc.getBukkitEntity().getLocation()) < 10) {
                    for (Map.Entry<Integer, ? extends ItemStack> e : ((Chest) loc.getKey().getBlock().getState()).getBlockInventory().all(material).entrySet()) {
                        //for (ItemStack isfor : ((Chest) loc.getKey().getBlock().getState()).getBlockInventory()) {
                        is.setAmount(e.getValue().getAmount());
                        if (e.getValue().equals(is)) {
                            amountFound += e.getValue().getAmount();
                        }
                    }
                } else {
                    // warn own that chest is to far away.
                }
            } else {
                // warn if owner is online, chest doesn't exist.
                System.out.println("Chest doesn't exist: " + loc.getKey().getBlock().getType().toString());
            }
        }
        return checkAmount ? amount <= amountFound : amountFound > 0;
    }

    public boolean removeItem(ItemStack removeitem) {

        for (Map.Entry<Location, String> loc : linkedChests.entrySet()) {
            if (loc.getKey().getBlock().getType().equals(Material.CHEST)) {
                if (loc.getKey().distance(npc.getBukkitEntity().getLocation()) < 10) {
                    ((Chest) loc.getKey().getBlock().getState()).getBlockInventory().remove(removeitem);
                } else {
                    // warn own that chest is to far away.
                }
            } else {
                // warn if owner is online, chest doesn't exist.
                System.out.println("Chest doesn't exist: " + loc.getKey().getBlock().getType().toString());
            }
        }

        if (removeitem.getAmount() > 0) {
            return false;
        }
        return true;
    }

    public boolean addItem(ItemStack iss) {
        for (Map.Entry<Location, String> loc : linkedChests.entrySet()) {
            if (loc.getKey().getBlock().getType().equals(Material.CHEST)) {
                if (loc.getKey().distance(npc.getBukkitEntity().getLocation()) < 10) {
                    ((Chest) loc.getKey().getBlock().getState()).getBlockInventory().addItem(iss);
                }
            }
        }

        return true;
    }

    public boolean setLinkedChest(Location loc, String catagory) {
        if (loc.getBlock().getType().equals(Material.CHEST)) {
            linkedChests.put(loc, catagory);
            return true;
        }

        return false;
    }

    public boolean setLinkedChest(Location loc) {
        return setLinkedChest(loc, "default");
    }

    public boolean removeLinkedChest(Location loc) {
        if (linkedChests.containsKey(loc)) {
            linkedChests.remove(loc);
            return true;
        }

        return false;
    }

    public Map<Location, String> getLinkedChests() {
        return linkedChests;
    }

    public boolean hasLinkedChest() {
        if (linkedChests.size() > 0) {
            return true;
        }
        return false;
    }

    public boolean hasSpace(ItemStack iss) {
        ItemStack is = iss.clone();

        for (Map.Entry<Location, String> loc : linkedChests.entrySet()) {
            if (loc.getKey().getBlock().getType().equals(Material.CHEST)) {
                if (loc.getKey().distance(npc.getBukkitEntity().getLocation()) < 10) {
                    Inventory chchest = Bukkit.createInventory(null, ((Chest) loc.getKey().getBlock().getState()).getBlockInventory().getSize());
                    /*for (ItemStack item : ((Chest) loc.getKey().getBlock().getState()).getBlockInventory()) {
                        chchest.addItem(item);
                    }*/
                    for (int i = 0; i < chchest.getSize(); i++) {
                        if (((Chest) loc.getKey().getBlock().getState()).getBlockInventory().getItem(i) != null) {
                            ItemStack item = ((Chest) loc.getKey().getBlock().getState()).getBlockInventory().getItem(i).clone();
                            chchest.setItem(i, item);
                        }
                    }
                    HashMap<Integer, ItemStack> returnItems = chchest.addItem(is);
                    if (!returnItems.isEmpty()) {
                        is = returnItems.get(0);
                        /*for (Entry<Integer, ItemStack> item : returnItems.entrySet()) {
                            System.out.println("Integer: " + item.getKey() + " ItemStack: " + item.getValue().toString());
                        }*/
                    } else {
                        is.setAmount(0);
                        break;
                    }
                }
            }
        }

        if (is.getAmount() > 0) {
            return false;
        }

        return true;
    }
}
