package io.github.foundationgames.deathrun.game.deathtrap;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.map_templates.BlockBounds;

public class LightningDeathTrap extends DeathTrap {
    protected LightningDeathTrap() {}

    @Override
    public void trigger(ServerWorld world, BlockBounds zone) {
        for (BlockPos pos : zone) {
            var state = world.getBlockState(pos);
            if (state.isOf(Blocks.LIGHTNING_ROD)) {
                var lightning = EntityType.LIGHTNING_BOLT.create(world);
                lightning.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(pos.up()));
                world.spawnEntity(lightning);
            }
        }
    }

    public static void init() {
        DeathTrap.add("lightning", new LightningDeathTrap());
    }
}
