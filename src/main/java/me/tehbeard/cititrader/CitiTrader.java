package me.tehbeard.cititrader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.Assert;
import me.tehbeard.cititrader.commands.CitiCommands;
import me.tehbeard.cititrader.commands.TraderCommands;
import me.tehbeard.cititrader.traits.LinkedChestTrait;
import me.tehbeard.cititrader.traits.ShopTrait;
import me.tehbeard.cititrader.traits.StockRoomTrait;
import me.tehbeard.cititrader.traits.TraderTrait;
import me.tehbeard.cititrader.traits.WalletTrait;
import net.citizensnpcs.Citizens;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.CitizensPlugin;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.command.CommandManager;
import net.citizensnpcs.command.CommandManager.CommandInfo;
import net.citizensnpcs.command.Injector;
import net.citizensnpcs.command.exception.CommandUsageException;
import net.citizensnpcs.command.exception.ServerCommandException;
import net.citizensnpcs.command.exception.UnhandledCommandException;
import net.citizensnpcs.command.exception.WrappedCommandException;
import net.citizensnpcs.util.Messages;
import net.citizensnpcs.util.Messaging;
import net.citizensnpcs.util.StringHelper;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
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

    private final CommandManager citicommands = new CommandManager();
    public static final String PERM_PREFIX = "traders";
    public static CitiTrader self;
    public static Economy economy;
    public static boolean outdated = false;
    public static boolean isTowny = false;
    private static CitizensPlugin citizens;
    public static Attributes atts;
    private FileConfiguration profiles = null;
    private File profilesFile = null;
    private FileConfiguration languages = null;
    private File languageFile = null;
    private CitiCommands commands;

    @Override
    public void onEnable() {
        setupConfig();
        reloadProfiles();
        reloadLanguage();

        setupTowny();

        self = this;

        if (setupEconomy()) {
            CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(ShopTrait.class).withName("shop"));
            CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(WalletTrait.class).withName("wallet"));
        } else {
            getLogger().severe(getLang().getString("error.noecon"));
        }

        CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(LinkedChestTrait.class).withName("linkedchest"));
        CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(StockRoomTrait.class).withName("stockroom"));
        CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(TraderTrait.class).withName("villagetrader"));


        registerCommands();

        commands = new CitiCommands(this);
        //getCommand("trader").setExecutor(commands);
        Bukkit.getPluginManager().registerEvents(new Trader(), this);

        try {
            this.getManifest();
        } catch (IOException ex) {
            Logger.getLogger(CitiTrader.class.getName()).log(Level.SEVERE, null, ex);
        }
        getLogger().log(Level.INFO, "v{0} loaded", getDescription().getVersion());
    }

    public CommandInfo getCommandInfo(String rootCommand, String modifier) {
        return citicommands.getCommand(rootCommand, modifier);
    }

    public Iterable<CommandInfo> getCommands(String base) {
        return citicommands.getCommands(base);
    }

    private void registerCommands() {
        citicommands.setInjector(new Injector(this));

        // Register command classes
        citicommands.register(TraderCommands.class);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String cmdName, String[] args) {
        try {
            // must put command into split.
            String[] split = new String[args.length + 1];
            System.arraycopy(args, 0, split, 1, args.length);
            split[0] = cmd.getName().toLowerCase();

            String modifier = args.length > 0 ? args[0] : "";

            if (!citicommands.hasCommand(split[0], modifier) && !modifier.isEmpty()) {
                return suggestClosestModifier(sender, split[0], modifier);
                //return true;
            }

            NPC npc =  ((Citizens)CitizensAPI.getPlugin()).getNPCSelector().getSelected(sender);
            // TODO: change the args supplied to a context style system for
            // flexibility (ie. adding more context in the future without
            // changing everything)
            try {
                citicommands.execute(split, sender, sender, npc);
            } catch (ServerCommandException ex) {
                Messaging.sendTr(sender, Messages.COMMAND_MUST_BE_INGAME);
            } catch (CommandUsageException ex) {
                Messaging.sendError(sender, ex.getMessage());
                Messaging.sendError(sender, ex.getUsage());
            } catch (WrappedCommandException ex) {
                throw ex.getCause();
            } catch (UnhandledCommandException ex) {
                return false;
            } catch (CommandException ex) {
                Messaging.sendError(sender, ex.getMessage());
            }
        } catch (NumberFormatException ex) {
            Messaging.sendErrorTr(sender, Messages.COMMAND_INVALID_NUMBER);
        } catch (Throwable ex) {
            ex.printStackTrace();
            if (sender instanceof Player) {
                Messaging.sendErrorTr(sender, Messages.COMMAND_REPORT_ERROR);
                Messaging.sendError(sender, ex.getClass().getName() + ": " + ex.getMessage());
            }
        }
        return true;
    }
    
    private boolean suggestClosestModifier(CommandSender sender, String command, String modifier) {
        int minDist = Integer.MAX_VALUE;
        String closest = "";
        for (String string : citicommands.getAllCommandModifiers(command)) {
            int distance = StringHelper.getLevenshteinDistance(modifier, string);
            if (minDist > distance) {
                minDist = distance;
                closest = string;
            }
        }
        if (!closest.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + Messaging.tr(Messages.UNKNOWN_COMMAND));
            sender.sendMessage(StringHelper.wrap(" /") + command + " " + StringHelper.wrap(closest));
            return true;
        }
        return false;
    }
    
    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }

    public enum Style {

        TRADER("trader"),
        VILLAGER("villagertrader");
        private String charName;

        private Style(String charName) {
            this.charName = charName;
        }

        public String getStyle() {
            return charName;
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
            if (npc.hasTrait(LinkedChestTrait.class)) {
            //if (npc.hasTrait(ShopTrait.class)) {
                if (npc.getTrait(LinkedChestTrait.class).hasLinkedChest()) {
                    if (npc.getTrait(LinkedChestTrait.class).getLinkedChests().containsKey(loc)) {
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

    public void setupTowny() {
        if (Bukkit.getPluginManager().getPlugin("Towny") != null) {
            if (getServer().getPluginManager().getPlugin("Towny").isEnabled() == true) {
                CitiTrader.isTowny = true;
            }
        }
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

    public void reloadLanguage() {
        languageFile = new File(this.getDataFolder() + File.separator + "lang", getConfig().getString("language") + ".yml");
        languages = YamlConfiguration.loadConfiguration(languageFile);

        // Look for defaults in the jar
        InputStream defConfigStream = this.getResource("en.yml");

        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            languages.setDefaults(defConfig);
            languages.options().copyDefaults(true);
        }
        this.saveLanguage();
    }

    public FileConfiguration getLang() {
        if (languages == null) {
            this.reloadLanguage();
        }
        return languages;
    }

    public void saveLanguage() {
        if (languages == null || languageFile == null) {
            return;
        }
        try {
            getProfiles().save(languageFile);
        } catch (IOException ex) {
            this.getLogger().log(Level.SEVERE, "Could not save config to " + languageFile, ex);
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
            String warning = String.format("%-9s %-12s %23s", "|", this.getDescription().getVersion(), "|");
            String newversion = String.format("%-9s %-12s %23s", "|", returnString, "|");
            getLogger().warning("*--------------------------------------------*");
            getLogger().warning("|    CitiTraders                             |");
            getLogger().warning("|      Version is outofdate:                 |");
            getLogger().warning(warning);
            getLogger().warning("|      New Version is:                       |");
            getLogger().warning(newversion);
            getLogger().warning("*--------------------------------------------*");
            outdated = true;
        }
    }

    public boolean isMayorOrAssistant(Player player) {
        if (!CitiTrader.isTowny) {
            return false;
        }

        com.palmergames.bukkit.towny.object.Resident resident;


        try {
            resident = com.palmergames.bukkit.towny.object.TownyUniverse.getDataSource().getResident(player.getName());
        } catch (Exception ex) {
            Logger.getLogger(CitiTrader.class
                    .getName()).log(Level.SEVERE, null, ex);

            return false;
        }

        if (resident.hasTown()) {
            try {
                com.palmergames.bukkit.towny.object.Town town = resident.getTown();
                if (resident.isMayor() || town.getAssistants().contains(resident)) {
                    return true;
                }
            } catch (Exception ex) {
                Logger.getLogger(WalletTrait.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }
        return false;
    }

    public String getTownBank(Player player) {
        if (!CitiTrader.isTowny) {
            return null;
        }

        if (!isMayorOrAssistant(player)) {
            return null;
        }

        com.palmergames.bukkit.towny.object.Resident resident;

        try {
            resident = com.palmergames.bukkit.towny.object.TownyUniverse.getDataSource().getResident(player.getName());
        } catch (Exception ex) {
            Logger.getLogger(CitiTrader.class
                    .getName()).log(Level.SEVERE, null, ex);

            return null;
        }
        try {
            return resident.getTown().getEconomyName();
        } catch (Exception ex) {
            Logger.getLogger(CitiTrader.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }
}
