/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.tehbeard.cititrader.commands;

import me.tehbeard.cititrader.CitiTrader;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.command.Command;
import net.citizensnpcs.command.CommandContext;
import net.citizensnpcs.command.Requirements;
import net.citizensnpcs.util.Messaging;
import org.bukkit.command.CommandSender;

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
}
