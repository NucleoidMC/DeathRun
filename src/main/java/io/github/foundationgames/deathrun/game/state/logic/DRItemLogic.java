package io.github.foundationgames.deathrun.game.state.logic;

import io.github.foundationgames.deathrun.DeathRun;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;

import java.util.HashMap;
import java.util.Map;

public class DRItemLogic {
    private final Map<String, Behavior> entries = new HashMap<>();
    
    public void addBehavior(String name, Behavior behavior) {
        entries.put(name, behavior);
    }

    public static void apply(String behavior, ItemStack stack) {
        stack.set(DeathRun.BEHAVIOR, behavior);
    }

    public InteractionResult processUse(ServerPlayer player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        var behavior = entries.get(stack.getOrDefault(DeathRun.BEHAVIOR, ""));
        if (behavior != null) {
            return behavior.use(player, stack, hand);
        }
        return InteractionResult.PASS;
    }

    public interface Behavior {
        InteractionResult use(ServerPlayer player, ItemStack stack, InteractionHand hand);
    }
}
