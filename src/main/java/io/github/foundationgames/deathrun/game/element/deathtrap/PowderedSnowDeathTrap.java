package io.github.foundationgames.deathrun.game.element.deathtrap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.foundationgames.deathrun.game.element.DeathTrap;
import io.github.foundationgames.deathrun.game.state.DRGame;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import xyz.nucleoid.map_templates.BlockBounds;

public class PowderedSnowDeathTrap extends ResettingDeathTrap {
    public static final MapCodec<PowderedSnowDeathTrap> CODEC = MapCodec.unit(PowderedSnowDeathTrap::new);

    @Override
    public void trigger(DRGame game, ServerLevel level, BlockBounds zone) {
        for (BlockPos pos : zone) {
            var state = level.getBlockState(pos);
            if (state.is(Blocks.SNOW_BLOCK)) {
                level.setBlockAndUpdate(pos, Blocks.POWDER_SNOW.defaultBlockState());
                level.players().forEach(p -> p.connection.send(new ClientboundLevelParticlesPacket(ParticleTypes.CLOUD, false, false, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0, 0, 0, 0, 1)));
            }
        }
        var center = zone.center();
        level.playSound(null, center.x, center.y, center.z, SoundEvents.SNOW_BREAK, SoundSource.BLOCKS, 2.0f, 1.0f);
    }

    @Override
    public void reset(DRGame game, ServerLevel level, BlockBounds zone) {
        for (BlockPos pos : zone) {
            var state = level.getBlockState(pos);
            if (state.is(Blocks.POWDER_SNOW)) {
                level.setBlockAndUpdate(pos, Blocks.SNOW_BLOCK.defaultBlockState());
            }
        }
        var center = zone.center();
        level.playSound(null, center.x, center.y, center.z, SoundEvents.SNOW_BREAK, SoundSource.BLOCKS, 2.0f, 1.0f);
    }

    @Override
    public MapCodec<? extends DeathTrap> getCodec() {
        return CODEC;
    }
}
