package io.github.foundationgames.deathrun.game.element.deathtrap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.foundationgames.deathrun.game.element.DeathTrap;
import io.github.foundationgames.deathrun.game.state.DRGame;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.map_templates.BlockBounds;

public class LightningDeathTrap extends DeathTrap {
    public static final MapCodec<LightningDeathTrap> CODEC = MapCodec.unit(LightningDeathTrap::new);

    @Override
    public void trigger(DRGame game, ServerWorld world, BlockBounds zone) {
        for (BlockPos pos : zone) {
            var state = world.getBlockState(pos);
            if (state.isOf(Blocks.LIGHTNING_ROD)) {
                var lightning = EntityType.LIGHTNING_BOLT.create(world, SpawnReason.COMMAND);
                lightning.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(pos.up()));
                world.spawnEntity(lightning);
            }
        }
    }

    @Override
    public MapCodec<? extends DeathTrap> getCodec() {
        return CODEC;
    }
}
