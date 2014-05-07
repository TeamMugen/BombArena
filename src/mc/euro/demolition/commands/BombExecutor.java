package mc.euro.demolition.commands;

import mc.euro.demolition.BombPlugin;
import java.util.List;
import java.util.Map;
import mc.alk.arena.BattleArena;
import mc.alk.arena.controllers.ArenaEditor;
import mc.alk.arena.executors.CustomCommandExecutor;
import mc.alk.arena.executors.MCCommand;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.spawns.TimedSpawn;
import mc.alk.arena.serializers.ArenaSerializer;
import mc.alk.arena.util.SerializerUtil;
import mc.alk.tracker.objects.PlayerStat;
import mc.alk.tracker.objects.Stat;
import mc.alk.tracker.objects.StatType;
import mc.euro.demolition.BombArena;
import mc.euro.demolition.debug.DebugOff;
import mc.euro.demolition.debug.DebugOn;
import mc.euro.demolition.objects.BaseType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * All the /bomb commands and subcommands.
 * @author Nikolai
 */
public class BombExecutor extends CustomCommandExecutor {
    
    BombPlugin plugin;
    
    public BombExecutor() {
        plugin = (BombPlugin) Bukkit.getServer().getPluginManager().getPlugin("BombArena");
    }
    
    @MCCommand(cmds={"setbase"}, perm="bomb.setbase", usage="setbase <arena> <teamID>")
    public boolean setbase(Player sender, Arena arena, Integer i) {
        if (i < 1 || i > 2) {
            sender.sendMessage("Bomb arenas can only have 2 teams: 1 or 2");
            return true;
        }
        String a = arena.getName();
        Location loc = sender.getLocation();
        BombArena bombarena = (BombArena) BattleArena.getArena(arena.getName());
        Location base_loc = bombarena.getExactLocation(loc);
        if (base_loc == null) {
            sender.sendMessage("setbase command failed to find a BaseBlock near your location.");
            sender.sendMessage("Please set 2 BaseBlocks in the arena (1 for each team).");
            sender.sendMessage("If you have already set BaseBlocks, then stand closer and re-run the command.");
            return true;
        }
        String path = "arenas." + a + ".bases";
        String wxyz = SerializerUtil.getLocString(base_loc);
        plugin.arenasYml.set(path + "." + i, wxyz);
        plugin.arenasYml.saveConfig();

        // Set<String> keys = plugin.getConfig("arenas").getConfigurationSection(path).getKeys(false);
        // ArenaSerializer.saveArenas(plugin);
        sender.sendMessage("Base set! " + wxyz);
        String msg = "[BOMB] Player " + sender.getName()
                + " has set a base for arena (" + a + ") to " + wxyz;
        plugin.getLogger().info(msg);
        return true;
    }
    
    @MCCommand(cmds={"spawnbomb"}, perm="bomb.spawnbomb", usage="spawnbomb <arena>")
    public boolean spawnbomb(Player sender, Arena arena) {
        int despawn = arena.getParams().getMatchTime();
        plugin.debug.log("spawnbomb() despawn = MatchTime = " + despawn);
        // shortcut and alias for
        // /aa select ArenaName
        // /aa addspawn BombBlock.name() fs=1 rs=300 ds=1200 index=1 1
        plugin.getServer().dispatchCommand(sender, "aa select " + arena.getName());
        plugin.getServer().dispatchCommand(sender, 
                "aa addspawn " + plugin.getBombBlock().name() 
                + "1 fs=1 rs=300 ds=" + despawn + " index=1");
        ArenaSerializer.saveArenas(plugin);
        sender.sendMessage("The bomb spawn for " + arena.getName() + " has been set!");
        // Add to documentation:
        // sender.sendMessage("Because this command was an alias for /aa, "
        //        + "please do not use the /aa command without first using /aa select");
        return true;
    }
    
    @MCCommand(cmds={"stats"}, perm="bomb.stats", usage="stats")
    public boolean stats(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) {
            sender.sendMessage("Invalid command syntax: Please specify a player name");
            sender.sendMessage("./bomb stats <player>");
            sender.sendMessage("or /bomb stats top X");
            return true;
        }
        stats(sender, plugin.getServer().getOfflinePlayer(sender.getName()));
        return true;
    }
    
    @MCCommand(cmds={"stats"}, perm="bomb.stats.other", usage="stats <player>")
    public boolean stats(CommandSender sender, OfflinePlayer p) {
        if (!plugin.ti.isEnabled()) {
            plugin.getLogger().warning("BattleTracker not found or turned off.");
            sender.sendMessage("BombArena statistics are not being tracked.");
            return true;
        }
        PlayerStat ps = plugin.ti.tracker.getPlayerRecord(p);
        int wins = ps.getWins();
        int ties = ps.getTies();
        int losses = ps.getLosses();
        int total = wins + losses;
        int percentage = (total == 0) ? 0 : (int)  (wins * 100.00) / total;
        String intro = (sender.getName().equalsIgnoreCase(p.getName())) ?
                "You have" : p.getName() + " has";
        sender.sendMessage(intro + " successfully destroyed the other teams base "
                + wins + " times out of " + total + " attempts. ("
                + percentage +"%)");
        sender.sendMessage(intro + " defused the bomb " + ties + " times.");
        return true;
    }
    
    /**
     * Shows bomb arena stats for the command sender.
     * Example Usage: /bomb stats top 5
     */
    @MCCommand(cmds={"stats"}, subCmds={"top"}, perm="bomb.stats.top", usage="stats top X")
    public boolean stats(CommandSender cs, Integer n) {
            if (!plugin.ti.isEnabled()) {
                plugin.getLogger().warning(ChatColor.AQUA + "BattleTracker not found or turned off.");
                cs.sendMessage(ChatColor.YELLOW + "Bomb Arena statistics are not being tracked.");
                return true;
            }
            
            List<Stat> planted = plugin.ti.getTopXWins(n);
            cs.sendMessage(ChatColor.AQUA  +  "Number of Bombs Planted");
            cs.sendMessage(ChatColor.YELLOW + "-----------------------");
            int i = 1;
            for (Stat w : planted) {
            if (w.getName().equalsIgnoreCase(plugin.FakeName)) {
                continue;
            }
            int total = w.getWins() + w.getLosses();
            int percentage = (total == 0) ? 0 : (int) (w.getWins() * 100.00) / total;
            cs.sendMessage("" + i + " " + w.getName() + " " 
                    + w.getWins() + " out of " + total + " (" + percentage + "%)");
            i = i + 1;
        }
            
            List<Stat> defused = plugin.ti.getTopX(StatType.TIES, n);
            cs.sendMessage(ChatColor.AQUA + "Number of Bombs Defused");
            cs.sendMessage(ChatColor.YELLOW + "-----------------------");
            i = 1;
            for (Stat d : defused) {
                if (d.getName().equalsIgnoreCase(plugin.FakeName)) continue;
                cs.sendMessage("" + i + " " + d.getName() + " " + d.getTies());
                i = i + 1;
            }
            
            return true;
    }
    
    @MCCommand(cmds={"setconfig"}, subCmds={"bombblock"}, perm="bombarena.setconfig.bombblock", 
            usage="setconfig BombBlock <handItem>")
    public boolean setBombBlock(Player p) {
        ItemStack hand = p.getInventory().getItemInHand();
        if (hand == null) {
            p.sendMessage("There is nothing in your hand.");
            return false;
        }
        plugin.setBombBlock(hand.getType());
        p.sendMessage("BombBlock has been set to " + hand.getType());
        p.sendMessage("Now you need to update all the arenas with the new bomb type: ");
        p.sendMessage("(at your location): /bomb spawnbomb <arena>");
        
        return true;
    }

    @MCCommand(cmds={"setconfig"}, subCmds={"baseblock"}, perm="bombarena.setconfig.baseblock",
            usage="setconfig BaseBlock <handItem>")
    public boolean setBaseBlock(Player p) {
        ItemStack hand = p.getInventory().getItemInHand();
        if (hand == null) {
            p.sendMessage("There is nothing in your hand.");
            return false;
        }
        if (!BaseType.containsKey(hand.getType().name())) {
            p.sendMessage("That is not a valid BaseBlock in your hand!");
            return true;
        }
        p.sendMessage("BaseBlock has been set to " + hand.getType().name());
        plugin.setBaseBlock(hand.getType());
        return true;
    }    
    private void updateArenaYml(String x) {
        // PATH = "arenas.{arenaName}.spawns.{index}.spawn"
        ConfigurationSection arenas = plugin.arenasYml.getConfigurationSection("arenas");
        for (String arena : arenas.getKeys(false)) {
            ConfigurationSection spawns = plugin.arenasYml.getConfigurationSection("arenas." + arena);
            for (String n : spawns.getKeys(false)) {
                String path = "arenas." + arena + ".spawns." + n + ".spawn";
                String value = x.toUpperCase() + " 1";
                plugin.arenasYml.set(path, value);
            }
        }
    }
    
    @MCCommand(cmds={"setconfig"}, perm="bombarena.setconfig.integer", usage="setconfig <option> <integerValue>")
    public boolean setconfig(CommandSender sender, String option, Integer value) {
        plugin.getConfig().set(option, value);
        plugin.saveConfig();
        plugin.loadDefaultConfig();
        return true;
    }
    
    @MCCommand(cmds={"setconfig"}, subCmds={"databasetable"}, 
            perm="bombarena.setconfig.database", usage="setconfig DatabaseTable <name>")
    public boolean setDatabaseTable(CommandSender sender, String table) {
        plugin.setDatabaseTable(table);
        return true;
    }

    /**
     * Toggles debug mode ON / OFF.
     * Usage: /bomb debug
     */
    @MCCommand(cmds={"debug"}, perm="bombarena.debug", usage="debug")
    public boolean debug(CommandSender cs) {
        if (plugin.debug instanceof DebugOn) {
            plugin.debug = new DebugOff(plugin);
            cs.sendMessage("Debugging mode for the BombArena has been turned off.");
            return true;
        } else if (plugin.debug instanceof DebugOff) {
            plugin.debug = new DebugOn(plugin);
            cs.sendMessage("Debugging mode for the BombArena has been turned on.");
            return true;
        }
        return false;
    }
    
    @MCCommand(cmds={"getname"}, perm="bombarena.getname")
    public boolean setBreakTime(Player p) {
        String name = p.getItemInHand().getType().name();
        p.sendMessage("You are holding " + name);
        return true;
    }

    
}
