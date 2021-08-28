package io.github.foundationgames.deathrun.game.deathtrap;

import net.minecraft.server.world.ServerWorld;
import xyz.nucleoid.map_templates.BlockBounds;

import java.util.HashMap;
import java.util.Map;

public class DeathTrap {
    private static final Map<String, DeathTrap> ENTRIES = new HashMap<>();

    public static final DeathTrap NO_OP = new DeathTrap();

    protected DeathTrap() {}

    public void trigger(ServerWorld world, BlockBounds zone) {}

    public void reset(ServerWorld world, BlockBounds zone) {}

    public static DeathTrap add(String name, DeathTrap deathTrap) {
        ENTRIES.put(name, deathTrap);
        return deathTrap;
    }

    public static DeathTrap get(String name) {
        return ENTRIES.getOrDefault(name, NO_OP);
    }

    public static void init() {
        DripleafDeathTrap.init();
        LightningDeathTrap.init();
        DripstoneDeathTrap.init();
    }
}
