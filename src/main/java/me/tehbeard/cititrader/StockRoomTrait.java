package me.tehbeard.cititrader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import me.tehbeard.cititrader.TraderStatus.Status;
import me.tehbeard.cititrader.WalletTrait.WalletType;
import me.tehbeard.cititrader.utils.TraderUtils;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.exception.NPCLoadException;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.api.util.ItemStorage;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class StockRoomTrait extends Trait implements InventoryHolder, TraderInterface {

    private Inventory stock;
    Map<ItemStack, Double> sellPrices;
    Map<ItemStack, Double> buyPrices;
    Map<Location, String> linkedChests;
    Map<ItemStack, Integer> stackSizes;
    Map<ItemStack, Integer> buyStackSizes;
    boolean enableLeftClick;
    boolean enableRightClick;
    boolean disabled;
    int linkedNPCID;

    public StockRoomTrait() {
        this(54);
    }

    private StockRoomTrait(int size) {
        super("stockroom");
        if (size <= 0 || size > 54) {
            throw new IllegalArgumentException("Size must be between 1 and 54");
        }

        stock = Bukkit.createInventory(this, size, "Stockroom");
        sellPrices = new HashMap<ItemStack, Double>();
        buyPrices = new HashMap<ItemStack, Double>();
        linkedChests = new HashMap<Location, String>();
        stackSizes = new HashMap<ItemStack, Integer>();
        buyStackSizes = new HashMap<ItemStack, Integer>();
        enableLeftClick = true;
        enableRightClick = true;
        disabled = false;
        linkedNPCID = -1;
    }

    @Override
    public void load(DataKey data) throws NPCLoadException {
        enableLeftClick = data.getBoolean("enableLeftClick");
        enableRightClick = data.getBoolean("enableRightClick");

        //Load the inventory
        for (DataKey slotKey : data.getRelative("inv").getIntegerSubKeys()) {
            stock.setItem(
                    Integer.parseInt(slotKey.name()), ItemStorage.loadItemStack(slotKey));
        }


        //load selling prices
        for (DataKey priceKey : data.getRelative("prices").getIntegerSubKeys()) {
            //System.out.println("price listing found");
            ItemStack k = ItemStorage.loadItemStack(priceKey.getRelative("item"));
            //System.out.println(k);
            double price = priceKey.getDouble("price");
            int stacksize = priceKey.getInt("stack", 1);
            //System.out.println(price);
            sellPrices.put(k, price);
            stackSizes.put(k, stacksize);
        }

        //load buy prices
        for (DataKey priceKey : data.getRelative("buyprices").getIntegerSubKeys()) {
            //System.out.println("price listing found");
            //String test = priceKey.get("item.id");
            ItemStack k = ItemStorage.loadItemStack(priceKey.getRelative("item"));
            
            // Assume once that if there is an item, that it is real.
            if(k == null) {
                int test = priceKey.getInt("item.id");
                Material mat = Material.getMaterial(test);
                priceKey.setString("item.id", mat.name());
                k = ItemStorage.loadItemStack((priceKey.getRelative("item")));
            }
            //System.out.println(k);
            double price = priceKey.getDouble("price");
            int stacksize = priceKey.getInt("stack", 1);
            //System.out.println(price);
            buyStackSizes.put(k, stacksize);
            buyPrices.put(k, price);
        }

        //load if disabled or enabled
        disabled = data.getBoolean("disabled");

        //load if Trader is linked to another NPC
        linkedNPCID = data.getInt("linkedNPCID", -1);

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

        data.setBoolean("enableRightClick", enableRightClick);
        data.setBoolean("enableLeftClick", enableLeftClick);
        data.setBoolean("disabled", disabled);
        data.setInt("linkedNPCID", linkedNPCID);

        //save the inventory
        int i = 0;
        for (ItemStack is : stock.getContents()) {
            if (is != null) {

                DataKey inv = data.getRelative("inv");
                ItemStorage.saveItem(inv.getRelative("" + i++), is);
            }
        }

        data.removeKey("prices");
        DataKey sellPriceIndex = data.getRelative("prices");

        i = 0;
        for (Entry<ItemStack, Double> price : sellPrices.entrySet()) {
            if (price.getValue() > 0.0D) {
                ItemStorage.saveItem(sellPriceIndex.getRelative("" + i).getRelative("item"), price.getKey());
                if (stackSizes.containsKey(price.getKey())) {
                    sellPriceIndex.getRelative("" + i).setInt("stack", stackSizes.get(price.getKey()));
                } else {
                    sellPriceIndex.getRelative("" + i).setInt("stack", 1);
                }
                sellPriceIndex.getRelative("" + i++).setDouble("price", price.getValue());
            }
        }


        data.removeKey("buyprices");
        DataKey buyPriceIndex = data.getRelative("buyprices");
        i = 0;
        for (Entry<ItemStack, Double> price : buyPrices.entrySet()) {
            if (price.getValue() > 0.0D) {
                ItemStorage.saveItem(buyPriceIndex.getRelative("" + i).getRelative("item"), price.getKey());
                if (buyStackSizes.containsKey(price.getKey())) {
                    sellPriceIndex.getRelative("" + i).setInt("stack", buyStackSizes.get(price.getKey()));
                } else {
                    sellPriceIndex.getRelative("" + i).setInt("stack", 1);
                }
                buyPriceIndex.getRelative("" + i++).setDouble("price", price.getValue());
            }
        }

        //Save Linked chests
        data.removeKey("chests");
        DataKey chestsKey = data.getRelative("chests");
        i = 0;
        for (Entry<Location, String> chest : linkedChests.entrySet()) {
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

    public Inventory getInventory() {
        return stock;
    }

    /**
     * Construct a viewing inventory
     *
     * @return
     */
    private Inventory constructViewing() {


        Inventory display = Bukkit.createInventory(null, 54, "Left Click Buy-Right Click Price");

        buildSalesWindow(display);

        return display;
    }

    private Inventory constructSellBox() {

        Inventory display = Bukkit.createInventory(null, 36, "Selling");
        return display;


    }

    /**
     * Does this stockroom contain this item
     *
     * @param locate Item to look for
     * @param checkAmount
     * @return
     */
    public boolean hasStock(ItemStack locate, boolean checkAmount) {

        ItemStack is = locate.clone();
        Material material = locate.getType();
        int amount = locate.getAmount();

        int amountFound = 0;

        if (hasLinkedChest()) {
            for (Entry<Location, String> loc : linkedChests.entrySet()) {
                if (loc.getKey().getBlock().getType().equals(Material.CHEST)) {
                    if (loc.getKey().distance(npc.getBukkitEntity().getLocation()) < 10) {
                        for (Entry<Integer, ? extends ItemStack> e : ((Chest) loc.getKey().getBlock().getState()).getBlockInventory().all(material).entrySet()) {
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
        } else {
            for (Entry<Integer, ? extends ItemStack> e : stock.all(material).entrySet()) {
                is.setAmount(e.getValue().getAmount());
                if (e.getValue().equals(is)) {
                    amountFound += e.getValue().getAmount();
                }
            }
        }
        return checkAmount ? amount <= amountFound : amountFound > 0;
    }

    public boolean setLinkedNPC(String name) {
        Iterator<NPC> it = CitizensAPI.getNPCRegistry().iterator();
        while (it.hasNext()) {
            NPC linkedNPC = it.next();
            if (linkedNPC.getName().equals(name)) {
                if (linkedNPC.hasTrait(StockRoomTrait.class)) {
                    // Check to see trying to link to NPC that is already linked to this NPC.
                    if (linkedNPC.getTrait(StockRoomTrait.class).getLinkedNPC().getId() == npc.getId()) {
                        return false;
                    } else {
                        linkedNPCID = linkedNPC.getId();
                    }
                    return true;
                }
            }
        }

        return false;
    }

    public boolean removeLinkedNPC() {
        linkedNPCID = -1;
        return true;
    }

    public boolean isLinkedNPC() {
        if (linkedNPCID > -1) {
            return true;
        }

        return false;
    }

    public NPC getLinkedNPC() {
        // Can't get NPC with id -1
        if(linkedNPCID == -1) {
            return null;
        }
        
        NPC linkedNPC = CitizensAPI.getNPCRegistry().getById(linkedNPCID);
        if (linkedNPC != null) {
            if (linkedNPC.hasTrait(StockRoomTrait.class)) {
                return linkedNPC;
            }
        }
        return null;
    }

    public boolean setLinkedChest(Location loc, String catagory) {
        if (loc.getBlock().getType().equals(Material.CHEST)) {
            linkedChests.put(loc, catagory);
            return true;
        }

        return false;
    }

    public boolean removeLinkedChest(Location loc) {
        if (linkedChests.containsKey(loc)) {
            linkedChests.remove(loc);
            return true;
        }

        return false;
    }

    public boolean setLinkedChest(Location loc) {
        return setLinkedChest(loc, "default");
    }

    public boolean hasLinkedChest() {
        if (linkedChests.size() > 0) {
            return true;
        }
        return false;
    }

    public void setDisabled(boolean value) {
        disabled = value;
    }

    public boolean getDisabled() {
        return disabled;
    }

    public double getSellPrice(ItemStack is) {
        ItemStack i = is.clone();
        i.setAmount(1);
        if (isLinkedNPC()) {
            NPC linkedNPC = getLinkedNPC();
            if (linkedNPC != null) {
                return linkedNPC.getTrait(StockRoomTrait.class).getSellPrices().containsKey(i) ? linkedNPC.getTrait(StockRoomTrait.class).getSellPrices().get(i) : 0;
            }
        }
        double price = sellPrices.containsKey(i) ? sellPrices.get(i) : 0;
        return price;

    }

    public Map<ItemStack, Double> getSellPrices() {
        return sellPrices;
    }

    public void setSellPrice(ItemStack is, double price) {
        ItemStack i = is.clone();
        i.setAmount(1);
        if (price == -1) {
            if (sellPrices.containsKey(i)) {
                sellPrices.remove(i);
            } else {
                System.out.println("Item not found!");
            }
            return;
        }

        sellPrices.put(i, price);

    }

    public boolean setSellStack(ItemStack is, int amount) {
        ItemStack i = is.clone();
        i.setAmount(1);
        if (amount == -1) {
            if (stackSizes.containsKey(i)) {
                stackSizes.remove(i);
            }
            return true;
        }
        stackSizes.put(i, amount);
        return true;
    }

    public int getSellStack(ItemStack is) {
        ItemStack i = is.clone();
        i.setAmount(1);

        return stackSizes.containsKey(i) ? stackSizes.get(i) : 1;
    }

    public double getBuyPrice(ItemStack is) {
        ItemStack i = is.clone();
        i.setAmount(1);
        if (isLinkedNPC()) {
            NPC linkedNPC = CitizensAPI.getNPCRegistry().getById(linkedNPCID);
            return linkedNPC.getTrait(StockRoomTrait.class).getBuyPrices().containsKey(i) ? linkedNPC.getTrait(StockRoomTrait.class).getBuyPrices().get(i) : 0;
        }
        return buyPrices.containsKey(i) ? buyPrices.get(i) : 0;

    }

    public Map<ItemStack, Double> getBuyPrices() {
        return buyPrices;
    }

    public void setBuyPrice(ItemStack is, double price) {
        ItemStack i = is.clone();
        i.setAmount(1);
        if (price == -1) {
            if (buyPrices.containsKey(i)) {
                buyPrices.remove(i);
            }
            return;
        }
        buyPrices.put(i, price);
    }

    public void openStockRoom(Player player) {
        // TODO Auto-generated method stub
        TraderStatus state = Trader.getStatus(player.getName());
        state.setTrader(npc);
        state.setStatus(Status.STOCKROOM);
        player.openInventory(getInventory());

    }

    public void openSalesWindow(Player player) {
        TraderStatus state = Trader.getStatus(player.getName());

        if (state.getStatus() == Status.ITEM_SELECT || state.getStatus() == Status.AMOUNT_SELECT) {
            state.setStatus(Status.ITEM_SELECT);
            buildSalesWindow(state.getInventory());
        } else {
            state.setTrader(npc);
            state.setStatus(Status.ITEM_SELECT);
            state.setInventory(constructViewing());
            player.openInventory(state.getInventory());
            //buildSalesWindow(state);
        }
    }

    public void openBuyWindow(Player player) {
        TraderStatus state = Trader.getStatus(player.getName());
        state.setTrader(npc);
        if (state.getStatus() == Status.NOT) {

            state.setStatus(Status.SELL_BOX);
            Inventory i = constructSellBox();
            state.setInventory(i);
            player.openInventory(i);

        }

    }

    public void openSalesWindowtest(Player player) {
        TraderStatus state = Trader.getStatus(player.getName());
        state.setTrader(npc);
        if (state.getStatus() == Status.NOT) {
            Inventory i = constructSellBox();
            state.setInventory(i);
        }
    }

    public void processInventoryClick(InventoryClickEvent event) {
        TraderStatus state = Trader.getStatus(event.getWhoClicked().getName());

        //stop if not item or not in trade windows
        if (event.getCurrentItem() == null || state.getStatus() == Status.NOT) {
            return;
        }


        //cancel the event.
        if (state.getStatus() != Status.STOCKROOM && state.getStatus() != Status.SELL_BOX) {
            event.setCancelled(true);
        }


        if (event.getRawSlot() == 45 && state.getStatus() == Status.AMOUNT_SELECT) {
            openSalesWindow((Player) event.getWhoClicked());
            return;
        }
        // Return if no item is selected.
        if (event.getCurrentItem().getType().equals(Material.AIR)) {
            return;
        }

        switch (state.getStatus()) {

            //selecting item to purchase
            case ITEM_SELECT: {
                if (!TraderUtils.isTopInventory(event)) {
                    break;
                }
                //if (event.isShiftClick()) {
                //event.setCancelled(true);
                //}
                if (event.isLeftClick()) {

                    buildSellWindow(event.getCurrentItem().clone(), state);
                } else {
                    Player p = (Player) event.getWhoClicked();
                    ItemStack is = event.getCurrentItem();
                    if (is == null) {
                        return;
                    }
                    double price = state.getTrader().getTrait(StockRoomTrait.class).getSellPrice(is);

                    if (getSellStack(is) > 1) {
                        p.sendMessage("Item costs (" + getSellStack(is) + "):");
                    } else {
                        p.sendMessage("Item costs: ");
                    }
                    p.sendMessage("" + price);
                }
            }
            break;

            //Amount selection window
            case AMOUNT_SELECT: {
                if (!TraderUtils.isTopInventory(event)) {
                    break;
                }
                if (event.isLeftClick()) {
                    System.out.println("AMOUNT SELECTED");
                    Player player = (Player) event.getWhoClicked();
                    sellToPlayer(player, state.getTrader(), event.getCurrentItem());
                } else {
                    Player p = (Player) event.getWhoClicked();
                    //double price = state.getTrader().getTrait(StockRoomTrait.class).getSellPrice(event.getCurrentItem()) * event.getCurrentItem().getAmount();
                    double price = state.getTrader().getTrait(StockRoomTrait.class).getSellPrice(event.getCurrentItem()) * (event.getCurrentItem().getAmount() / getSellStack(event.getCurrentItem()));

                    p.sendMessage("Stack costs:");
                    p.sendMessage("" + price);
                }
            }
            break;

            case SELL_BOX: {
                if (!TraderUtils.isTopInventory(event) && !TraderUtils.isBottomInventory(event)) {
                    break;
                }

                if (event.isShiftClick()) {
                    event.setCancelled(true);
                    return;
                }
                if (event.isRightClick()) {
                    Player p = (Player) event.getWhoClicked();
                    double price = state.getTrader().getTrait(StockRoomTrait.class).getBuyPrice(event.getCurrentItem());
                    p.sendMessage(ChatColor.GOLD + "Item price: ");
                    p.sendMessage(ChatColor.GOLD + "" + price);
                    p.sendMessage(ChatColor.GOLD + "Stack price:");
                    p.sendMessage(ChatColor.GOLD + "" + price * event.getCurrentItem().getAmount());
                    event.setCancelled(true);
                }
            }

        }




    }

    private void sellToPlayer(Player player, NPC npc, final ItemStack isold) {
        //TODO: If admin shop, do not deduct items.
        StockRoomTrait store = npc.getTrait(StockRoomTrait.class);
        TraderStatus state = Trader.getStatus(player.getName());
        Material material = isold.getType();
        ItemStack is = isold.clone();

        if (store.hasStock(is, true)) {

            final Inventory playerInv = player.getInventory();

            Inventory chkr = Bukkit.createInventory(null, 9 * 4);

            for (ItemStack item : playerInv.getContents()) {
                try {
                    ItemStack newItem = item.clone();
                    chkr.addItem(newItem);
                } catch (Exception e) {
                }
            }
            //chkr.setContents(playerInv.getContents());
            if (chkr.addItem(is).size() > 0) {
                player.sendMessage(ChatColor.RED + "You do not have enough space to purchase that item");
            } else {
                //check econ
                WalletTrait wallet = npc.getTrait(WalletTrait.class);
                double cost = (isold.getAmount() / getSellStack(isold)) * store.getSellPrice(isold);
                String playerName = player.getName();
                if (CitiTrader.economy.has(playerName, cost)) {
                    if (CitiTrader.economy.withdrawPlayer(playerName, cost).type == ResponseType.SUCCESS) {
                        if (wallet.deposit(cost)) {
                            boolean transaction = true;
                            if (npc.getTrait(WalletTrait.class).getType() != WalletType.ADMIN) {

                                if (hasLinkedChest()) {
                                    Integer amount = isold.getAmount();
                                    Inventory tempinv = Bukkit.createInventory(null, 9 * 4);
                                    double refund = 0;
                                    for (Entry<Location, String> loc : linkedChests.entrySet()) {
                                        if (locationIsBlock(loc.getKey(), Material.CHEST)) {
                                            if (!chestToFar(loc.getKey(), player)) {
                                                Inventory blockInv = ((Chest) loc.getKey().getBlock().getState()).getBlockInventory();
                                                for (Entry<Integer, ? extends ItemStack> e : blockInv.all(material).entrySet()) {
                                                    is.setAmount(e.getValue().getAmount());
                                                    if (e.getValue().equals(is)) {
                                                        //System.out.println("item found");
                                                        if (e.getValue().getAmount() > amount) {
                                                            //System.out.println("removing " + isold.toString());
                                                            blockInv.removeItem(isold);
                                                            amount = 0;
                                                        } else {
                                                            //System.out.println("removing " + e.toString());
                                                            blockInv.removeItem(e.getValue());
                                                            tempinv.addItem(e.getValue());
                                                            //isold.setAmount(isold.getAmount() - e.getValue().getAmount());
                                                            amount -= e.getValue().getAmount();
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        if (!(getSellStack(is) > 1)) {
                                            refund = amount * store.getSellPrice(is);
                                        } else if (amount > 0) {
                                            // revert and kill
                                            transaction = false;
                                            for (ItemStack replace : tempinv) {
                                                for (Entry<Location, String> locs : state.getTrader().getTrait(StockRoomTrait.class).linkedChests.entrySet()) {
                                                    if (locationIsBlock(locs.getKey(), Material.CHEST)) {
                                                        if (!chestToFar(locs.getKey(), player)) {
                                                            Inventory blockInv = ((Chest) loc.getKey().getBlock().getState()).getBlockInventory();

                                                            if (checkSpace(blockInv, replace.clone())) {
                                                                blockInv.addItem(replace);
                                                                break;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                    }
                                    if (refund > 0) {
                                        CitiTrader.economy.depositPlayer(playerName, cost);
                                        player.sendRawMessage("Refund : " + cost);
                                    }
                                } else {
                                    store.getInventory().removeItem(isold);
                                }
                            }

                            if (transaction) {
                                player.sendMessage(ChatColor.GOLD + isold.getType().name() + "*" + isold.getAmount());
                                player.sendMessage(ChatColor.GOLD + "purchased");
                                player.sendMessage(ChatColor.GOLD + "" + cost);

                                playerInv.addItem(isold);
                            } else if (CitiTrader.economy.depositPlayer(playerName, cost).type != ResponseType.SUCCESS) {
                                System.out.println("SEVERE ERROR: FAILED TO ROLLBACK TRANSACTION, PLEASE RECREDIT " + playerName + " " + cost);
                                player.sendMessage(ChatColor.RED + "An error occured, please notify an operator to refund your account.");
                            }

                            buildSellWindow(isold, state);
                        } else {
                            if (CitiTrader.economy.depositPlayer(playerName, cost).type != ResponseType.SUCCESS) {
                                System.out.println("SEVERE ERROR: FAILED TO ROLLBACK TRANSACTION, PLEASE RECREDIT " + playerName + " " + cost);
                                player.sendMessage(ChatColor.RED + "An error occured, please notify an operator to refund your account.");
                            }
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "Could not transfer funds");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have enough money!");
                }


            }
        } else {
            player.sendMessage(ChatColor.RED + "Not enough items to purchase");
        }
    }

    public void processInventoryClose(InventoryCloseEvent event) {
        TraderStatus state = Trader.getStatus(event.getPlayer().getName());

        if (state.getStatus() == Status.SELL_BOX) {
            Inventory sellbox = state.getInventory();
            Double total = 0.0D;
            for (int i = 0; i < sellbox.getSize(); i++) {
                ItemStack is = sellbox.getItem(i);

                if (is == null) {
                    continue;
                }

                double price = state.getTrader().getTrait(StockRoomTrait.class).getBuyPrice(is);

                //check we buy it.
                if (price == 0.0D) {
                    continue;
                }


// start chest loop here
                Inventory targetInv = null;
                //check space
                if (state.getTrader().getTrait(StockRoomTrait.class).hasLinkedChest()) {
                    for (Entry<Location, String> loc : state.getTrader().getTrait(StockRoomTrait.class).linkedChests.entrySet()) {
                        if (locationIsBlock(loc.getKey(), Material.CHEST)) {
                            if (!chestToFar(loc.getKey(), ((Player) event.getPlayer()))) {
                                Inventory blockInv = ((Chest) loc.getKey().getBlock().getState()).getBlockInventory();

                                if (checkSpace(blockInv, is.clone())) {
                                    targetInv = blockInv;
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    if (!checkSpace(state.getTrader().getTrait(StockRoomTrait.class).getInventory(), is)) {
                        ((CommandSender) event.getPlayer()).sendMessage(ChatColor.RED + "Trader does not have enough space to hold something you sold him.");
                    } else {
                        targetInv = state.getTrader().getTrait(StockRoomTrait.class).getInventory();
                    }
                }

                if (targetInv == null) {
                    continue;
                }

                WalletTrait wallet = state.getTrader().getTrait(WalletTrait.class);
                //check we have the cash
                double sale = price * is.getAmount();
                if (!wallet.has(sale)) {
                    ((CommandSender) event.getPlayer()).sendMessage(ChatColor.RED + "Trader does not have the funds to pay you.");
                    break;
                }

                //give cash
                if (!wallet.withdraw(sale)) {
                    ((CommandSender) event.getPlayer()).sendMessage(ChatColor.RED + "Trader couldn't find their wallet.");
                    break;
                }

                if (!CitiTrader.economy.depositPlayer(event.getPlayer().getName(), sale).transactionSuccess()) {
                    ((CommandSender) event.getPlayer()).sendMessage(ChatColor.RED + "Couldn't find your wallet.");
                    wallet.deposit(sale);
                    break;
                }
                total += sale;
                //take item
                sellbox.setItem(i, null);
                if (state.getTrader().getTrait(WalletTrait.class).getType() != WalletType.ADMIN) {
                    //state.getTrader().getTrait(StockRoomTrait.class).getInventory().addItem(is);
                    targetInv.addItem(is);
                    System.out.println("Adding item " + is.toString());
                }


            }


            ((Player) event.getPlayer()).sendMessage("Total money from sale to trader: " + total);
            //drop all items in sellbox inventory
            Iterator<ItemStack> it = state.getInventory().iterator();
            while (it.hasNext()) {
                ItemStack is = it.next();
                if (is != null) {
                    event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation().add(0.5, 0.0, 0.5), is);
                }
            }

        }

        Trader.clearStatus(event.getPlayer().getName());


    }

    /**
     *
     * @param loc
     * @param player
     * @return
     */
    public boolean chestToFar(Location loc, Player player) {
        if (loc.distance(npc.getBukkitEntity().getLocation()) < 10) {
            return false;
        }

        return true;
    }

    public boolean locationIsBlock(Location loc, Material mat) {
        if (loc.getBlock().getType().equals(mat)) {
            return true;
        }
        System.out.println("Location is not a chest.");
        return false;
    }

    public boolean checkSpace(Inventory inv, ItemStack item) {
        Inventory chkr = Bukkit.createInventory(null, inv.getSize());
        //chkr.setContents(inv.getContents().clone());
        ItemStack chkitem = null;
        for (ItemStack is : inv.getContents()) {
            if (is != null) {
                chkitem = is.clone();
                chkr.addItem(chkitem);
            }
        }

        if (chkr.addItem(item).size() > 0) {
            return false;
        }

        return true;
    }

    public boolean isStockRoomEmpty() {
        for (ItemStack is : stock) {
            if (is != null) {
                return false;
            }
        }

        return true;
    }

    public boolean isEnableLeftClick() {
        return enableLeftClick;
    }

    public void setEnableLeftClick(boolean enableLeftClick) {
        this.enableLeftClick = enableLeftClick;
    }

    public boolean isEnableRightClick() {
        return enableRightClick;
    }

    public void setEnableRightClick(boolean enableRightClick) {
        this.enableRightClick = enableRightClick;
    }

    public void buildSellWindow(final ItemStack item, final TraderStatus state) {
        CitiTrader.self.getServer().getScheduler().scheduleAsyncDelayedTask(CitiTrader.self, new Runnable() {
            @Override
            public void run() {
                ItemStack is = item.clone();
                //clear the inventory
                for (int i = 0; i < 54; i++) {
                    state.getInventory().setItem(i, null);
                }
                //if (hasLinkedChest()) {
                //set up the amount selection
                int k = 0;
                ItemStack newIs = is.clone();
                int in = 1;
                System.out.println(newIs.toString());
                newIs.setAmount(1);
                if (stackSizes.containsKey(newIs)) {


                    in = stackSizes.get(newIs);
                }
                for (int i = in; i <= 64; i *= 2) {
                    if (i <= is.getMaxStackSize()) {
                        newIs = is.clone();
                        newIs.setAmount(i);
                        if (hasStock(newIs, true)) {
                            state.getInventory().setItem(k, newIs);
                        }
                        k++;
                    }
                }
                //}

                //set up the amount selection
                /*int k = 0;
                 for (int i = 1; i <= 64; i *= 2) {
                 if (i <= is.getMaxStackSize()) {
                 ItemStack newIs = is.clone();
                 newIs.setAmount(i);
                 if (hasStock(newIs, true)) {
                 state.getInventory().setItem(k, newIs);
                 }
                 k++;
                 }
                 }*/
                state.getInventory().setItem(45, new ItemStack(Material.ARROW, 1));
                state.setStatus(Status.AMOUNT_SELECT);
            }
        });

        //System.out.println("ITEM SELECTED");
    }

    public void buildSalesWindow(final Inventory inv) {


        System.out.println("Trader has Linked Chest: " + hasLinkedChest());
        CitiTrader.self.getServer().getScheduler().scheduleAsyncDelayedTask(CitiTrader.self, new Runnable() {
            @Override
            public void run() {
                //clear the inventory
                for (int i = 0; i < 54; i++) {
                    inv.setItem(i, null);
                }
                if (hasLinkedChest()) {
                    for (Entry<Location, String> loc : linkedChests.entrySet()) {
                        if (loc.getKey().getBlock().getType().equals(Material.CHEST)) {
                            if (loc.getKey().distance(npc.getBukkitEntity().getLocation()) < 10) {
                                for (ItemStack is : ((Chest) loc.getKey().getBlock().getState()).getBlockInventory()) {
                                    if (is == null) {
                                        continue;
                                    }
                                    ItemStack chk = new ItemStack(is.getType(), 1, is.getDurability());
                                    chk.addEnchantments(is.getEnchantments());
                                    if (inv.contains(chk) == false && getSellPrice(is) > 0.0D) {
                                        inv.addItem(chk);
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
                } else {
                    for (ItemStack is : stock) {
                        if (is == null) {
                            continue;
                        }
                        ItemStack chk = new ItemStack(is.getType(), 1, is.getDurability());
                        chk.addEnchantments(is.getEnchantments());
                        if (inv.contains(chk) == false && getSellPrice(is) > 0.0D) {
                            inv.addItem(chk);
                        }
                    }
                }
            }
        });
    }
}
