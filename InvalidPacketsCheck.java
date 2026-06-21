package dk.zai.anticheat.checks.combat;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.checks.Check;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Reach detection.
 * Flags if attack distance exceeds max-reach (or max-reach-mace if the
 * player is falling fast while holding a Mace - the 1.21.11 mace buffer).
 */
public class ReachCheck extends Check {

    private final double maxReach;
    private final double maxReachMace;
    private final double maceMinFallSpeed;

    public ReachCheck(AntiCheatPlugin plugin) {
        super(plugin, "Reach", "combat.reach");
        this.maxReach = cfg().getDouble("max-reach", 3.2);
        this.maxReachMace = cfg().getDouble("max-reach-mace", 6.0);
        this.maceMinFallSpeed = cfg().getDouble("mace-min-fall-speed", -0.4);
    }

    public void onAttack(Player attacker, Entity target, EntityDamageByEntityEvent event) {
        if (!enabled || bypass(attacker)) return;

        double dist = attacker.getLocation().distance(target.getLocation());
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        boolean mace = weapon != null && weapon.getType() == Material.MACE;
        boolean fallAttack = attacker.getVelocity().getY() < maceMinFallSpeed;
        double limit = (mace && fallAttack) ? maxReachMace : maxReach;

        if (dist > limit) {
            flag(attacker, "dist=" + String.format("%.2f", dist) + " limit=" + limit
                    + (mace ? " (mace)" : ""));
            if (plugin.getConfig().getBoolean("settings.cancel-on-flag", true)) event.setCancelled(true);
        }
    }
}
