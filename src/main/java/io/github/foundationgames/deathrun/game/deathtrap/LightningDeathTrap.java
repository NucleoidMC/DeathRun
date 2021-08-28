package io.github.foundationgames.deathrun.game.deathtrap;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.map_templates.BlockBounds;

public class LightningDeathTrap extends DeathTrap {
    @Override
    public void trigger(ServerWorld world, BlockBounds zone) {
        for (BlockPos pos : zone) {
            var state = world.getBlockState(pos);
            if (state.isOf(Blocks.LIGHTNING_ROD)) {
                var lightning = EntityType.LIGHTNING_BOLT.create(world);
                lightning.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(pos.up()));
            }
        }
        var center = zone.center();
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_BIG_DRIPLEAF_FALL, SoundCategory.BLOCKS, 2.0f, 1.0f);
    }
}
