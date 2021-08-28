package io.github.foundationgames.deathrun.game.deathtrap;

import net.minecraft.block.Blocks;
import net.minecraft.block.enums.Tilt;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.map_templates.BlockBounds;

public class DripleafDeathTrap extends DeathTrap {
    protected DripleafDeathTrap() {}

    @Override
    public void trigger(ServerWorld world, BlockBounds zone) {
        for (BlockPos pos : zone) {
            var state = world.getBlockState(pos);
            if (state.isOf(Blocks.BIG_DRIPLEAF)) {
                world.setBlockState(pos, state.with(Properties.TILT, Tilt.NONE));
            }
        }
        var center = zone.center();
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_BIG_DRIPLEAF_FALL, SoundCategory.BLOCKS, 2.0f, 1.0f);
    }

    static {
        DeathTrap.add("dripleaf", new DripleafDeathTrap());
    }
}
