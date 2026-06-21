package dk.zai.anticheat.listeners;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.checks.movement.*;
import dk.zai.anticheat.checks.player.NoFallCheck;
import dk.zai.anticheat.checks.exploits.TimerCheck;
import dk.zai.anticheat.checks.exploits.InvalidPacketsCheck;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Aggregates all movement-based checks. Runs at LOW priority so other
 * plugins can override our cancel before MONITOR listeners see the result.
 */
public class MoveListener implements Listener {

    private final AntiCheatPlugin plugin;
    private final FlyCheck fly;
    private final SpeedCheck speed;
    private final JesusCheck jesus;
    private final NoSlowdownCheck noslowdown;
    private final SpiderCheck spider;
    private final StepCheck step;
    private final NoWebCheck noweb;
    private final NoFallCheck nofall;
    private final TimerCheck timer;
    private final InvalidPacketsCheck invalidPackets;

    public MoveListener(AntiCheatPlugin plugin) {
        this.plugin = plugin;
        this.fly = plugin.getChecks().fly;
        this.speed = plugin.getChecks().speed;
        this.jesus = plugin.getChecks().jesus;
        this.noslowdown = plugin.getChecks().noslowdown;
        this.spider = plugin.getChecks().spider;
        this.step = plugin.getChecks().step;
        this.noweb = plugin.getChecks().noweb;
        this.nofall = plugin.getChecks().nofall;
        this.timer = plugin.getChecks().timer;
        this.invalidPackets = plugin.getChecks().invalidPackets;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        var player = event.getPlayer();

        // Packet flood + timer accounting for every inbound move
        invalidPackets.onPacket(player);
        timer.onMovePacket(player);

        // Movement checks
        fly.onMove(player, event);
        speed.onMove(player, event);
        jesus.onMove(player, event);
        noslowdown.onMove(player, event);
        spider.onMove(player, event);
        step.onMove(player, event);
        noweb.onMove(player, event);
        nofall.onMove(player, event);
    }
}
