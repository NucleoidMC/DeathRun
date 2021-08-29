package io.github.foundationgames.deathrun.game.element;

import com.mojang.serialization.Codec;
import net.minecraft.server.world.ServerWorld;
import xyz.nucleoid.map_templates.BlockBounds;

public abstract class DeathTrap {
    public abstract void trigger(ServerWorld world, BlockBounds zone);

    public abstract Codec<? extends DeathTrap> getCodec();
}
