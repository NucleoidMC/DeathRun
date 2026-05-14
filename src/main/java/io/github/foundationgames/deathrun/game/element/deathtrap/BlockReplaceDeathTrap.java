package io.github.foundationgames.deathrun.game.element.deathtrap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.foundationgames.deathrun.game.element.DeathTrap;
import io.github.foundationgames.deathrun.game.state.DRGame;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import xyz.nucleoid.map_templates.BlockBounds;

public class BlockReplaceDeathTrap extends ResettingDeathTrap {
    public static final MapCodec<BlockReplaceDeathTrap> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BlockState.CODEC.fieldOf("first").forGetter(trap -> trap.first),
                    BlockState.CODEC.fieldOf("second").forGetter(trap -> trap.second),
                    Codec.BOOL.optionalFieldOf("filter_by_state", true).forGetter(trap -> trap.filterByState)
            ).apply(instance, BlockReplaceDeathTrap::new)
    );

    private final BlockState first;
    private final BlockState second;
    private final boolean filterByState;

    public BlockReplaceDeathTrap(BlockState first, BlockState second, boolean filterByState) {
        this.first = first;
        this.second = second;
        this.filterByState = filterByState;
    }

    @Override
    public void trigger(DRGame game, ServerLevel level, BlockBounds zone) {
        for (BlockPos pos : zone) {
            var state = level.getBlockState(pos);
            if (filterByState ? state == first : state.is(first.getBlock())) {
                level.setBlockAndUpdate(pos, second);
            }
        }
    }

    @Override
    public void reset(DRGame game, ServerLevel level, BlockBounds zone) {
        for (BlockPos pos : zone) {
            var state = level.getBlockState(pos);
            if (filterByState ? state == second : state.is(second.getBlock())) {
                level.setBlockAndUpdate(pos, first);
            }
        }
    }

    @Override
    public MapCodec<? extends DeathTrap> getCodec() {
        return CODEC;
    }
}
