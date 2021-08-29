package io.github.foundationgames.deathrun.game.element.deathtrap;

import io.github.foundationgames.deathrun.game.element.DeathTrap;
import net.minecraft.server.world.ServerWorld;
import xyz.nucleoid.map_templates.BlockBounds;

public abstract class ResettingDeathTrap extends DeathTrap {
    public abstract void reset(ServerWorld world, BlockBounds zone);
}
