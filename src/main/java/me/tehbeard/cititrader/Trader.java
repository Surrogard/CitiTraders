package me.tehbeard.cititrader;

import me.tehbeard.cititrader.traits.WalletTrait;
import me.tehbeard.cititrader.traits.StockRoomTrait;
import me.tehbeard.cititrader.traits.ShopTrait;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import me.tehbeard.cititrader.TraderStatus.Status;
import me.tehbeard.cititrader.traits.WalletTrait.WalletType;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.CitizensEnableEvent;
import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Owner;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.mcstats.Metrics;
import org.mcstats.Metrics.Graph;

/**
 * @author James
 *
 */
public class Trader implements Listener {

    private static Map<String, TraderStatus> status;

    public static TraderStatus getStatus(String player) {
        if (!status.containsKey(player)) {
            status.put(player, new TraderStatus());
        }
        return status.get(player);

    }

    public static void clearStatus(String player) {
        status.remove(player);

    }

    public Trader() {
        status = new HashMap<String, TraderStatus>();
    }

    @EventHandler
    public void onCitizensLoad(CitizensEnableEvent event) {
        try {
            Metrics metrics = new Metrics(CitiTrader.self);
            Graph graph = metrics.createGraph("Traders");
            graph.addPlotter(new Metrics.Plotter("Total Traders") {
                @Override
                public int getValue() {

                    Integer totaltrader = 0;
                    try {
                        Iterator it = CitizensAPI.getNPCRegistry().iterator();
                        while (it.hasNext()) {
                            NPC npcount = (NPC) it.next();
                            if (npcount.hasTrait(ShopTrait.class)) {
                                totaltrader++;
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("error");
                        e.printStackTrace();
                    }
                    if (CitiTrader.self.getConfig().getBoolean("debug.tradercount", false)) {
                        CitiTrader.self.getLogger().log(Level.INFO, "Traders: {0}", totaltrader);
                    }
                    return totaltrader;
                }
            });
            metrics.start();
            CitiTrader.self.getLogger().info("Metrics Started.");
        } catch (IOException e) {
            CitiTrader.self.getLogger().info("Failed:");
            e.printStackTrace();
        }

        if (CitiTrader.self.getConfig().getBoolean("debug.versioncheck", true)) {
            CitiTrader.self.checkVersion();
        }
    }

    @EventHandler
    public void onLeftClick(EntityDamageEvent event) {
        if (!(event instanceof EntityDamageByEntityEvent)) {
            return;
        }
        
        if (!CitizensAPI.getNPCRegistry().isNPC(event.getEntity())) {
            //Bukkit.broadcastMessage("Failed Spot Two");
            return;
        }

        EntityDamageByEntityEvent devent = (EntityDamageByEntityEvent)event;
        if (!(devent.getDamager() instanceof Player)) {
            //Bukkit.broadcastMessage("Failed Spot Three");
            return;
        }
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getEntity());
        Player player = (Player) devent.getDamager();
        
        if (!npc.hasTrait(ShopTrait.class)) {
            //Bukkit.broadcastMessage("Failed Spot Four");
            return;
        }
        if (!npc.getTrait(ShopTrait.class).getDisabled()) {
            npc.getTrait(ShopTrait.class).openBuyWindow(player);
        } else {
            player.sendMessage(ChatColor.DARK_PURPLE + "This trader is currently disabled.");
        }
    }
    
    /*@EventHandler
    public void onLeftClick(NPCLeftClickEvent event) {
        NPC npc = event.getNPC();
        Player by = event.getClicker();

        if (!npc.hasTrait(StockRoomTrait.class)) {
            return;
        }
        if (!npc.getTrait(StockRoomTrait.class).getDisabled()) {
            npc.getTrait(StockRoomTrait.class).openBuyWindow(by);
        } else {
            by.sendMessage(ChatColor.DARK_PURPLE + "This trader is currently disabled.");
        }

    }*/

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        if (!event.getClickedBlock().getType().equals(Material.CHEST)) {
            return;
        }

        Player by = event.getPlayer();

        TraderStatus state = getStatus(by.getName());

        if (state.getStatus().equals(Status.SELECT_LINK_CHEST)) {
            event.setCancelled(true);
            state.setChestLocation(event.getClickedBlock().getLocation());
            state.setStatus(Status.SELECT_CHEST_NPC);
            by.sendMessage(ChatColor.DARK_PURPLE + "Right click the NPC you want to link.");
        }

        if (state.getStatus().equals(Status.SELECT_UNLINK_CHEST)) {
            event.setCancelled(true);
            state.setChestLocation(event.getClickedBlock().getLocation());
            state.setStatus(Status.SELECT_UNCHEST_NPC);
            by.sendMessage(ChatColor.DARK_PURPLE + "Right click the NPC you want to unlink.");
        }
    }

    @EventHandler
    public void onRightClick(NPCRightClickEvent event) {
        NPC npc = event.getNPC();
        Player by = event.getClicker();
        if (!npc.hasTrait(ShopTrait.class)) {
            return;
        }

        if (!npc.getTrait(ShopTrait.class).isEnableRightClick()) {
            return;
        }
        TraderStatus state = getStatus(by.getName());
        state.setTrader(npc);
        String owner = npc.getTrait(Owner.class).getOwner();

        if (by.getName().equalsIgnoreCase(owner) || by.getName().equalsIgnoreCase(npc.getName())) {
            switch (state.getStatus()) {
                case DISABLE:
                    state.getTrader().getTrait(ShopTrait.class).setDisabled(true);
                    clearStatus(by.getName());
                    by.sendMessage(ChatColor.DARK_PURPLE + "Trader " + npc.getName() + " has been disabled.");
                    return;
                case ENABLE:
                    state.getTrader().getTrait(ShopTrait.class).setDisabled(false);
                    clearStatus(by.getName());
                    by.sendMessage(ChatColor.DARK_PURPLE + "Trader " + npc.getName() + " has been enabled.");
                    return;
            }
        }
        if (by.getName().equalsIgnoreCase(owner)) {

            switch (state.getStatus()) {
                case FIRING: {
                    if (!state.getTrader().getTrait(StockRoomTrait.class).isStockRoomEmpty()) {
                        by.sendMessage(ChatColor.RED + "Cannot fire trader! He still has items on him!");
                        clearStatus(by.getName());
                        return;
                    }
                    if (state.getTrader().getTrait(WalletTrait.class).getType() == WalletType.PRIVATE && state.getTrader().getTrait(WalletTrait.class).getAmount() > 0.0D) {
                        by.sendMessage(ChatColor.RED + "Trader still has money in their wallet!");
                        clearStatus(by.getName());
                        return;
                    }

                    by.sendMessage(ChatColor.DARK_RED + "Firing trader!");
                    npc.removeTrait(ShopTrait.class);
                    npc.removeTrait(WalletTrait.class);
                    npc.destroy();
                }
                case SET_PRICE_SELL: {
                    state.getTrader().getTrait(ShopTrait.class).setSellPrice(by.getItemInHand(), state.getMoney());
                    state.setStatus(Status.NOT);
                    if (state.getMoney() == -1) {
                        by.sendMessage(ChatColor.GREEN + "Item price removed.");
                    } else {
                        by.sendMessage(ChatColor.GREEN + "Sell price set.");
                    }
                    return;
                }

                case SET_PRICE_BUY: {
                    state.getTrader().getTrait(ShopTrait.class).setBuyPrice(by.getItemInHand(), state.getMoney());
                    state.setStatus(Status.NOT);
                    if (state.getMoney() == -1) {
                        by.sendMessage(ChatColor.GREEN + "Item price removed.");
                    } else {
                        by.sendMessage(ChatColor.GREEN + "Buy price set.");
                    }
                    return;
                }

                case SET_WALLET: {
                    state.getTrader().getTrait(WalletTrait.class).setAccount(state.getAccName());
                    state.getTrader().getTrait(WalletTrait.class).setType(state.getWalletType());
                    state.setStatus(Status.NOT);
                    by.sendMessage(ChatColor.GREEN + "Wallet information set");
                    return;
                }

                case GIVE_MONEY: {
                    if (state.getTrader().getTrait(WalletTrait.class).getType() != WalletType.PRIVATE) {
                        by.sendMessage(ChatColor.RED + "Cannot use give/take on traders who use economy backed accounts.");
                        return;
                    }
                    if (!CitiTrader.economy.has(by.getName(), state.getMoney())) {
                        by.sendMessage(ChatColor.RED + "Not enough funds.");
                    }
                    if (!state.getTrader().getTrait(WalletTrait.class).deposit(state.getMoney())) {
                        by.sendMessage(ChatColor.RED + "Cannot  give trader the money.");
                        return;
                    }
                    if (!CitiTrader.economy.withdrawPlayer(by.getName(), state.getMoney()).transactionSuccess()) {
                        by.sendMessage(ChatColor.RED + "Cannot give trader the money from your wallet.");
                        state.getTrader().getTrait(WalletTrait.class).withdraw(state.getMoney());
                        return;
                    }
                    by.sendMessage(ChatColor.GREEN + "Money given");
                    status.remove(by.getName());
                    return;
                }
                case TAKE_MONEY: {

                    if (state.getTrader().getTrait(WalletTrait.class).getType() != WalletType.PRIVATE) {
                        by.sendMessage(ChatColor.RED + "Cannot use give/take on traders who use economy backed accounts.");
                        return;
                    }
                    WalletTrait wallet = state.getTrader().getTrait(WalletTrait.class);

                    if (!wallet.has(state.getMoney())) {
                        by.sendMessage(ChatColor.RED + "Not enough funds.");
                        return;
                    }
                    if (!CitiTrader.economy.depositPlayer(by.getName(), state.getMoney()).transactionSuccess()) {
                        by.sendMessage(ChatColor.RED + "Cannot take the money.");
                        return;
                    }
                    if (!wallet.withdraw(state.getMoney())) {
                        by.sendMessage(ChatColor.RED + "Cannot take the money from the trader's wallet.");
                        CitiTrader.economy.withdrawPlayer(by.getName(), state.getMoney());
                        return;
                    }
                    by.sendMessage(ChatColor.GREEN + "Money given");
                    status.remove(by.getName());
                    return;
                }
                case BALANCE_MONEY: {
                    WalletTrait wallet = state.getTrader().getTrait(WalletTrait.class);
                    
                    by.sendMessage(ChatColor.GOLD + "Traders balance is " + wallet.getAmount());
                    status.remove(by.getName());
                    return;
                }
                    
                case SET_LINK: {
                    if (!state.getTrader().getTrait(ShopTrait.class).setLinkedNPC(state.getLinkedNPCName())) {
                        by.sendMessage(ChatColor.RED + "Trader could not be linked to " + state.getLinkedNPCName());
                        state.setStatus(Status.NOT);
                        return;
                    }

                    by.sendMessage(ChatColor.GREEN + "Trader linked to " + state.getLinkedNPCName());
                    state.setStatus(Status.NOT);
                    return;
                }
                case REMOVE_LINK: {
                    if (!state.getTrader().getTrait(ShopTrait.class).removeLinkedNPC()) {
                        by.sendMessage(ChatColor.RED + "Trader could not be unlinked.");
                        state.setStatus(Status.NOT);
                        return;
                    }
                    by.sendMessage(ChatColor.GREEN + "Trader has been unlinked, he will use is own pricelist now.");
                    state.setStatus(Status.NOT);
                    return;
                }
                case SELECT_CHEST_NPC: {
                    NPC chestOwner = CitiTrader.self.isChestLinked(state.getChestLocation());
                    if (chestOwner != null) {
                        if(!chestOwner.getTrait(Owner.class).isOwnedBy(by)) {
                            by.sendMessage(ChatColor.RED + "Chest is already linked to a different Owner.");
                            state.setStatus(Status.NOT);
                            return;
                        }
                    }
                    if (!state.getTrader().getTrait(ShopTrait.class).setLinkedChest(state.getChestLocation())) {
                        by.sendMessage(ChatColor.RED + "Chest could not be linked to Trader.");
                        state.setStatus(Status.NOT);
                        return;
                    }

                    int chestLimit = CitiTrader.self.getChestLimit(by);
                    if (chestLimit != -1 && chestLimit <= state.getTrader().getTrait(ShopTrait.class).getLinkedChests().size()-1) {
                        by.sendMessage(ChatColor.RED + "You cannot Link another Chest to this NPC. (" + chestLimit + ")" + state.getTrader().getTrait(ShopTrait.class).getLinkedChests().size());
                        state.setStatus(Status.NOT);
                        return;
                    }

                    by.sendMessage(ChatColor.GREEN + "Chest has been linked to " + npc.getName());
                    state.setStatus(Status.NOT);
                    return;
                }
                case SELECT_UNCHEST_NPC: {
                    NPC chestOwner = CitiTrader.self.isChestLinked(state.getChestLocation());
                    if (chestOwner != null) {
                        if(!chestOwner.getTrait(Owner.class).isOwnedBy(by)) {
                            by.sendMessage(ChatColor.RED + "You don't own the Linked NPC, you can't unlink this chest.");
                            state.setStatus(Status.NOT);
                            return;
                        }
                    }
                    
                    if (!state.getTrader().getTrait(ShopTrait.class).removeLinkedChest(state.getChestLocation())) {
                        by.sendMessage(ChatColor.RED + "Chest could not be unlinked from Trader.");
                        state.setStatus(Status.NOT);
                        return;
                    }

                    by.sendMessage(ChatColor.GREEN + "Chest has been unlinked from " + npc.getName());
                    state.setStatus(Status.NOT);
                    return;
                }
                case SET_SELL_STACK: {
                    if(!state.getTrader().getTrait(ShopTrait.class).setSellStack(by.getItemInHand(), state.getStackAmount())) {
                        by.sendMessage("Something went wrong.");
                        return;
                    }
                    by.sendMessage(ChatColor.GREEN + "Stack amount set.");
                    status.remove(by.getName());
                    return;
                }
                    
                case LIST_SELL_PRICE: {
                    Map<ItemStack, Double> price = state.getTrader().getTrait(ShopTrait.class).getSellPrices();
                    by.sendMessage(ChatColor.GOLD + "---{Sell Prices}---");
                    for (Entry<ItemStack, Double> item : price.entrySet()) {
                        by.sendMessage(ChatColor.YELLOW + item.getKey().getType().name() + "   " + ChatColor.GREEN + item.getValue());
                    }
                    status.remove(by.getName());
                    return;
                }
                case LIST_BUY_PRICE: {
                    Map<ItemStack, Double> price = state.getTrader().getTrait(ShopTrait.class).getBuyPrices();
                    by.sendMessage(ChatColor.GOLD + "---{Buy Prices}---");
                    for (Entry<ItemStack, Double> item : price.entrySet()) {
                        by.sendMessage(ChatColor.YELLOW + item.getKey().getType().name() + "   " + ChatColor.GREEN + item.getValue());
                    }
                    status.remove(by.getName());
                    return;
                }
            }

        }


        if (by.getName().equalsIgnoreCase(owner) && by.getItemInHand().getType() == Material.BOOK) {
            if (!npc.getTrait(ShopTrait.class).hasLinkedChest()) {
                npc.getTrait(StockRoomTrait.class).openStockRoom(by);
            } else {
                by.sendMessage(ChatColor.RED + "Trader has linked chests, use them to stock trader.");
            }
        } else {
            if (!npc.getTrait(ShopTrait.class).getDisabled()) {
                npc.getTrait(ShopTrait.class).openSalesWindow(by);
            } else {
                by.sendMessage(ChatColor.DARK_PURPLE + "This shop is currently disabled.");
            }
        }
    }

    public static void setUpNPC(NPC npc) {
        if (!npc.hasTrait(ShopTrait.class)) {
            npc.addTrait(ShopTrait.class);

        }

        if (!npc.hasTrait(WalletTrait.class)) {
            npc.addTrait(WalletTrait.class);

        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void inventoryClick(InventoryClickEvent event) {
        TraderStatus state = getStatus(event.getWhoClicked().getName());

        if (state.getStatus() != Status.NOT) {
            state.getTrader().getTrait(ShopTrait.class).processInventoryClick(event);
        }
    }

    /**
     *
     * if they close the inventory cancel the trading
     */
    @EventHandler
    public void inventoryClose(InventoryCloseEvent event) {
        TraderStatus state = getStatus(event.getPlayer().getName());
        if (state.getStatus() != Status.NOT) {
            state.getTrader().getTrait(ShopTrait.class).processInventoryClose(event);
        }
    }

    @EventHandler
    public void onPlayerLogin(PlayerJoinEvent event) {
        if (event.getPlayer().isOp() && CitiTrader.outdated) {
            event.getPlayer().sendMessage(ChatColor.GOLD + "Your version of Cititraders(" + CitiTrader.self.getDescription().getVersion() + ") is outdated, please update.");
        }
    }
}
