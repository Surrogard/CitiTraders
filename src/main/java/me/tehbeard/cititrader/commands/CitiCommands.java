/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.tehbeard.cititrader.commands;

import me.tehbeard.cititrader.CitiTrader;
import me.tehbeard.cititrader.CitiTrader.Style;
import me.tehbeard.cititrader.traits.ShopTrait;
import me.tehbeard.cititrader.Trader;
import me.tehbeard.cititrader.TraderStatus;
import me.tehbeard.cititrader.traits.WalletTrait;
import me.tehbeard.cititrader.utils.ArgumentPack;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.MobType;
import net.citizensnpcs.api.trait.trait.Owner;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

/**
 *
 * @author tenowg
 */
public class CitiCommands implements CommandExecutor {

    private CitiTrader plugin;

    public CitiCommands(CitiTrader instance) {
        plugin = instance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
            String label, String[] args) {

        if (args.length == 0) {
            return false;
        }
        if (sender instanceof Player == false) {
            sender.sendMessage("DOES NOT WORK FROM CONSOLE");
            return true;
        }

        Player player = (Player) sender;
        if (!sender.hasPermission(CitiTrader.PERM_PREFIX + ".command." + args[0])) {
            return false;
        }
        Subcommand subCom;
        try {
            subCom = Subcommand.valueOf(args[0]);
        } catch (Exception e) {
            return false;
        }
        switch (subCom) {
            case create: 
                return create(sender, command, label, args);
            case sellprice: {
                if (args.length == 2) {
                    TraderStatus state = Trader.getStatus(((Player) sender).getName());
                    if (state.getStatus().equals(TraderStatus.Status.SET_PRICE_BUY)) {
                        sender.sendMessage(ChatColor.YELLOW + "Please finish setting your buy price first");
                        sender.sendMessage(ChatColor.YELLOW + "Or cancel with /trader cancel");
                        return true;
                    }
                    state.setStatus(TraderStatus.Status.SET_PRICE_SELL);

                    double price;
                    if (args[1].equalsIgnoreCase("remove")) {
                        price = -1;
                    } else {
                        price = Double.parseDouble(args[1]);
                    }

                    state.setMoney(price);
                    sender.sendMessage(ChatColor.DARK_PURPLE + "Now right click with item to finish.");
                }
                sender.sendMessage(ChatColor.YELLOW + "Proper usage is: /trader sellprice <amount>");
                return true;
            }

            case buyprice: {
                if (args.length == 2) {
                    TraderStatus state = Trader.getStatus(((Player) sender).getName());
                    if (state.getStatus().equals(TraderStatus.Status.SET_PRICE_SELL)) {
                        sender.sendMessage(ChatColor.YELLOW + "Please finish setting your sell price first");
                        sender.sendMessage(ChatColor.YELLOW + "Or cancel with /trader cancel");
                        return true;
                    }
                    state.setStatus(TraderStatus.Status.SET_PRICE_BUY);

                    double price;
                    if (args[1].equalsIgnoreCase("remove")) {
                        price = -1;
                    } else {
                        price = Double.parseDouble(args[1]);
                    }
                    state.setMoney(price);
                    sender.sendMessage(ChatColor.DARK_PURPLE + "Now right click with item to finish.");
                    return true;
                }
                sender.sendMessage(ChatColor.YELLOW + "Proper usage is: /trader buyprice <amount>");
                return true;
            }

            case setwallet: {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Wallet Type needed!");
                    return true;
                }
                TraderStatus state = Trader.getStatus(((Player) sender).getName());
                WalletTrait.WalletType type = WalletTrait.WalletType.valueOf(args[1].toUpperCase());
                if (type == null) {
                    sender.sendMessage(ChatColor.RED + "Invalid Wallet Type!");
                    return true;
                }

                if (type == WalletTrait.WalletType.BANK && args.length != 3) {
                    sender.sendMessage(ChatColor.RED + "An account name is needed for this type of wallet");
                    return true;
                } else {
                    String an = "";
                    if (args.length > 2) {
                        an = args[2];
                    }
                    state.setAccName(an);
                }


                if (type.equals(WalletTrait.WalletType.TOWN_BANK)) {
                    if (!CitiTrader.isTowny) {
                        sender.sendMessage(ChatColor.RED + "Towny is not enabled on your server.");
                        return false;
                    }
                    String bank = plugin.getTownBank(player);

                    if (bank == null) {
                        sender.sendMessage(ChatColor.RED + "You are not the mayor or assistant of this town.");
                        return true;
                    }
                    state.setAccName(bank);

                }

                if (!type.hasPermission(sender)) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this wallet type!");
                    return true;
                }

                state.setStatus(TraderStatus.Status.SET_WALLET);
                state.setWalletType(type);
                sender.sendMessage(ChatColor.DARK_PURPLE + "Right click trader to set his wallet!");

                return true;
            }

            case wallet: {

                if (args[1].equalsIgnoreCase("balance") && args.length == 2) {
                    TraderStatus status = Trader.getStatus(player.getName());
                    status.setStatus(TraderStatus.Status.BALANCE_MONEY);
                    player.sendMessage(ChatColor.DARK_PURPLE + "Right click the Trader to see his balance.");
                    return true;
                } else if (args[1].equalsIgnoreCase("balance") && args.length > 2) {
                    sender.sendMessage(ChatColor.RED + "No modifier is needed for balance.");
                    return true;
                }

                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Transaction type and amount needed.");
                    return true;
                }

                if (args[1].equalsIgnoreCase("give")) {
                    TraderStatus status = Trader.getStatus(player.getName());
                    status.setStatus(TraderStatus.Status.GIVE_MONEY);
                    status.setMoney(Double.parseDouble(args[2]));
                    player.sendMessage(ChatColor.DARK_PURPLE + "Right click the Trader you would like to give money too.");
                }

                if (args[1].equalsIgnoreCase("take")) {
                    TraderStatus status = Trader.getStatus(player.getName());
                    status.setStatus(TraderStatus.Status.TAKE_MONEY);
                    status.setMoney(Double.parseDouble(args[2]));
                    player.sendMessage(ChatColor.DARK_PURPLE + "Right click the Trader you would like to take money from.");
                }


                return true;
            }
            case fire: {
                TraderStatus status = Trader.getStatus(player.getName());
                status.setStatus(TraderStatus.Status.FIRING);
                player.sendMessage(ChatColor.DARK_PURPLE + "Right click the Trader you want to fire.");
                return true;
            }
            case cancel: {
                Trader.clearStatus(player.getName());
                player.sendMessage(ChatColor.GREEN + "Status reset.");
                return true;
            }
            case version: {
                player.sendMessage("Running Cititraders version: " + plugin.getDescription().getVersion());
                player.sendMessage("With build number: " + CitiTrader.atts.getValue("Build-Tag"));
                return true;
            }
            case reloadprofiles: {
                plugin.reloadProfiles();
                return true;
            }
            case disable: {
                TraderStatus status = Trader.getStatus(player.getName());
                player.sendMessage(ChatColor.DARK_PURPLE + "Right click the Trader you want to disable.");
                status.setStatus(TraderStatus.Status.DISABLE);
                return true;
            }
            case enable: {
                TraderStatus status = Trader.getStatus(player.getName());
                player.sendMessage(ChatColor.DARK_PURPLE + "Right click the Trader you want to enable.");
                status.setStatus(TraderStatus.Status.ENABLE);
                return true;
            }

            case link: {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.YELLOW + "You need to name a trader to link too.");
                    return true;
                }
                TraderStatus status = Trader.getStatus(player.getName());
                player.sendMessage(ChatColor.DARK_PURPLE + "Right click the Trader you want to link to " + args[1]);
                status.setLinkedNPC(args[1]);
                status.setStatus(TraderStatus.Status.SET_LINK);
                return true;
            }
            case removelink: {
                TraderStatus status = Trader.getStatus(player.getName());
                player.sendMessage(ChatColor.DARK_PURPLE + "Right click the Trader you want to remove the link from.");
                status.setStatus(TraderStatus.Status.REMOVE_LINK);
                return true;
            }
            case linkchest: {
                TraderStatus status = Trader.getStatus(player.getName());
                player.sendMessage(ChatColor.DARK_PURPLE + "Right click the Chest you want to link.");
                status.setStatus(TraderStatus.Status.SELECT_LINK_CHEST);
                return true;
            }
            case unlinkchest: {
                TraderStatus status = Trader.getStatus(player.getName());
                player.sendMessage(ChatColor.DARK_PURPLE + "Right click the Chest you want to unlink.");
                status.setStatus(TraderStatus.Status.SELECT_UNLINK_CHEST);
                return true;
            }
            case sellstack: {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "You need to input an amount /trader sellstack 16");
                    return true;
                }
                if (args.length > 2) {
                    player.sendMessage(ChatColor.RED + "You have to many arguements /trader sellstack 16");
                }
                TraderStatus status = Trader.getStatus(player.getName());
                player.sendMessage(ChatColor.DARK_PURPLE + "Right click the Trader with the item you want to set.");
                status.setStackAmount(Integer.parseInt(args[1]));
                status.setStatus(TraderStatus.Status.SET_SELL_STACK);
                return true;
            }
            case buystack: {
            }
            case list: {
                if (args.length != 2) {
                    player.sendMessage(ChatColor.RED + "Command syntax is /trader list <buy|sell>");
                    return true;
                }

                if (!args[1].equalsIgnoreCase("buy") && !args[1].equalsIgnoreCase("sell")) {
                    player.sendMessage(ChatColor.RED + "Command syntax is /trader list <buy|sell>");
                    return true;
                }

                TraderStatus status = Trader.getStatus(player.getName());
                player.sendMessage(ChatColor.DARK_PURPLE + "Right click a Trader to see their price list.");
                if (args[1].equalsIgnoreCase("buy")) {
                    status.setStatus(TraderStatus.Status.LIST_BUY_PRICE);
                } else {
                    status.setStatus(TraderStatus.Status.LIST_SELL_PRICE);
                }

                return true;
            }
        }
        return false;
    }

    private String compact(String[] a, int idx) {
        String s = "";
        for (int i = idx; i < a.length; i++) {
            if (s.length() > 0) {
                s += " ";
            }
            s += a[i];
        }
        return s;
    }

    public boolean create(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        ArgumentPack argPack = new ArgumentPack(new String[0], new String[]{"type", "style"}, compact(args, 1));
        EntityType npcType = EntityType.PLAYER;
        Style style = Style.TRADER;
        
        if (argPack.getOption("type") != null && plugin.isValidNPCType(player, argPack.getOption("type").toUpperCase())) {
            npcType = EntityType.fromName(argPack.getOption("type").toUpperCase());
        }

        if (argPack.getOption("type") != null && plugin.isValidNPCType(player, argPack.getOption("style").toUpperCase())) {
            style = Style.valueOf(argPack.getOption("style").toUpperCase());
        }

        if (argPack.size() != 1) {
            sender.sendMessage(ChatColor.RED + "Invalid format of arguments");
            return true;
        }

        String npcName = argPack.get(0);

        int owned = 0;

        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (npc.hasTrait(ShopTrait.class)) {
                if (npc.getTrait(Owner.class).getOwner().equalsIgnoreCase(player.getName())) {
                    owned += 1;
                }
            }
        }
        
        int traderLimit = plugin.getTraderLimit(player);
        if (traderLimit != -1 && traderLimit <= owned) {
            sender.sendMessage(ChatColor.RED + "Cannot spawn another trader NPC!");
            return true;
        }

        //, character);
        NPC npc = CitizensAPI.getNPCRegistry().createNPC(npcType, npcName);

        npc.getTrait(MobType.class).setType(npcType);
        npc.getTrait(Owner.class).setOwner(player.getName());

        npc.spawn(player.getLocation());
        
        Trader.setUpNPC(npc, style);

        return true;
    }

    private enum Subcommand {

        sellprice,
        buyprice,
        create,
        setwallet,
        wallet,
        fire,
        cancel,
        version,
        reloadprofiles,
        disable,
        enable,
        link,
        removelink,
        linkchest,
        unlinkchest,
        sellstack,
        buystack,
        list
    }
}
