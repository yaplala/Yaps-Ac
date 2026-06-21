package dk.zai.anticheat.commands;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.ColorUtil;
import dk.zai.anticheat.data.Punishment;
import dk.zai.anticheat.data.PunishmentType;
import dk.zai.anticheat.managers.LayoutsManager;
import dk.zai.anticheat.managers.PermissionManager;
import dk.zai.anticheat.managers.TimeParser;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Single dispatcher handling all AdvancedBan-style punishment commands.
 * The command name (label) selects the action; arguments are parsed
 * uniformly: [-s] <name> [duration] [reason/@Layout]
 */
public class PunishmentCommands implements CommandExecutor, TabCompleter {

    private final AntiCheatPlugin plugin;

    public PunishmentCommands(AntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();
        switch (cmd) {
            case "ban"        -> handleSimple(sender, args, PunishmentType.BAN, "ab.ban");
            case "ipban", "banip", "ban-ip" -> handleSimple(sender, args, PunishmentType.IP_BAN, "ab.ipban");
            case "mute"       -> handleSimple(sender, args, PunishmentType.MUTE, "ab.mute");
            case "kick"       -> handleSimple(sender, args, PunishmentType.KICK, "ab.kick");
            case "warn"       -> handleSimple(sender, args, PunishmentType.WARN, "ab.warn");
            case "note"       -> handleSimple(sender, args, PunishmentType.NOTE, "ab.note");
            case "tempban"    -> handleTemp(sender, args, PunishmentType.TEMP_BAN, "ab.tempban");
            case "tempipban", "tipban" -> handleTemp(sender, args, PunishmentType.TEMP_IP_BAN, "ab.tempipban");
            case "tempmute"   -> handleTemp(sender, args, PunishmentType.TEMP_MUTE, "ab.tempmute");
            case "tempwarn"   -> handleTemp(sender, args, PunishmentType.TEMP_WARN, "ab.tempwarn");
            case "unban"      -> handleUnban(sender, args);
            case "unmute"     -> handleUnmute(sender, args);
            case "unwarn"     -> handleUndo(sender, args, "Unwarn", "ab.unwarn");
            case "unnote"     -> handleUndo(sender, args, "Unnote", "ab.unnote");
            case "unpunish"   -> handleUndo(sender, args, "Unpunish", "ab.unpunish");
            case "change-reason" -> handleChangeReason(sender, args);
            case "banlist"    -> handleList(sender, args, "ban");
            case "history"    -> handleHistory(sender, args);
            case "warns"      -> handleWarns(sender, args);
            case "notes"      -> handleList(sender, args, "note");
            case "check"      -> handleCheck(sender, args);
            default -> sender.sendMessage("§cUnknown command: " + cmd);
        }
        return true;
    }

    // =====================================================================
    // Simple: ban, ipban, mute, kick, warn, note  (no duration)
    // =====================================================================

    private void handleSimple(CommandSender sender, String[] args, PunishmentType type, String perm) {
        PermissionManager pm = plugin.getPermissionManager();
        if (!pm.has(sender, perm)) { noPerms(sender); return; }

        boolean silent = false;
        List<String> list = new ArrayList<>(Arrays.asList(args));
        if (!list.isEmpty() && list.get(0).equalsIgnoreCase("-s")) {
            silent = true;
            list.remove(0);
        }
        if (list.isEmpty()) { usage(sender, type); return; }

        String name = list.get(0);
        String reason = list.size() > 1 ? String.join(" ", list.subList(1, list.size())) : null;
        if (reason == null) reason = plugin.getConfig().getString("punishment.default-reason", "none");

        String ip = "";
        if ((type == PunishmentType.IP_BAN) && name.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            ip = name; // raw IP given
        } else if (sender instanceof Player && name.equalsIgnoreCase(((Player) sender).getName())) {
            sender.sendMessage("§cYou cannot punish yourself.");
            return;
        }

        plugin.getPunishmentManager().issue(type, name, ip, -1L, reason, sender, silent);
    }

    // =====================================================================
    // Temp: tempban, tempipban, tempmute, tempwarn  (with duration)
    // =====================================================================

    private void handleTemp(CommandSender sender, String[] args, PunishmentType type, String perm) {
        PermissionManager pm = plugin.getPermissionManager();
        if (!pm.has(sender, perm)) { noPerms(sender); return; }

        boolean silent = false;
        List<String> list = new ArrayList<>(Arrays.asList(args));
        if (!list.isEmpty() && list.get(0).equalsIgnoreCase("-s")) {
            silent = true;
            list.remove(0);
        }
        if (list.size() < 2) { usage(sender, type); return; }

        String name = list.get(0);
        String durationStr = list.get(1);
        String reason = list.size() > 2 ? String.join(" ", list.subList(2, list.size())) : null;
        if (reason == null) reason = plugin.getConfig().getString("punishment.default-reason", "none");

        long durationMs = TimeParser.parseToMillis(durationStr);
        if (durationMs == 0) {
            sender.sendMessage("§cInvalid duration: " + durationStr
                    + " (use Xmo, Xd, Xh, Xm, Xs or 'perma')");
            return;
        }
        if (durationMs > 0) {
            long maxSec = pm.getMaxDuration(sender, type.name().toLowerCase().replace("temp_", "temp"));
            if (maxSec == 0) { noPerms(sender); return; }
            if (maxSec > 0 && durationMs / 1000L > maxSec) {
                sender.sendMessage(plugin.getLayoutsManager().renderList(List.of(
                        plugin.getLayoutsManager().getMessage(type.getConfigKey() + ".MaxDuration")
                                .replace("%MAX%", String.valueOf(maxSec)))));
                return;
            }
        }

        String ip = "";
        if (type == PunishmentType.TEMP_IP_BAN && name.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            ip = name;
        }
        plugin.getPunishmentManager().issue(type, name, ip, durationMs, reason, sender, silent);
    }

    // =====================================================================
    // Unban / Unmute
    // =====================================================================

    private void handleUnban(CommandSender sender, String[] args) {
        if (!plugin.getPermissionManager().has(sender, "ab.unban")) { noPerms(sender); return; }
        if (args.length < 1) { sender.sendMessage("§cUsage: /unban <name>"); return; }
        plugin.getPunishmentManager().revokeBan(args[0], sender);
    }

    private void handleUnmute(CommandSender sender, String[] args) {
        if (!plugin.getPermissionManager().has(sender, "ab.unmute")) { noPerms(sender); return; }
        if (args.length < 1) { sender.sendMessage("§cUsage: /unmute <name>"); return; }
        plugin.getPunishmentManager().revokeMute(args[0], sender);
    }

    // =====================================================================
    // Unwarn / Unnote / Unpunish (by ID)
    // =====================================================================

    private void handleUndo(CommandSender sender, String[] args, String type, String perm) {
        if (!plugin.getPermissionManager().has(sender, perm)) { noPerms(sender); return; }
        if (args.length < 1) { sender.sendMessage("§cUsage: /" + type.toLowerCase() + " <id>"); return; }
        try {
            long id = Long.parseLong(args[0]);
            plugin.getPunishmentManager().revokeById(id, sender);
        } catch (NumberFormatException ex) {
            sender.sendMessage("§cInvalid ID: " + args[0]);
        }
    }

    private void handleChangeReason(CommandSender sender, String[] args) {
        if (!plugin.getPermissionManager().has(sender, "ab.change-reason")) { noPerms(sender); return; }
        if (args.length < 2) { sender.sendMessage("§cUsage: /change-reason <id> <new reason>"); return; }
        try {
            long id = Long.parseLong(args[0]);
            String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            plugin.getPunishmentManager().changeReason(id, reason, sender);
        } catch (NumberFormatException ex) {
            sender.sendMessage("§cInvalid ID: " + args[0]);
        }
    }

    // =====================================================================
    // List views: banlist, notes, warns, history, check
    // =====================================================================

    private void handleList(CommandSender sender, String[] args, String kind) {
        String perm = "ban".equals(kind) ? "ab.banlist" : "ab.notes";
        if (!plugin.getPermissionManager().has(sender, perm)) { noPerms(sender); return; }

        int page = 1;
        if (args.length > 0) {
            try { page = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }

        List<Punishment> src;
        if ("ban".equals(kind)) {
            src = plugin.getDataManager().getActiveBans();
        } else {
            // Notes: requires a name arg
            if (args.length < 1) { sender.sendMessage("§cUsage: /notes <name> [page]"); return; }
            String name = args[0];
            UUID uuid = plugin.getDataManager().getCachedUuid(name);
            if (uuid == null) { OfflinePlayer op = Bukkit.getOfflinePlayer(name); uuid = op.getUniqueId(); }
            src = plugin.getDataManager().getNotes(uuid);
        }

        int perPage = 8;
        int pages = Math.max(1, (src.size() + perPage - 1) / perPage);
        page = Math.max(1, Math.min(page, pages));
        int from = (page - 1) * perPage;
        int to = Math.min(src.size(), from + perPage);

        LayoutsManager L = plugin.getLayoutsManager();
        String header = L.getMessage(kind.equals("ban") ? "Banlist.Header" : "Notes.Header")
                .replace("%PAGE%", String.valueOf(page))
                .replace("%PAGES%", String.valueOf(pages));
        sender.sendMessage(ColorUtil.toComponent(header));

        if (src.isEmpty()) {
            sender.sendMessage(ColorUtil.toComponent(L.getMessage(
                    kind.equals("ban") ? "Banlist.Empty" : "Notes.Empty")));
        } else {
            for (int i = from; i < to; i++) {
                Punishment p = src.get(i);
                String entry = L.getMessage(kind.equals("ban") ? "Banlist.Entry" : "Notes.Entry");
                sender.sendMessage(ColorUtil.toComponent(L.format(entry, p)));
            }
        }
        sender.sendMessage(ColorUtil.toComponent(L.getMessage(
                kind.equals("ban") ? "Banlist.Footer" : "Notes.Footer")));
    }

    private void handleHistory(CommandSender sender, String[] args) {
        if (!plugin.getPermissionManager().has(sender, "ab.history")) { noPerms(sender); return; }
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /history <name> [page]");
            return;
        }
        String name = args[0];
        UUID uuid = plugin.getDataManager().getCachedUuid(name);
        if (uuid == null) { OfflinePlayer op = Bukkit.getOfflinePlayer(name); uuid = op.getUniqueId(); }
        if (uuid == null) {
            sender.sendMessage(plugin.getLayoutsManager().renderList(List.of(
                    plugin.getLayoutsManager().getMessage("General.PlayerNotFound")
                            .replace("%NAME%", name))));
            return;
        }
        int page = args.length > 1 ? safeInt(args[1], 1) : 1;
        List<Punishment> hist = plugin.getDataManager().getHistory(uuid);
        paginate(sender, hist, page, "History", name);
    }

    private void handleWarns(CommandSender sender, String[] args) {
        // Self if no args
        String name;
        if (args.length == 0) {
            if (!(sender instanceof Player p)) { sender.sendMessage("§cUsage: /warns <name> [page]"); return; }
            name = p.getName();
        } else {
            name = args[0];
        }
        if (!plugin.getPermissionManager().canViewWarnsOf(sender, name)) { noPerms(sender); return; }
        UUID uuid = plugin.getDataManager().getCachedUuid(name);
        if (uuid == null) { OfflinePlayer op = Bukkit.getOfflinePlayer(name); uuid = op.getUniqueId(); }
        int page = args.length > 1 ? safeInt(args[1], 1) : 1;
        List<Punishment> warns = plugin.getDataManager().getActiveWarns(uuid);
        paginate(sender, warns, page, "Warns", name);
    }

    private void handleCheck(CommandSender sender, String[] args) {
        if (!plugin.getPermissionManager().has(sender, "ab.check")) { noPerms(sender); return; }
        if (args.length < 1) { sender.sendMessage("§cUsage: /check <name>"); return; }
        String name = args[0];
        UUID uuid = plugin.getDataManager().getCachedUuid(name);
        if (uuid == null) { OfflinePlayer op = Bukkit.getOfflinePlayer(name); uuid = op.getUniqueId(); }
        if (uuid == null) { sender.sendMessage("§cPlayer not found."); return; }

        LayoutsManager L = plugin.getLayoutsManager();
        sender.sendMessage(ColorUtil.toComponent(L.getMessage("Check.Header").replace("%NAME%", name)));

        Punishment ban = plugin.getDataManager().getActiveBan(uuid);
        Punishment mute = plugin.getDataManager().getActiveMute(uuid);
        String ip = plugin.getDataManager().get(uuid).getLastIp();

        String banLine = L.getMessage("Check.Ban")
                .replace("%BANNED%", ban == null ? "No" : "Yes")
                .replace("%REASON%", ban == null ? "-" : ban.getReason())
                .replace("%UNTIL%", ban == null ? "-" :
                        (ban.isPermanent() ? "Permanent" :
                                L.getDateFormat().format(new Date(ban.getEnd()))));
        sender.sendMessage(ColorUtil.toComponent(banLine));

        String muteLine = L.getMessage("Check.Mute")
                .replace("%MUTED%", mute == null ? "No" : "Yes")
                .replace("%REASON%", mute == null ? "-" : mute.getReason())
                .replace("%UNTIL%", mute == null ? "-" :
                        (mute.isPermanent() ? "Permanent" :
                                L.getDateFormat().format(new Date(mute.getEnd()))));
        sender.sendMessage(ColorUtil.toComponent(muteLine));

        String warnLine = L.getMessage("Check.WarnCount")
                .replace("%COUNT%", String.valueOf(plugin.getDataManager().getActiveWarns(uuid).size()));
        sender.sendMessage(ColorUtil.toComponent(warnLine));

        String noteLine = L.getMessage("Check.NoteCount")
                .replace("%COUNT%", String.valueOf(plugin.getDataManager().getNotes(uuid).size()));
        sender.sendMessage(ColorUtil.toComponent(noteLine));

        String ipLine = L.getMessage("Check.Ip").replace("%IP%", ip == null ? "-" : ip);
        sender.sendMessage(ColorUtil.toComponent(ipLine));

        sender.sendMessage(ColorUtil.toComponent(L.getMessage("Check.Footer")));
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private void paginate(CommandSender sender, List<Punishment> src, int page, String type, String name) {
        int perPage = 8;
        int pages = Math.max(1, (src.size() + perPage - 1) / perPage);
        page = Math.max(1, Math.min(page, pages));
        int from = (page - 1) * perPage;
        int to = Math.min(src.size(), from + perPage);

        LayoutsManager L = plugin.getLayoutsManager();
        String header = L.getMessage(type + ".Header")
                .replace("%NAME%", name)
                .replace("%PAGE%", String.valueOf(page))
                .replace("%PAGES%", String.valueOf(pages));
        sender.sendMessage(ColorUtil.toComponent(header));

        if (src.isEmpty()) {
            sender.sendMessage(ColorUtil.toComponent(L.getMessage(type + ".Empty")));
        } else {
            for (int i = from; i < to; i++) {
                Punishment p = src.get(i);
                String entry = L.getMessage(type + ".Entry")
                        .replace("%TYPE%", p.getType().basicName());
                sender.sendMessage(ColorUtil.toComponent(L.format(entry, p)));
            }
        }
        sender.sendMessage(ColorUtil.toComponent(L.getMessage(type + ".Footer")));
    }

    private void usage(CommandSender sender, PunishmentType type) {
        sender.sendMessage(plugin.getLayoutsManager().renderMessage(type.getConfigKey() + ".Usage"));
    }

    private void noPerms(CommandSender sender) {
        sender.sendMessage(plugin.getLayoutsManager().renderMessage("General.NoPerms"));
    }

    private int safeInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = command.getName().toLowerCase();
        List<String> out = new ArrayList<>();

        // Strip silent flag for tab handling
        int offset = (args.length > 0 && args[0].equalsIgnoreCase("-s")) ? 1 : 0;
        int effective = args.length - offset;

        if (cmd.equals("ban") || cmd.equals("mute") || cmd.equals("kick")
                || cmd.equals("warn") || cmd.equals("note")
                || cmd.equals("tempban") || cmd.equals("tempmute") || cmd.equals("tempwarn")
                || cmd.equals("ipban") || cmd.equals("tempipban") || cmd.equals("banip") || cmd.equals("tipban")) {
            if (effective == 0) {
                out.add("-s");
                for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
            } else if (effective == 1) {
                for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
            } else if (effective == 2 && cmd.startsWith("temp")) {
                out.addAll(List.of("30m", "1h", "6h", "1d", "7d", "1mo", "perma"));
            }
        } else if (cmd.equals("unban") || cmd.equals("unmute") || cmd.equals("check")
                || cmd.equals("history") || cmd.equals("warns") || cmd.equals("notes")) {
            if (effective == 0) {
                for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
            }
        }
        return out.stream()
                .filter(s -> args.length == 0 || s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
