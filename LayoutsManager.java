package dk.zai.anticheat.commands;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.ColorUtil;
import dk.zai.anticheat.data.PlayerData;
import dk.zai.anticheat.data.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AntiCheatCommand implements CommandExecutor, TabCompleter {

    private final AntiCheatPlugin plugin;

    public AntiCheatCommand(AntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("yaps.admin")) {
            sender.sendMessage(ColorUtil.toComponent(plugin.getConfig().getString("messages.prefix", "")
                    + plugin.getConfig().getString("messages.no-permission", "&cNo permission.")));
            return true;
        }
        if (args.length == 0) {
            // If sender is a player, open the GUI
            if (sender instanceof Player p) {
                if (p.hasPermission("yaps.gui") || p.hasPermission("yaps.admin")) {
                    plugin.getGui().openMain(p);
                    return true;
                }
            }
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadAll();
                sender.sendMessage(ColorUtil.toComponent(plugin.getConfig().getString("messages.prefix", "")
                        + plugin.getConfig().getString("messages.reload-success", "&aReloaded.")));
            }
            case "reset" -> {
                if (args.length < 2) { sender.sendMessage("§cUsage: /" + label + " reset <player>"); return true; }
                OfflinePlayer t = Bukkit.getOfflinePlayer(args[1]);
                plugin.getDataManager().resetPlayerVL(t.getUniqueId());
                sender.sendMessage(ColorUtil.toComponent(plugin.getConfig().getString("messages.prefix", "")
                        + plugin.getConfig().getString("messages.reset-success", "&aReset %player%.")
                        .replace("%player%", args[1])));
            }
            case "status" -> {
                if (args.length < 2) { sender.sendMessage("§cUsage: /" + label + " status <player>"); return true; }
                PlayerData pd = plugin.getDataManager().getByName(args[1]);
                if (pd == null) { notFound(sender, args[1]); return true; }
                sendStatus(sender, args[1], pd);
            }
            case "checks" -> sendChecks(sender);
            case "gui" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("§cGUI can only be opened by a player.");
                    return true;
                }
                if (!p.hasPermission("yaps.gui") && !p.hasPermission("yaps.admin")) {
                    sender.sendMessage(ColorUtil.toComponent(plugin.getConfig().getString("messages.prefix", "")
                            + plugin.getConfig().getString("messages.no-permission", "&cNo permission.")));
                    return true;
                }
                plugin.getGui().openMain(p);
            }
            default -> sendHelp(sender, label);
        }
        return true;
    }

    private void sendHelp(CommandSender s, String label) {
        s.sendMessage("§8§m---------- §e§lYAPS §6§lAntiCheat §8§m----------");
        s.sendMessage("§e/" + label + " §7- Open the admin GUI §8(no args)");
        s.sendMessage("§e/" + label + " gui §7- Open the admin GUI");
        s.sendMessage("§e/" + label + " reload §7- Reload config");
        s.sendMessage("§e/" + label + " reset <player> §7- Reset player VL");
        s.sendMessage("§e/" + label + " status <player> §7- Show VL/mute/ban");
        s.sendMessage("§e/" + label + " checks §7- List all anti-cheat checks");
    }

    private void sendStatus(CommandSender s, String name, PlayerData pd) {
        s.sendMessage(ColorUtil.toComponent(plugin.getConfig().getString("messages.status-header",
                "&8--- §eYAPS§7: %player% ---").replace("%player%", name)));
        Punishment ban = plugin.getDataManager().getActiveBan(pd.getUuid());
        Punishment mute = plugin.getDataManager().getActiveMute(pd.getUuid());
        s.sendMessage(ColorUtil.toComponent(plugin.getConfig().getString("messages.status-line",
                        "&7VL: &f%vl% &7| Muted: &f%muted% &7| Banned: &f%banned%")
                .replace("%vl%", String.valueOf(pd.getViolationLevel()))
                .replace("%muted%", String.valueOf(mute != null))
                .replace("%banned%", String.valueOf(ban != null))));
    }

    private void sendChecks(CommandSender s) {
        s.sendMessage("§8§m---------- §e§lYAPS §7Checks §8§m----------");
        for (var check : plugin.getChecks().all()) {
            s.sendMessage("§e" + check.getName() + " §7- " + (check.isEnabled() ? "§aenabled" : "§cdisabled"));
        }
    }

    private void notFound(CommandSender s, String name) {
        s.sendMessage(ColorUtil.toComponent(plugin.getConfig().getString("messages.prefix", "")
                + plugin.getConfig().getString("messages.player-not-found", "&cPlayer not found: %player%")
                .replace("%player%", name)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return Arrays.asList("reload", "reset", "status", "checks", "gui");
        if (args.length == 2 && !args[0].equalsIgnoreCase("reload")
                && !args[0].equalsIgnoreCase("checks") && !args[0].equalsIgnoreCase("gui")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return names;
        }
        return new ArrayList<>();
    }
}
