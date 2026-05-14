package io.github.foundationgames.deathrun.game.element.deathtrap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.foundationgames.deathrun.game.element.DeathTrap;
import io.github.foundationgames.deathrun.game.state.DRGame;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.map_templates.BlockBounds;

public class LightningDeathTrap extends DeathTrap {
    public static final MapCodec<LightningDeathTrap> CODEC = MapCodec.unit(LightningDeathTrap::new);

    @Override
    public void trigger(DRGame game, ServerLevel level, BlockBounds zone) {
        for (BlockPos pos : zone) {
            var state = level.getBlockState(pos);
            if (state.is(Blocks.LIGHTNING_ROD)) {
                var lightning = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.COMMAND);
                lightning.snapTo(Vec3.atBottomCenterOf(pos.above()));
                level.addFreshEntity(lightning);
            }
        }
    }

    @Override
    public MapCodec<? extends DeathTrap> getCodec() {
        return CODEC;
    }
}
