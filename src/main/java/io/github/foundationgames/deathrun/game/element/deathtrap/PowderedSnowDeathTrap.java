package io.github.foundationgames.deathrun.game.element.deathtrap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.foundationgames.deathrun.game.element.DeathTrap;
import io.github.foundationgames.deathrun.game.state.DRGame;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.map_templates.BlockBounds;

public class PowderedSnowDeathTrap extends ResettingDeathTrap {
    public static final MapCodec<PowderedSnowDeathTrap> CODEC = MapCodec.unit(PowderedSnowDeathTrap::new);

    @Override
    public void trigger(DRGame game, ServerWorld world, BlockBounds zone) {
        for (BlockPos pos : zone) {
            var state = world.getBlockState(pos);
            if (state.isOf(Blocks.SNOW_BLOCK)) {
                world.setBlockState(pos, Blocks.POWDER_SNOW.getDefaultState());
                world.getPlayers().forEach(p -> p.networkHandler.sendPacket(new ParticleS2CPacket(ParticleTypes.CLOUD, false, false, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0, 0, 0, 0, 1)));
            }
        }
        var center = zone.center();
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_SNOW_BREAK, SoundCategory.BLOCKS, 2.0f, 1.0f);
    }

    @Override
    public void reset(DRGame game, ServerWorld world, BlockBounds zone) {
        for (BlockPos pos : zone) {
            var state = world.getBlockState(pos);
            if (state.isOf(Blocks.POWDER_SNOW)) {
                world.setBlockState(pos, Blocks.SNOW_BLOCK.getDefaultState());
            }
        }
        var center = zone.center();
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_SNOW_BREAK, SoundCategory.BLOCKS, 2.0f, 1.0f);
    }

    @Override
    public MapCodec<? extends DeathTrap> getCodec() {
        return CODEC;
    }
}
