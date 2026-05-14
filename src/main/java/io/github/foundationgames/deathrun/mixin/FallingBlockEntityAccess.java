package io.github.foundationgames.deathrun.mixin;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FallingBlockEntity.class)
public interface FallingBlockEntityAccess {
    @Invoker("<init>")
	static FallingBlockEntity deathrun$construct(Level world, double x, double y, double z, BlockState block) {
		throw new AssertionError();
	}
}
