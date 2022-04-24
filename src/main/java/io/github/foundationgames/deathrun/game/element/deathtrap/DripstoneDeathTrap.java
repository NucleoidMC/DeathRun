package io.github.foundationgames.deathrun.game.element.deathtrap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.foundationgames.deathrun.game.element.DeathTrap;
import io.github.foundationgames.deathrun.game.state.DRGame;
import io.github.foundationgames.deathrun.mixin.FallingBlockEntityAccess;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.Thickness;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.map_templates.BlockBounds;

public class DripstoneDeathTrap extends DeathTrap {
    public static final Codec<DripstoneDeathTrap> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.optionalFieldOf("length", 2).forGetter(trap -> trap.length)
            ).apply(instance, DripstoneDeathTrap::new)
    );

    private static final BlockState[] dripstoneStates = {
            dripstoneState(Thickness.TIP),
            dripstoneState(Thickness.FRUSTUM),
            dripstoneState(Thickness.MIDDLE)
    };

    private final int length;

    public DripstoneDeathTrap(int length) {
        this.length = length;
    }

    @Override
    public void trigger(DRGame game, ServerWorld world, BlockBounds zone) {
        for (BlockPos pos : zone) {
            var state = world.getBlockState(pos);
            if (state.isOf(Blocks.DRIPSTONE_BLOCK)) {
                var dripstonePos = Vec3d.ofBottomCenter(pos.down().down(length - 1));
                float off = world.random.nextFloat();
                for (int i = 0; i < length; i++) {
                    var dState = dripstoneStates[Math.min(i, dripstoneStates.length - 1)];
                    var dripstone = FallingBlockEntityAccess.deathrun$construct(world, dripstonePos.x, dripstonePos.y + i - off, dripstonePos.z, dState);
                    dripstone.timeFalling = 1;
                    dripstone.dropItem = false;
                    world.spawnEntity(dripstone);
                }
            }
        }
    }

    private static BlockState dripstoneState(Thickness thickness) {
        return Blocks.POINTED_DRIPSTONE.getDefaultState()
                .with(Properties.VERTICAL_DIRECTION, Direction.DOWN)
                .with(Properties.THICKNESS, thickness);
    }

    @Override
    public Codec<? extends DeathTrap> getCodec() {
        return CODEC;
    }
}
