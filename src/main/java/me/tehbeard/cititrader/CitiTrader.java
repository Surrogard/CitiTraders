package me.tehbeard.cititrader;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.Assert;
import me.tehbeard.cititrader.TraderStatus.Status;
import me.tehbeard.cititrader.WalletTrait.WalletType;
import me.tehbeard.cititrader.utils.ArgumentPack;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.CitizensPlugin;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.api.trait.trait.MobType;
import net.citizensnpcs.api.trait.trait.Owner;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Provides a trader for
 *
 * @author James
 *
 */
public class CitiTrader extends JavaPlugin {

    public static final String PERM_PREFIX = "traders";
    public static CitiTrader self;
    public static Economy economy;
    public static boolean outdated = false;
    public static Towny towny;
    public static boolean isTowny = false;
    private static CitizensPlugin citizens;
    private static Attributes atts;
    private FileConfiguration profiles = null;
    private File profilesFile = null;

    @Override
    public void onEnable() {
        setupConfig();
        this.reloadProfiles();

        setupTowny();

        if (setupEconomy()) {
            self = this;
            citizens = (CitizensPlugin) Bukkit.getPluginManager().getPlugin("Citizens");
            citizens.getTraitFactory().registerTrait(TraitInfo.create(StockRoomTrait.class).withName("stockroom"));
            citizens.getTraitFactory().registerTrait(TraitInfo.create(WalletTrait.class).withName("wallet"));
            //citizens.getCharacterManager().registerCharacter(new CharacterFactory(Trader.class).withName("trader"));

            Bukkit.getPluginManager().registerEvents(new Trader(), this);
        } else {
            getLogger().severe("COULD NOT FIND AN ECONOMY PLUGIN");
        }

        try {
            this.getManifest();
        } catch (IOException ex) {
            Logger.getLogger(CitiTrader.class.getName()).log(Level.SEVERE, null, ex);
        }
        getLogger().log(Level.INFO, "v{0} loaded", getDescription().getVersion());
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
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
        if (!sender.hasPermission(PERM_PREFIX + ".command." + args[0])) {
            return false;
        }
        Subcommand subCom;
        try {
            subCom = Subcommand.valueOf(args[0]);
        } catch (Exception e) {
            return false;
        }
        switch (subCom) {
            case create: {
                ArgumentPack argPack = new ArgumentPack(new String[0], new String[]{"type", "style"}, compact(args, 1));
                EntityType npcType = EntityType.PLAYER;
                if (argPack.getOption("type") != null && isValidNPCType(player, argPack.getOption("type").toUpperCase())) {
                    npcType = EntityType.fromName(argPack.getOption("type").toUpperCase());
                }

                //TODO: UNCOMMENT WHEN 1.3 COMES OUT
            /*if(argPack.getOption("type")!=null && isValidNPCType(player,argPack.getOption("style").toUpperCase())){
                 character = Style.valueOf(argPack.getOption("style").toUpperCase()).getCharacter();
                 }*/
                if (argPack.size() != 1) {
                    sender.sendMessage(ChatColor.RED + "Invalid format of arguments");
                }

                String npcName = argPack.get(0);

                int owned = 0;

                for (NPC npc : citizens.getNPCRegistry()) {
                    if (npc.hasTrait(StockRoomTrait.class)) {
                        if (npc.getTrait(Owner.class).getOwner().equalsIgnoreCase(player.getName())) {

                            owned += 1;
                        }
                    }
                }
                int traderLimit = getTraderLimit(player);
                if (traderLimit != -1 && traderLimit <= owned) {
                    sender.sendMessage(ChatColor.RED + "Cannot spawn another trader NPC!");
                    return true;
                }



                //, character);
                NPC npc = CitizensAPI.getNPCRegistry().createNPC(npcType, npcName);

                npc.getTrait(MobType.class).setType(npcType);
                npc.getTrait(Owner.class).setOwner(player.getName());

                npc.spawn(player.getLocation());
                Trader.setUpNPC(npc);

                return true;

            }
            case sellprice: {
                if (args.length == 2) {
                    TraderStatus state = Trader.getStatus(((Player) sender).getName());
                    if (state.getStatus().equals(Status.SET_PRICE_BUY)) {
                        sender.sendMessage(ChatColor.YELLOW + "Please finish setting your buy price first");
                        sender.sendMessage(ChatColor.YELLOW + "Or cancel with /trader cancel");
                        return true;
                    }
                    state.setStatus(Status.SET_PRICE_SELL);

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
                    if (state.getStatus().equals(Status.SET_PRICE_SELL)) {
                        sender.sendMessage(ChatColor.YELLOW + "Please finish setting your sell price first");
                        sender.sendMessage(ChatColor.YELLOW + "Or cancel with /trader cancel");
                        return true;
                    }
                    state.setStatus(Status.SET_PRICE_BUY);

                    double price;
                    if (args[1].equalsIgnoreCase("remove")) {
                        price = -1;
                    } else {
                        price = Double.parseDouble(args[1]);
                    }
                    state.setMoney(price);
                    sender.sendMessage(ChatColor.DARK_PURPLE + "Now right click with item to finish.");
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
                WalletType type = WalletType.valueOf(args[1].toUpperCase());
                if (type == null) {
                    sender.sendMessage(ChatColor.RED + "Invalid Wallet Type!");
                    return true;
                }

                if (type == WalletType.BANK && args.length != 3) {
                    sender.sendMessage(ChatColor.RED + "An account name is needed for this type of wallet");
                    return true;
                } else {
                    String an = "";
                    if (args.length > 2) {
                        an = args[2];
                    }
                    state.setAccName(an);
                }


                if (type.equals(WalletType.TOWN_BANK)) {
                    if (CitiTrader.isTowny) {
                        Hashtable<String, Resident> table = CitiTrader.towny.getTownyUniverse().getResidentMap();
                        if (table.containsKey(sender.getName())) {
                            Resident res = table.get(sender.getName());
                            if (res.hasTown()) {
                                Town town = null;
                                try {
                                    town = res.getTown();
                                } catch (Exception ex) {
                                    Logger.getLogger(WalletTrait.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                if (town != null) {
                                    if (res.isMayor() || town.getAssistants().contains(res)) {
                                        //sender.sendMessage(town.getEconomyName());
                                        state.setAccName(town.getEconomyName());
                                    } else {
                                        sender.sendMessage(ChatColor.RED + "You are not the mayor or assistant of this town.");
                                        return true;
                                    }
                                }
                            } else {
                                sender.sendMessage(ChatColor.RED + "You are not a resident of any town.");
                                return true;
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "You are not a resident of any town.");
                            return true;
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Towny is not enabled on this server.");
                        return true;
                    }
                }

                if (!type.hasPermission(sender)) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this wallet type!");
                    return true;
                }

                state.setStatus(Status.SET_WALLET);
                state.setWalletType(type);
                sender.sendMessage(ChatColor.DARK_PURPLE + "Right click trader to set his wallet!");

                return true;
            }

            case wallet: {
                
                if(args[1].equalsIgnoreCase("balance") && args.length == 2) {
                    TraderStatus status = Trader.getStatus(player.getName());
                    status.setStatus(Status.BALANCE_MONEY);
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
                    status.setStatus(Status.GIVE_MONEY);
                    status.setMoney(Double.parseDouble(args[2]));
                    player.sendMessage(ChatColor.DARK_PURPLE + "Right click the Trader you would like to give money too.");
                }

                if (args[1].equalsIgnoreCase("take")) {
                    TraderStatus status = Trader.getStatus(player.getName());
                    status.setStatus(Status.TAKE_MONEY);
                    status.setMoney(Double.parseDouble(args[2]));
                    player.sendMessage(ChatColor.DARK_PURPLE + "Right click the Trader you would like to take money from.");
                }
                
                
                return true;
            }
            case fire: {
                TraderStatus status = Trader.getStatus(player.getName());
                status.setStatus(Status.FIRING);
                player.sendMessage(ChatColor.DARK_PURPLE + "Right click the Trader you want to fire.");
                return true;
            }
            case cancel: {
                Trader.clearStatus(player.getName());
                player.sendMessage(ChatColor.GREEN + "Status reset.");
                return true;
            }
            case version: {
                player.sendMessage("Running Cititraders version: " + getDescription().getVersion());
                player.sendMessage("With build number: " + atts.getValue("Build-Tag"));
                return true;
            }
            case reloadprofiles: {
                this.reloadProfiles();
                return true;
            }
            case disable: {
                TraderStatus status = Trader.getStatus(player.getName());
                player.sendMessage(ChatColor.DARK_PURPLE + "Right click the Trader you want to disable.");
                status.setStatus(Status.DISABLE);
                return true;
            }
            case enable: {
                TraderStatus status = Trader.getStatus(player.getName());
                player.sendMessage(ChatColor.DARK_PURPLE + "Right click the Trader you want to enable.");
                status.setStatus(Status.ENABLE);
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
                status.setStatus(Status.SET_LINK);
                return true;
            }
            case removelink: {
                TraderStatus status = Trader.getStatus(player.getName());
                player.sendMessage(ChatColor.DARK_PURPLE + "Right click the Trader you want to remove the link from.");
                status.setStatus(Status.REMOVE_LINK);
                return true;
            }
            case linkchest: {
                TraderStatus status = Trader.getStatus(player.getName());
                player.sendMessage(ChatColor.DARK_PURPLE + "Right click the Chest you want to link.");
                status.setStatus(Status.SELECT_LINK_CHEST);
                return true;
            }
            case unlinkchest: {
                TraderStatus status = Trader.getStatus(player.getName());
                player.sendMessage(ChatColor.DARK_PURPLE + "Right click the Chest you want to unlink.");
                status.setStatus(Status.SELECT_UNLINK_CHEST);
                return true;
            }
            case sellstack: {
                if(args.length < 2) {
                    player.sendMessage(ChatColor.RED + "You need to input an amount /trader sellstack 16");
                    return true;
                }
                if(args.length > 2) {
                    player.sendMessage(ChatColor.RED + "You have to many arguements /trader sellstack 16");
                }
                TraderStatus status = Trader.getStatus(player.getName());
                player.sendMessage(ChatColor.DARK_PURPLE + "Right click the Trader with the item you want to set.");
                status.setStackAmount(Integer.parseInt(args[1]));
                status.setStatus(Status.SET_SELL_STACK);
                return true;                
            }
            case buystack: {

            }
            case list: {
                if(args.length != 2) {
                    player.sendMessage(ChatColor.RED + "Command syntax is /trader list <buy|sell>");
                    return true;
                }
                
                if(!args[1].equalsIgnoreCase("buy") && !args[1].equalsIgnoreCase("sell")) {
                    player.sendMessage(ChatColor.RED + "Command syntax is /trader list <buy|sell>");
                    return true;
                }
                
                TraderStatus status = Trader.getStatus(player.getName());
                player.sendMessage(ChatColor.DARK_PURPLE + "Right click a Trader to see their price list.");
                if(args[1].equalsIgnoreCase("buy")) {
                    status.setStatus(Status.LIST_BUY_PRICE);
                } else {
                    status.setStatus(Status.LIST_SELL_PRICE);
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

    private enum Style {

        trader("trader"),
        villager("villagertrader");
        private String charName;

        private Style(String charName) {
            this.charName = charName;
        }
    }

    public boolean isValidNPCType(Player player, String type) {
        return getConfig().getStringList("trader-types").contains(type);
    }

    public boolean isValidTraderStyle(Player player) {
        return true;//TODO: Proper checks when 1.3 hits
    }

    public int getTraderLimit(Player player) {
        int limit = getProfiles().getInt("profiles.default.trader-limit", 1);
        for (String s : getProfiles().getConfigurationSection("profiles").getKeys(false)) {
            if (s.equals("default")) {
                continue;
            }
            if (player.hasPermission(PERM_PREFIX + ".profile." + s)) {
                limit = Math.max(getProfiles().getInt("profiles." + s + ".trader-limit"), limit);
            }

        }

        return limit;
    }

    public int getChestLimit(Player player) {
        int limit = getProfiles().getInt("profiles.default.chest-limit", 1);
        for (String s : getProfiles().getConfigurationSection("profiles").getKeys(false)) {
            if (s.equals("default")) {
                continue;
            }
            if (player.hasPermission(PERM_PREFIX + ".profile." + s)) {
                limit = Math.max(getProfiles().getInt("profiles." + s + ".chest-limit"), limit);
            }

        }

        return limit;
    }

    public NPC isChestLinked(Location loc) {
        for (NPC npc : citizens.getNPCRegistry()) {
            if (npc.hasTrait(StockRoomTrait.class)) {
                if (npc.getTrait(StockRoomTrait.class).hasLinkedChest()) {
                    if (npc.getTrait(StockRoomTrait.class).linkedChests.containsKey(loc)) {
                        return npc;
                    }
                }
            }
        }
        
        return null;
    }

    public void getManifest() throws IOException {
        URL res = Assert.class.getResource(Assert.class.getSimpleName() + ".class");
        JarURLConnection conn = (JarURLConnection) res.openConnection();
        Manifest mf = conn.getManifest();
        //JarInputStream jarStream = new JarInputStream(this.getResource("CitiTrader.class"));
        //Manifest mf = jarStream.getManifest();
        atts = mf.getMainAttributes();
    }

    public boolean setupTowny() {
        try {
            if (Bukkit.getPluginManager().getPlugin("Towny") != null) {
                towny = (Towny) Bukkit.getPluginManager().getPlugin("Towny");
                CitiTrader.isTowny = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public void setupConfig() {

        getConfig();
        getConfig().options().copyDefaults(true);
        this.saveConfig();
    }

    public void reloadProfiles() {
        profilesFile = new File(this.getDataFolder(), "profiles.yml");
        profiles = YamlConfiguration.loadConfiguration(profilesFile);

        // Look for defaults in the jar
        InputStream defConfigStream = this.getResource("profiles.yml");

        if (defConfigStream != null && !profilesFile.exists()) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            profiles.setDefaults(defConfig);
            profiles.options().copyDefaults(true);
        }
        this.saveProfiles();
    }

    public FileConfiguration getProfiles() {
        if (profiles == null) {
            this.reloadProfiles();
        }
        return profiles;
    }

    public void saveProfiles() {
        if (profiles == null || profilesFile == null) {
            return;
        }
        try {
            getProfiles().save(profilesFile);
        } catch (IOException ex) {
            this.getLogger().log(Level.SEVERE, "Could not save config to " + profilesFile, ex);
        }
    }

    public void checkVersion() {
        InputStream is = null;
        String returnString = "";
        try {
            URL url = new URL("http://thedemgel.com/files/public-docs/CitiTraders/version.txt");
            is = url.openStream();
            Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
            if (scanner.hasNext()) {
                returnString = scanner.next();
            }
        } catch (IOException ex) {
            Logger.getLogger(CitiTrader.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                Logger.getLogger(CitiTrader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        returnString = returnString.trim();
        if (!returnString.equals(this.getDescription().getVersion())) {
            String warning = String.format("%-9s %-12s %32s", "|", this.getDescription().getVersion(), "|");
            String newversion = String.format("%-9s %-12s %32s", "|", returnString, "|");
            getLogger().warning("*-----------------------------------------------------*");
            getLogger().warning("|    CitiTraders                                      |");
            getLogger().warning("|      Version is outofdate:                          |");
            getLogger().warning(warning);
            getLogger().warning("|      New Version is:                                |");
            getLogger().warning(newversion);
            getLogger().warning("*-----------------------------------------------------*");
            outdated = true;
        }
    }
}
