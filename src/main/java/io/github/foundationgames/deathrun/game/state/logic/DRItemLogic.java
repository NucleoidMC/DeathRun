package io.github.foundationgames.deathrun.game.state.logic;

import io.github.foundationgames.deathrun.DeathRun;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

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

    public ActionResult processUse(ServerPlayerEntity player, Hand hand) {
        var stack = player.getStackInHand(hand);
        var behavior = entries.get(stack.getOrDefault(DeathRun.BEHAVIOR, ""));
        if (behavior != null) {
            return behavior.use(player, stack, hand);
        }
        return ActionResult.PASS;
    }

    public interface Behavior {
        ActionResult use(ServerPlayerEntity player, ItemStack stack, Hand hand);
    }
}
