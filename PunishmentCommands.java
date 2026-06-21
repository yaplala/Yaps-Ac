package dk.zai.anticheat.checks;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.checks.combat.*;
import dk.zai.anticheat.checks.movement.*;
import dk.zai.anticheat.checks.player.*;
import dk.zai.anticheat.checks.exploits.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Container that instantiates every check and exposes them as a single
 * object so listeners and commands can reference them without DI.
 */
public class CheckContainer {

    // Combat
    public final KillAuraCheck killAura;
    public final ReachCheck reach;
    public final SilentAimCheck silentAim;
    public final AutoClickerCheck autoClicker;

    // Movement
    public final FlyCheck fly;
    public final SpeedCheck speed;
    public final JesusCheck jesus;
    public final NoSlowdownCheck noslowdown;
    public final SpiderCheck spider;
    public final StepCheck step;
    public final NoWebCheck noweb;

    // Player / world
    public final NoFallCheck nofall;
    public final FastPlaceCheck fastPlace;
    public final FastBreakCheck fastBreak;
    public final AutoEatCheck autoEat;
    public final AutoArmorCheck autoArmor;

    // Exploits
    public final TimerCheck timer;
    public final InvalidPacketsCheck invalidPackets;

    public CheckContainer(AntiCheatPlugin plugin) {
        killAura = new KillAuraCheck(plugin);
        reach = new ReachCheck(plugin);
        silentAim = new SilentAimCheck(plugin);
        autoClicker = new AutoClickerCheck(plugin);

        fly = new FlyCheck(plugin);
        speed = new SpeedCheck(plugin);
        jesus = new JesusCheck(plugin);
        noslowdown = new NoSlowdownCheck(plugin);
        spider = new SpiderCheck(plugin);
        step = new StepCheck(plugin);
        noweb = new NoWebCheck(plugin);

        nofall = new NoFallCheck(plugin);
        fastPlace = new FastPlaceCheck(plugin);
        fastBreak = new FastBreakCheck(plugin);
        autoEat = new AutoEatCheck(plugin);
        autoArmor = new AutoArmorCheck(plugin);

        timer = new TimerCheck(plugin);
        invalidPackets = new InvalidPacketsCheck(plugin);
    }

    public List<Check> all() {
        List<Check> list = new ArrayList<>();
        list.add(killAura); list.add(reach); list.add(silentAim); list.add(autoClicker);
        list.add(fly); list.add(speed); list.add(jesus); list.add(noslowdown);
        list.add(spider); list.add(step); list.add(noweb);
        list.add(nofall); list.add(fastPlace); list.add(fastBreak);
        list.add(autoEat); list.add(autoArmor);
        list.add(timer); list.add(invalidPackets);
        return list;
    }

    public void reloadAll() { all().forEach(Check::reload); }
}
