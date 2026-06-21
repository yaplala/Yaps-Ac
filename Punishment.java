package dk.zai.anticheat.checks.exploits;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.checks.Check;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Invalid Packets / Packet flood detection.
 * Hooks into all inbound PlayerMove / PlayerInteract / PlayerTick events
 * via a per-second counter. If a player sends more than max-packets-per-second
 * they are flagged; if they exceed kick-packets-per-second they are kicked
 * immediately to prevent crash / lag attempts.
 *
 * Note: full packet-level access requires ProtocolLib. This implementation
 * uses Bukkit events which cover the vast majority of packet-flood vectors
 * (move, interact, chat, swap, drop) without an external dependency.
 */
public class InvalidPacketsCheck extends Check {

    private final int maxPerSecond;
    private final int kickPerSecond;

    public InvalidPacketsCheck(AntiCheatPlugin plugin) {
        super(plugin, "InvalidPackets", "exploits.invalid-packets");
        this.maxPerSecond = cfg().getInt("max-packets-per-second", 200);
        this.kickPerSecond = cfg().getInt("kick-packets-per-second", 500);
    }

    /** Called by every monitored inbound event listener. */
    public void onPacket(Player player) {
        if (!enabled || bypass(player)) return;
        var pd = plugin.getDataManager().get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (now - pd.getPacketWindowStart() > 1000L) {
            pd.setPacketWindowStart(now);
            pd.setPacketCount(0);
        }
        pd.incrementPacketCount();
        int c = pd.getPacketCount();
        if (c > kickPerSecond) {
            // Immediate kick - DDoS-style flood
            Bukkit.getScheduler().runTask(plugin, () -> player.kick(
                    net.kyori.adventure.text.Component.text("§cKicked: packet flood")));
        } else if (c == maxPerSecond + 1 || c == maxPerSecond + 50 || c == maxPerSecond + 100) {
            // Flag at a few milestones so we don't spam alerts every packet
            flag(player, "packets=" + c + "/s (cap " + maxPerSecond + ")");
        }
    }
}
