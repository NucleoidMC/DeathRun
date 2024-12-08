package io.github.foundationgames.deathrun.game.element;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.foundationgames.deathrun.game.state.DRGame;
import net.minecraft.server.world.ServerWorld;
import xyz.nucleoid.map_templates.BlockBounds;

public abstract class DeathTrap {
    public abstract void trigger(DRGame game, ServerWorld world, BlockBounds zone);

    public abstract MapCodec<? extends DeathTrap> getCodec();
}
