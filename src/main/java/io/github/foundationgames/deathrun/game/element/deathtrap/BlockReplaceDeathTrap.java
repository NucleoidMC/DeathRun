package io.github.foundationgames.deathrun.game.element.deathtrap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.foundationgames.deathrun.game.element.DeathTrap;
import io.github.foundationgames.deathrun.game.state.DRGame;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.map_templates.BlockBounds;

public class BlockReplaceDeathTrap extends ResettingDeathTrap {
    public static final Codec<BlockReplaceDeathTrap> CODEC = RecordCodecBuilder.create(instance ->
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
    public void trigger(DRGame game, ServerWorld world, BlockBounds zone) {
        for (BlockPos pos : zone) {
            var state = world.getBlockState(pos);
            if (filterByState ? state == first : state.isOf(first.getBlock())) {
                world.setBlockState(pos, second);
            }
        }
    }

    @Override
    public void reset(DRGame game, ServerWorld world, BlockBounds zone) {
        for (BlockPos pos : zone) {
            var state = world.getBlockState(pos);
            if (filterByState ? state == second : state.isOf(second.getBlock())) {
                world.setBlockState(pos, first);
            }
        }
    }

    @Override
    public Codec<? extends DeathTrap> getCodec() {
        return CODEC;
    }
}
