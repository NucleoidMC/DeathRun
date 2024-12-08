package io.github.foundationgames.deathrun.game.element.deathtrap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.foundationgames.deathrun.game.element.DeathTrap;
import io.github.foundationgames.deathrun.game.state.DRGame;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.Tilt;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.map_templates.BlockBounds;

public class DripleafDeathTrap extends DeathTrap {
    public static final MapCodec<DripleafDeathTrap> CODEC = MapCodec.unit(DripleafDeathTrap::new);

    @Override
    public void trigger(DRGame game, ServerWorld world, BlockBounds zone) {
        for (BlockPos pos : zone) {
            var state = world.getBlockState(pos);
            if (state.isOf(Blocks.BIG_DRIPLEAF)) {
                world.setBlockState(pos, state.with(Properties.TILT, Tilt.FULL));
                world.scheduleBlockTick(pos, state.getBlock(), 69);
            }
        }
        var center = zone.center();
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_BIG_DRIPLEAF_FALL, SoundCategory.BLOCKS, 2.0f, 1.0f);
    }

    @Override
    public MapCodec<? extends DeathTrap> getCodec() {
        return CODEC;
    }
}
