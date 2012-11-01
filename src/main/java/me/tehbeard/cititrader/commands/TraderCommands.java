/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.tehbeard.cititrader.commands;

import me.tehbeard.cititrader.CitiTrader;
import me.tehbeard.cititrader.Trader;
import me.tehbeard.cititrader.TraderStatus;
import me.tehbeard.cititrader.traits.ShopTrait;
import me.tehbeard.cititrader.traits.WalletTrait;
import me.tehbeard.cititrader.utils.ArgumentPack;
import net.citizensnpcs.Citizens;
import net.citizensnpcs.Settings.Setting;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.PlayerCreateNPCEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.MobType;
import net.citizensnpcs.api.trait.trait.Owner;
import net.citizensnpcs.command.Command;
import net.citizensnpcs.command.CommandContext;
import net.citizensnpcs.command.Requirements;
import net.citizensnpcs.util.Messages;
import net.citizensnpcs.util.Messaging;
import net.citizensnpcs.util.StringHelper;
import net.citizensnpcs.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 *
 * @author tenowg
 */
@Requirements
public class TraderCommands {

    public final CitiTrader plugin;

    public TraderCommands(CitiTrader plugin) {
        this.plugin = plugin;
    }

    @Command(
            aliases = {"tradertest", "ctradertest"},
    desc = "Show basic Trader information")
    public void cititraders(CommandContext args, CommandSender player, NPC npc) {
        Messaging.send(player, " <7>-- <c>Written by tenowg and tehBeard");
        Messaging.send(player, " <7>-- <c>Source Code: http://github.com/tenowg");
        Messaging.send(player, " <7>-- <c>Website: " + plugin.getDescription().getWebsite());
    }

    @Command(aliases = {"trader"},
    desc = "Creates a Cititrader NPC.",
    usage = "create [name] --type (type) --trait ('trait1, trait2...')",
    flags = "bu",
    modifiers = {"create"},
    min = 2,
    permission = "trader.create")
    public void create(CommandContext args, CommandSender sender, NPC npc) {
        String name = StringHelper.parseColors(args.getJoinedStrings(1));
        if (name.length() > 16) {
            Messaging.sendErrorTr(sender, Messages.NPC_NAME_TOO_LONG);
            name = name.substring(0, 15);
        }

        EntityType type = EntityType.PLAYER;
        if (args.hasValueFlag("type")) {
            String inputType = args.getFlag("type");
            type = Util.matchEntityType(inputType);
            if (type == null) {
                Messaging.sendErrorTr(sender, Messages.NPC_CREATE_INVALID_MOBTYPE, inputType);
                type = EntityType.PLAYER;
            } else if (!LivingEntity.class.isAssignableFrom(type.getEntityClass())) {
                Messaging.sendErrorTr(sender, Messages.NOT_LIVING_MOBTYPE, type);
                type = EntityType.PLAYER;
            }
        }

        int owned = 0;

        for (NPC npcs : CitizensAPI.getNPCRegistry()) {
            if (npcs.hasTrait(ShopTrait.class)) {
                if (npcs.getTrait(Owner.class).getOwner().equalsIgnoreCase(sender.getName())) {
                    owned += 1;
                }
            }
        }

        int traderLimit = plugin.getTraderLimit((Player) sender);
        if (traderLimit != -1 && traderLimit <= owned) {
            //sender.sendMessage(ChatColor.RED + "Cannot spawn another trader NPC!");
            throw new CommandException("Cannot spawn another trader NPC!");
        }

        npc = CitizensAPI.getNPCRegistry().createNPC(type, name);
        String msg = "You created [[" + npc.getName() + "]]";

        msg += ".";

        // Initialize necessary traits
        if (!Setting.SERVER_OWNS_NPCS.asBoolean()) {
            npc.getTrait(Owner.class).setOwner(sender.getName());
        }
        npc.getTrait(MobType.class).setType(type);
        Location spawnLoc = null;
        if (sender instanceof Player) {
            spawnLoc = ((Player) sender).getLocation();
            PlayerCreateNPCEvent event = new PlayerCreateNPCEvent((Player) sender, npc);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                npc.destroy();
                String reason = "Couldn't create NPC.";
                if (!event.getCancelReason().isEmpty()) {
                    reason += " Reason: " + event.getCancelReason();
                }
                throw new CommandException(reason);
            }
        }

        if (spawnLoc == null) {
            npc.destroy();
            throw new CommandException(Messages.INVALID_SPAWN_LOCATION);
        }

        npc.spawn(spawnLoc);

        CitiTrader.Style style = CitiTrader.Style.TRADER;

        Trader.setUpNPC(npc, style);

        ((Citizens) CitizensAPI.getPlugin()).getNPCSelector().select(sender, npc);
        Messaging.send(sender, msg);
    }

    @Command(aliases = {"trader"},
    desc = "Sets a Sellprice",
    usage = "sellprice <price> --group (group) --player (player) (-r)",
    flags = "r",
    modifiers = {"sellprice"},
    min = 1,
    permission = "trader.commands.sellprice")
    public void sellPrice(CommandContext args, CommandSender sender, NPC npc) {
        TraderStatus state = Trader.getStatus(((Player) sender).getName());
        if (state.getStatus().equals(TraderStatus.Status.SET_PRICE_BUY)) {
            sender.sendMessage(ChatColor.YELLOW + "Please finish setting your buy price first");
            sender.sendMessage(ChatColor.YELLOW + "Or cancel with /trader cancel");
            return;
        }
        state.setStatus(TraderStatus.Status.SET_PRICE_SELL);

        double price;
        if (args.hasFlag('r')) {
            price = -1;
        } else {
            price = args.getDouble(1);
        }

        state.setMoney(price);
        sender.sendMessage(ChatColor.DARK_PURPLE + "Now right click with item to finish.");
    }

    @Command(aliases = {"trader"},
    desc = "Sets a Buyprice",
    usage = "buyprice <price> --group (group) --player (player) (-r)",
    flags = "r",
    modifiers = {"buyprice"},
    min = 1,
    permission = "trader.commands.buyprice")
    public void buyPrice(CommandContext args, CommandSender sender, NPC npc) {
        TraderStatus state = Trader.getStatus(((Player) sender).getName());
        if (state.getStatus().equals(TraderStatus.Status.SET_PRICE_SELL)) {
            sender.sendMessage(ChatColor.YELLOW + "Please finish setting your sell price first");
            sender.sendMessage(ChatColor.YELLOW + "Or cancel with /trader cancel");
            return;
        }
        state.setStatus(TraderStatus.Status.SET_PRICE_BUY);

        double price;
        if (args.hasFlag('r')) {
            price = -1;
        } else {
            price = args.getDouble(1);
        }
        state.setMoney(price);
        sender.sendMessage(ChatColor.DARK_PURPLE + "Now right click with item to finish.");
    }

    @Command(aliases = {"trader"},
    desc = "Sets the Traders Wallet Type.",
    usage = "setwallet [owner|bank|town_bank|private] (--bank (name))",
    modifiers = {"setwallet"},
    min = 2,
    permission = "trader.commands.setwallet")
    public void setWallet(CommandContext args, CommandSender sender, NPC npc) {
        String walltype = args.getString(1);
        if (args == null) {
            sender.sendMessage(ChatColor.RED + "Wallet Type needed!");
            return;
        }

        TraderStatus state = Trader.getStatus(((Player) sender).getName());
        WalletTrait.WalletType type = WalletTrait.WalletType.valueOf(walltype.toUpperCase());
        if (type == null) {
            sender.sendMessage(ChatColor.RED + "Invalid Wallet Type!");
            return;
        }

        if (type == WalletTrait.WalletType.BANK && !args.hasValueFlag("bank")) {
            sender.sendMessage(ChatColor.RED + "An account name is needed for this type of wallet (add --bank (name))");
            return;
        } else {
            state.setAccName(args.getFlag("bank"));
        }


        if (type.equals(WalletTrait.WalletType.TOWN_BANK)) {
            if (!CitiTrader.isTowny) {
                sender.sendMessage(ChatColor.RED + "Towny is not enabled on your server.");
                return;
            }
            String bank = plugin.getTownBank((Player) sender);

            if (bank == null) {
                sender.sendMessage(ChatColor.RED + "You are not the mayor or assistant of this town.");
                return;
            }
            state.setAccName(bank);

        }

        if (!type.hasPermission(sender)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this wallet type!");
            return;
        }

        state.setStatus(TraderStatus.Status.SET_WALLET);
        state.setWalletType(type);
        sender.sendMessage(ChatColor.DARK_PURPLE + "Right click trader to set his wallet!");
    }

    @Command(aliases = {"trader"},
    desc = "Various wallet functions.",
    usage = "wallet [balance|give|take]",
    modifiers = {"wallet"},
    min = 2,
    permission = "trader.commands.wallet")
    public void doWallet(CommandContext args, CommandSender sender, NPC npc) {
        TraderStatus status = Trader.getStatus(sender.getName());

        String transaction = args.getString(1);

        if (transaction.equalsIgnoreCase("balance")) {
            status.setStatus(TraderStatus.Status.BALANCE_MONEY);
            sender.sendMessage(ChatColor.DARK_PURPLE + "Right click the Trader to see his balance.");
            return;
        }

        if (args.argsLength() < 3) {
            sender.sendMessage(ChatColor.RED + "Transaction type and amount needed.");
            return;
        }

        if (transaction.equalsIgnoreCase("give")) {
            status.setStatus(TraderStatus.Status.GIVE_MONEY);
            status.setMoney(args.getDouble(2));
            sender.sendMessage(ChatColor.DARK_PURPLE + "Right click the Trader you would like to give money too.");
        }

        if (transaction.equalsIgnoreCase("take")) {
            status.setStatus(TraderStatus.Status.TAKE_MONEY);
            status.setMoney(args.getDouble(2));
            sender.sendMessage(ChatColor.DARK_PURPLE + "Right click the Trader you would like to take money from.");
        }
    }

    @Command(aliases = {"trader"},
    desc = "Releave your trader of his possition.",
    usage = "fire",
    modifiers = {"fire"},
    min = 1,
    permission = "trader.commands.fire")
    public void doFire(CommandContext args, CommandSender sender, NPC npc) {
        TraderStatus status = Trader.getStatus(sender.getName());
        status.setStatus(TraderStatus.Status.FIRING);
        sender.sendMessage(ChatColor.DARK_PURPLE + "Right click the Trader you want to fire.");
    }

    @Command(aliases = {"trader"},
    desc = "Cancels all transaction currently in use with a player.",
    usage = "cancel",
    modifiers = {"cancel"},
    min = 1,
    permission = "trader.commands.cancel")
    public void doCancel(CommandContext args, CommandSender sender, NPC npc) {
        Trader.clearStatus(sender.getName());
        sender.sendMessage(ChatColor.GREEN + "Status reset.");
    }

    @Command(aliases = {"trader"},
    desc = "Displays the version of CitiTraders currently in use.",
    usage = "version",
    modifiers = {"version"},
    min = 1)
    public void doVersion(CommandContext args, CommandSender sender, NPC npc) {
        sender.sendMessage("Running Cititraders version: " + plugin.getDescription().getVersion());
        sender.sendMessage("With build number: " + CitiTrader.atts.getValue("Build-Tag"));
    }

    @Command(aliases = {"trader"},
    desc = "Reloads the profiles.yml file after a manual edit.",
    usage = "reloadprofiles",
    modifiers = {"reloadprofiles"},
    min = 1,
    permission = "trader.commands.reloadprofiles")
    public void doReloadProfiles(CommandContext args, CommandSender sender, NPC npc) {
        plugin.reloadProfiles();
    }

    @Command(aliases = {"trader"},
    desc = "Closes the Traders shop from doing business.",
    usage = "disable",
    modifiers = {"disable"},
    min = 1,
    permission = "trader.commands.disable")
    public void doDisable(CommandContext args, CommandSender sender, NPC npc) {
        TraderStatus status = Trader.getStatus(sender.getName());
        sender.sendMessage(ChatColor.DARK_PURPLE + "Right click the Trader you want to disable.");
        status.setStatus(TraderStatus.Status.DISABLE);
    }

    @Command(aliases = {"trader"},
    desc = "Opens the Traders shop for business.",
    usage = "enable",
    modifiers = {"enable"},
    min = 1,
    permission = "trader.commands.enable")
    public void doEnable(CommandContext args, CommandSender sender, NPC npc) {
        TraderStatus status = Trader.getStatus(sender.getName());
        sender.sendMessage(ChatColor.DARK_PURPLE + "Right click the Trader you want to enable.");
        status.setStatus(TraderStatus.Status.ENABLE);
    }

    @Command(aliases = {"trader"},
    desc = "Links two NPCs to share one pricelist.",
    usage = "link",
    modifiers = {"link"},
    min = 1,
    permission = "trader.commands.link")
    public void doLink(CommandContext args, CommandSender sender, NPC npc) {
        if (!(args.argsLength() > 1)) {

            sender.sendMessage(ChatColor.YELLOW + "You need to name a trader to link too.");
            return;
        }

        String name = args.getJoinedStrings(1);
        TraderStatus status = Trader.getStatus(sender.getName());
        sender.sendMessage(ChatColor.DARK_PURPLE + "Right click the Trader you want to link to " + name);
        status.setLinkedNPC(name);
        status.setStatus(TraderStatus.Status.SET_LINK);
    }

    @Command(aliases = {"trader"},
    desc = "Removes the Link between two Traders",
    usage = "removelink",
    modifiers = {"removelink"},
    min = 1,
    permission = "trader.commands.removelink")
    public void removeLink(CommandContext args, CommandSender sender, NPC npc) {
        TraderStatus status = Trader.getStatus(sender.getName());
        sender.sendMessage(ChatColor.DARK_PURPLE + "Right click the Trader you want to remove the link from.");
        status.setStatus(TraderStatus.Status.REMOVE_LINK);
    }

    @Command(aliases = {"trader"},
    desc = "Links a Chest to an NPC",
    usage = "linkchest",
    modifiers = {"linkchest"},
    min = 1,
    permission = "trader.commands.linkchest")
    public void linkChest(CommandContext args, CommandSender sender, NPC npc) {
        TraderStatus status = Trader.getStatus(sender.getName());
        sender.sendMessage(ChatColor.DARK_PURPLE + "Right click the Chest you want to link.");
        status.setStatus(TraderStatus.Status.SELECT_LINK_CHEST);
    }

    @Command(aliases = {"trader"},
    desc = "Links a Chest to an NPC",
    usage = "unlinkchest",
    modifiers = {"unlinkchest"},
    min = 1,
    permission = "trader.commands.unlinkchest")
    public void unlinkChest(CommandContext args, CommandSender sender, NPC npc) {
        TraderStatus status = Trader.getStatus(sender.getName());
        sender.sendMessage(ChatColor.DARK_PURPLE + "Right click the Chest you want to unlink.");
        status.setStatus(TraderStatus.Status.SELECT_UNLINK_CHEST);
    }

    @Command(aliases = {"trader"},
    desc = "Set the size of the stack to sell.",
    usage = "sellstack",
    modifiers = {"sellstack"},
    min = 2,
    max = 2,
    permission = "trader.commands.sellstack")
    public void sellStack(CommandContext args, CommandSender sender, NPC npc) {
        TraderStatus status = Trader.getStatus(sender.getName());
        sender.sendMessage(ChatColor.DARK_PURPLE + "Right click the Trader with the item you want to set.");
        status.setStackAmount(args.getInteger(1));
        status.setStatus(TraderStatus.Status.SET_SELL_STACK);
    }

    @Command(aliases = {"trader"},
    desc = "Lists the Buy/Sell options of an NPC.",
    usage = "list (buy|sell)",
    modifiers = {"list"},
    min = 2,
    max = 2,
    permission = "trader.commands.list")
    public void doList(CommandContext args, CommandSender sender, NPC npc) {
        if (!args.getString(1).equalsIgnoreCase("buy") && !args.getString(1).equalsIgnoreCase("sell")) {
            sender.sendMessage(ChatColor.RED + "Command syntax is /trader list <buy|sell>");
            return;
        }

        TraderStatus status = Trader.getStatus(sender.getName());
        sender.sendMessage(ChatColor.DARK_PURPLE + "Right click a Trader to see their price list.");
        if (args.getString(1).equalsIgnoreCase("buy")) {
            status.setStatus(TraderStatus.Status.LIST_BUY_PRICE);
        } else {
            status.setStatus(TraderStatus.Status.LIST_SELL_PRICE);
        }

    }
    
    @Command(aliases = {"trader"},
    desc = "Sets options for inventory.",
    usage = "setinv -i (toggle infinite Inventory) or -s # (multiple of 9) sets the Size of the traders inventory.",
    modifiers = {"setinv"},
    flags = "is",
    min = 2,
    max = 3,
    permission = "trader.commands.setinv")
    public void doSetInv(CommandContext args, CommandSender sender, NPC npc) {
        TraderStatus status = Trader.getStatus(sender.getName());
        if (args.hasFlag('i')) {
            if (sender.hasPermission("trader.commands.setinv.infinite")) {
                Messaging.send(sender, "Right Click Trader to set infinite (static) inventory.");
                status.setStatus(TraderStatus.Status.SET_INFINITE);
                return;
            } else {
                Messaging.send(sender, "You don't have permission to set infinite Inventory.");
                return;
            }
        }
        
        if (args.hasFlag('s')) {
            
        }
    }
}
