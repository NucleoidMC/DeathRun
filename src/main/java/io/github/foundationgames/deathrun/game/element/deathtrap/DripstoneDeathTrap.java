package io.github.foundationgames.deathrun.game.element.deathtrap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.foundationgames.deathrun.game.element.DeathTrap;
import io.github.foundationgames.deathrun.game.state.DRGame;
import io.github.foundationgames.deathrun.mixin.FallingBlockEntityAccess;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.map_templates.BlockBounds;

public class DripstoneDeathTrap extends DeathTrap {
    public static final MapCodec<DripstoneDeathTrap> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.INT.optionalFieldOf("length", 2).forGetter(trap -> trap.length)
            ).apply(instance, DripstoneDeathTrap::new)
    );

    private static final BlockState[] dripstoneStates = {
            dripstoneState(DripstoneThickness.TIP),
            dripstoneState(DripstoneThickness.FRUSTUM),
            dripstoneState(DripstoneThickness.MIDDLE)
    };

    private final int length;

    public DripstoneDeathTrap(int length) {
        this.length = length;
    }

    @Override
    public void trigger(DRGame game, ServerLevel level, BlockBounds zone) {
        for (BlockPos pos : zone) {
            var state = level.getBlockState(pos);
            if (state.is(Blocks.DRIPSTONE_BLOCK)) {
                var dripstonePos = Vec3.atBottomCenterOf(pos.below().below(length - 1));
                float off = level.getRandom().nextFloat();
                for (int i = 0; i < length; i++) {
                    var dState = dripstoneStates[Math.min(i, dripstoneStates.length - 1)];
                    var dripstone = FallingBlockEntityAccess.deathrun$construct(level, dripstonePos.x, dripstonePos.y + i - off, dripstonePos.z, dState);
                    dripstone.time = 1;
                    dripstone.dropItem = false;
                    level.addFreshEntity(dripstone);
                }
            }
        }
    }

    private static BlockState dripstoneState(DripstoneThickness thickness) {
        return Blocks.POINTED_DRIPSTONE.defaultBlockState()
                .setValue(BlockStateProperties.VERTICAL_DIRECTION, Direction.DOWN)
                .setValue(BlockStateProperties.DRIPSTONE_THICKNESS, thickness);
    }

    @Override
    public MapCodec<? extends DeathTrap> getCodec() {
        return CODEC;
    }
}
