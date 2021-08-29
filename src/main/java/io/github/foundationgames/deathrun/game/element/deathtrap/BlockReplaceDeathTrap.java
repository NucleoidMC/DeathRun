package io.github.foundationgames.deathrun.game.element.deathtrap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.foundationgames.deathrun.game.element.DeathTrap;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.map_templates.BlockBounds;

public class BlockReplaceDeathTrap extends ResettingDeathTrap {
    public static final Codec<BlockReplaceDeathTrap> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockState.CODEC.fieldOf("first").forGetter(trap -> trap.first),
                    BlockState.CODEC.fieldOf("second").forGetter(trap -> trap.second)
            ).apply(instance, BlockReplaceDeathTrap::new)
    );

    private final BlockState first;
    private final BlockState second;

    public BlockReplaceDeathTrap(BlockState first, BlockState second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public void trigger(ServerWorld world, BlockBounds zone) {
        for (BlockPos pos : zone) {
            var state = world.getBlockState(pos);
            if (state == first) {
                world.setBlockState(pos, second);
            }
        }
    }

    @Override
    public void reset(ServerWorld world, BlockBounds zone) {
        for (BlockPos pos : zone) {
            var state = world.getBlockState(pos);
            if (state == second) {
                world.setBlockState(pos, first);
            }
        }
    }

    @Override
    public Codec<? extends DeathTrap> getCodec() {
        return CODEC;
    }
}
