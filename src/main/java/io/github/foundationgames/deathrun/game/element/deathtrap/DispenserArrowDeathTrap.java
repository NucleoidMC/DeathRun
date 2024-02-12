package io.github.foundationgames.deathrun.game.element.deathtrap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.foundationgames.deathrun.game.element.DeathTrap;
import io.github.foundationgames.deathrun.game.state.DRGame;
import io.github.foundationgames.deathrun.game.state.logic.entity.ProjectileEntityBehavior;
import net.minecraft.block.Blocks;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.map_templates.BlockBounds;

public class DispenserArrowDeathTrap extends DeathTrap {
    public static final Codec<DispenserArrowDeathTrap> CODEC = RecordCodecBuilder.create(instance ->
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
    public void trigger(DRGame game, ServerWorld world, BlockBounds zone) {
        for (BlockPos pos : zone) {
            var state = world.getBlockState(pos);
            if (state.isOf(Blocks.DISPENSER)) {
                var facing = state.get(Properties.FACING);
                var arrPos = Vec3d.ofCenter(pos.offset(facing));
                var arrow = new ArrowEntity(world, arrPos.x, arrPos.y, arrPos.z, Items.ARROW.getDefaultStack());
                arrow.setVelocity(facing.getOffsetX(), facing.getOffsetY() + 0.1, facing.getOffsetZ(), force, variation);
                world.syncWorldEvent(DISPENSER_EVENT_ID, pos.offset(facing), 0);
                game.spawn(arrow, new ProjectileEntityBehavior.Arrow());
            }
        }
    }

    @Override
    public Codec<? extends DeathTrap> getCodec() {
        return CODEC;
    }
}
