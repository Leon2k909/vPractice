package pw.vera.vpractice.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pw.vera.vpractice.arena.Arena;
import pw.vera.vpractice.vPractice;

/**
 * /arena <create|setspawna|setspawnb|setmin|setmax|list|delete> - Arena management (admin)
 */
public class ArenaCommand implements CommandExecutor {

    private final vPractice plugin;

    public ArenaCommand(vPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cOnly players can use this command!"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("vpractice.admin")) {
            player.sendMessage(color("&cYou don't have permission to use this command!"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create":
                if (args.length < 2) {
                    player.sendMessage(color("&cUsage: /arena create <name>"));
                    return true;
                }
                handleCreate(player, args[1]);
                break;

            case "setspawna":
            case "seta":
                if (args.length < 2) {
                    player.sendMessage(color("&cUsage: /arena setspawna <name>"));
                    return true;
                }
                handleSetSpawnA(player, args[1]);
                break;

            case "setspawnb":
            case "setb":
                if (args.length < 2) {
                    player.sendMessage(color("&cUsage: /arena setspawnb <name>"));
                    return true;
                }
                handleSetSpawnB(player, args[1]);
                break;

            case "setmin":
                if (args.length < 2) {
                    player.sendMessage(color("&cUsage: /arena setmin <name>"));
                    return true;
                }
                handleSetMin(player, args[1]);
                break;

            case "setmax":
                if (args.length < 2) {
                    player.sendMessage(color("&cUsage: /arena setmax <name>"));
                    return true;
                }
                handleSetMax(player, args[1]);
                break;

            case "list":
                handleList(player);
                break;

            case "delete":
            case "remove":
                if (args.length < 2) {
                    player.sendMessage(color("&cUsage: /arena delete <name>"));
                    return true;
                }
                handleDelete(player, args[1]);
                break;

            case "tp":
            case "teleport":
                if (args.length < 2) {
                    player.sendMessage(color("&cUsage: /arena tp <name>"));
                    return true;
                }
                handleTeleport(player, args[1]);
                break;

            default:
                sendHelp(player);
        }

        return true;
    }

    private void handleCreate(Player player, String name) {
        if (plugin.getArenaManager().getArena(name) != null) {
            player.sendMessage(color("&cAn arena with that name already exists!"));
            return;
        }

        Arena arena = new Arena(name);
        arena.setSpawnA(player.getLocation());
        plugin.getArenaManager().addArena(arena);
        
        player.sendMessage(color("&aArena &e" + name + " &acreated!"));
        player.sendMessage(color("&7Now set spawn B with &e/arena setspawnb " + name));
    }

    private void handleSetSpawnA(Player player, String name) {
        Arena arena = plugin.getArenaManager().getArena(name);
        if (arena == null) {
            player.sendMessage(color("&cArena not found!"));
            return;
        }

        arena.setSpawnA(player.getLocation());
        player.sendMessage(color("&aSpawn A set for arena &e" + name + "&a!"));
    }

    private void handleSetSpawnB(Player player, String name) {
        Arena arena = plugin.getArenaManager().getArena(name);
        if (arena == null) {
            player.sendMessage(color("&cArena not found!"));
            return;
        }

        arena.setSpawnB(player.getLocation());
        player.sendMessage(color("&aSpawn B set for arena &e" + name + "&a!"));
    }

    private void handleSetMin(Player player, String name) {
        Arena arena = plugin.getArenaManager().getArena(name);
        if (arena == null) {
            player.sendMessage(color("&cArena not found!"));
            return;
        }

        arena.setMin(player.getLocation());
        player.sendMessage(color("&aMin corner set for arena &e" + name + "&a!"));
    }

    private void handleSetMax(Player player, String name) {
        Arena arena = plugin.getArenaManager().getArena(name);
        if (arena == null) {
            player.sendMessage(color("&cArena not found!"));
            return;
        }

        arena.setMax(player.getLocation());
        player.sendMessage(color("&aMax corner set for arena &e" + name + "&a!"));
    }

    private void handleList(Player player) {
        player.sendMessage(color("&7&m--------------------------"));
        player.sendMessage(color("&6&lArenas"));
        player.sendMessage(color("&7&m--------------------------"));

        for (Arena arena : plugin.getArenaManager().getAllArenas()) {
            String status = arena.isSetup() ? "&a✔" : "&c✘";
            String inUse = arena.isInUse() ? " &7(In Use)" : "";
            player.sendMessage(color(status + " &e" + arena.getName() + inUse));
        }

        player.sendMessage(color("&7&m--------------------------"));
    }

    private void handleDelete(Player player, String name) {
        Arena arena = plugin.getArenaManager().getArena(name);
        if (arena == null) {
            player.sendMessage(color("&cArena not found!"));
            return;
        }

        if (arena.isInUse()) {
            player.sendMessage(color("&cCannot delete arena while it's in use!"));
            return;
        }

        plugin.getArenaManager().removeArena(name);
        player.sendMessage(color("&cArena &e" + name + " &cdeleted!"));
    }

    private void handleTeleport(Player player, String name) {
        Arena arena = plugin.getArenaManager().getArena(name);
        if (arena == null) {
            player.sendMessage(color("&cArena not found!"));
            return;
        }

        if (arena.getSpawnA() != null) {
            player.teleport(arena.getSpawnA());
            player.sendMessage(color("&aTeleported to arena &e" + name + "&a!"));
        } else {
            player.sendMessage(color("&cArena spawn not set!"));
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(color("&7&m--------------------------"));
        player.sendMessage(color("&6&lArena Commands"));
        player.sendMessage(color("&7&m--------------------------"));
        player.sendMessage(color("&e/arena create <name> &7- Create an arena"));
        player.sendMessage(color("&e/arena setspawna <name> &7- Set spawn A"));
        player.sendMessage(color("&e/arena setspawnb <name> &7- Set spawn B"));
        player.sendMessage(color("&e/arena setmin <name> &7- Set min corner"));
        player.sendMessage(color("&e/arena setmax <name> &7- Set max corner"));
        player.sendMessage(color("&e/arena list &7- List all arenas"));
        player.sendMessage(color("&e/arena tp <name> &7- Teleport to arena"));
        player.sendMessage(color("&e/arena delete <name> &7- Delete an arena"));
        player.sendMessage(color("&7&m--------------------------"));
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
