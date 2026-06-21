package dk.zai.anticheat.listeners;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.ColorUtil;
import dk.zai.anticheat.data.Punishment;
import dk.zai.anticheat.data.PunishmentType;
import dk.zai.anticheat.managers.LayoutsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

/**
 * Blocks chat AND configured commands for muted players. Persists across
 * relogs because mutes are stored as Punishment records in data.yml.
 */
public class ChatListener implements Listener {

    private final AntiCheatPlugin plugin;

    public ChatListener(AntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        var player = event.getPlayer();
        Punishment mute = plugin.getDataManager().getActiveMute(player.getUniqueId());
        if (mute != null) {
            event.setCancelled(true);
            LayoutsManager L = plugin.getLayoutsManager();
            String path = mute.getType() == PunishmentType.MUTE ? "Mute.Blocked" : "Tempmute.Blocked";
            String msg = L.format(L.getMessage(path), mute);
            player.sendMessage(ColorUtil.toComponent(L.getPrefix() + " " + msg));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        var player = event.getPlayer();
        Punishment mute = plugin.getDataManager().getActiveMute(player.getUniqueId());
        if (mute == null) return;

        String raw = event.getMessage();
        if (raw.startsWith("/")) raw = raw.substring(1);
        // Strip namespace prefix like "minecraft:me"
        if (raw.contains(":")) raw = raw.substring(raw.indexOf(':') + 1);
        String base = raw.split(" ")[0].toLowerCase();

        List<String> blocked = plugin.getConfig().getStringList("punishment.mute-commands");
        for (String b : blocked) {
            if (b.equalsIgnoreCase(base)) {
                event.setCancelled(true);
                LayoutsManager L = plugin.getLayoutsManager();
                String path = mute.getType() == PunishmentType.MUTE ? "Mute.Blocked" : "Tempmute.Blocked";
                String msg = L.format(L.getMessage(path), mute);
                player.sendMessage(ColorUtil.toComponent(L.getPrefix() + " " + msg));
                return;
            }
        }
    }
}
