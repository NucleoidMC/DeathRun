package io.github.foundationgames.deathrun.game.element.deathtrap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.foundationgames.deathrun.game.element.DeathTrap;
import io.github.foundationgames.deathrun.game.state.DRGame;
import io.github.foundationgames.deathrun.game.state.logic.entity.ProjectileEntityBehavior;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.map_templates.BlockBounds;

public class DispenserArrowDeathTrap extends DeathTrap {
    public static final MapCodec<DispenserArrowDeathTrap> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.FLOAT.optionalFieldOf("variation", 6f).forGetter(trap -> trap.variation),
                    Codec.FLOAT.optionalFieldOf("force", 1.1f).forGetter(trap -> trap.force)
            ).apply(instance, DispenserArrowDeathTrap::new)
    );

    private static final int DISPENSER_EVENT_ID = 1002;

    private final float variation;
    private final float force;

    public DispenserArrowDeathTrap(float variation, float force) {
        this.variation = variation;
        this.force = force;
    }

    @Override
    public void trigger(DRGame game, ServerLevel level, BlockBounds zone) {
        for (BlockPos pos : zone) {
            var state = level.getBlockState(pos);
            if (state.is(Blocks.DISPENSER)) {
                var facing = state.getValue(BlockStateProperties.FACING);
                var arrPos = Vec3.atCenterOf(pos.relative(facing));
                var arrow = new Arrow(level, arrPos.x, arrPos.y, arrPos.z, Items.ARROW.getDefaultInstance(), Items.BOW.getDefaultInstance());
                arrow.shoot(facing.getStepX(), facing.getStepY() + 0.1, facing.getStepZ(), force, variation);
                level.levelEvent(DISPENSER_EVENT_ID, pos.relative(facing), 0);
                game.spawn(arrow, new ProjectileEntityBehavior.Arrow());
            }
        }
    }

    @Override
    public MapCodec<? extends DeathTrap> getCodec() {
        return CODEC;
    }
}
