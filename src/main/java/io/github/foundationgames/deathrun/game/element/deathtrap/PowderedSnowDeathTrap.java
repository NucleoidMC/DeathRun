package io.github.foundationgames.deathrun.game.element.deathtrap;

import com.mojang.serialization.Codec;
import io.github.foundationgames.deathrun.game.element.DeathTrap;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.map_templates.BlockBounds;

public class PowderedSnowDeathTrap extends ResettingDeathTrap {
    public static final Codec<PowderedSnowDeathTrap> CODEC = Codec.unit(PowderedSnowDeathTrap::new);

    @Override
    public void trigger(ServerWorld world, BlockBounds zone) {
        for (BlockPos pos : zone) {
            var state = world.getBlockState(pos);
            if (state.isOf(Blocks.SNOW)) {
                world.setBlockState(pos, Blocks.POWDER_SNOW.getDefaultState());
            }
        }
        var center = zone.center();
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_SNOW_BREAK, SoundCategory.BLOCKS, 2.0f, 1.0f);
    }

    @Override
    public void reset(ServerWorld world, BlockBounds zone) {
        for (BlockPos pos : zone) {
            var state = world.getBlockState(pos);
            if (state.isOf(Blocks.POWDER_SNOW)) {
                world.setBlockState(pos, Blocks.SNOW.getDefaultState());
            }
        }
        var center = zone.center();
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_SNOW_BREAK, SoundCategory.BLOCKS, 2.0f, 1.0f);
    }

    @Override
    public Codec<? extends DeathTrap> getCodec() {
        return CODEC;
    }
}
