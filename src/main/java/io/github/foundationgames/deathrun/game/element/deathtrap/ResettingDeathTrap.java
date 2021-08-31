package io.github.foundationgames.deathrun.game.element.deathtrap;

import io.github.foundationgames.deathrun.game.element.DeathTrap;
import io.github.foundationgames.deathrun.game.state.DRGame;
import net.minecraft.server.world.ServerWorld;
import xyz.nucleoid.map_templates.BlockBounds;

public abstract class ResettingDeathTrap extends DeathTrap {
    public abstract void reset(DRGame game, ServerWorld world, BlockBounds zone);
}
