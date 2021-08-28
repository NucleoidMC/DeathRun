package io.github.foundationgames.deathrun.game.deathtrap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.foundationgames.deathrun.DeathRun;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.registry.TinyRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class DeathTrap {
    public abstract void trigger(ServerWorld world, BlockBounds zone);
    public abstract Codec<? extends DeathTrap> getCodec();
}
