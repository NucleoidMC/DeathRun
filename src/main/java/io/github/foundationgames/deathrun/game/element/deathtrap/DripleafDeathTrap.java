package io.github.foundationgames.deathrun.game.element.deathtrap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.foundationgames.deathrun.game.element.DeathTrap;
import io.github.foundationgames.deathrun.game.state.DRGame;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.Tilt;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.BlockPos;
import xyz.nucleoid.map_templates.BlockBounds;

public class DripleafDeathTrap extends DeathTrap {
    public static final MapCodec<DripleafDeathTrap> CODEC = MapCodec.unit(DripleafDeathTrap::new);

    @Override
    public void trigger(DRGame game, ServerLevel level, BlockBounds zone) {
        for (BlockPos pos : zone) {
            var state = level.getBlockState(pos);
            if (state.is(Blocks.BIG_DRIPLEAF)) {
                level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.TILT, Tilt.FULL));
                level.scheduleTick(pos, state.getBlock(), 69);
            }
        }
        var center = zone.center();
        level.playSound(null, center.x, center.y, center.z, SoundEvents.BIG_DRIPLEAF_FALL, SoundSource.BLOCKS, 2.0f, 1.0f);
    }

    @Override
    public MapCodec<? extends DeathTrap> getCodec() {
        return CODEC;
    }
}
