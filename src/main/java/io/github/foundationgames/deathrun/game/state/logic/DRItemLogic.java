package io.github.foundationgames.deathrun.game.state.logic;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;

import java.util.HashMap;
import java.util.Map;

public class DRItemLogic {
    private final Map<String, Behavior> entries = new HashMap<>();
    
    public void addBehavior(String name, Behavior behavior) {
        entries.put(name, behavior);
    }

    public static void apply(String behavior, ItemStack stack) {
        stack.getOrCreateNbt().putString("behavior", behavior);
    }

    public TypedActionResult<ItemStack> processUse(ServerPlayerEntity player, Hand hand) {
        var stack = player.getStackInHand(hand);
        if (stack.hasNbt()) {
            if (stack.getNbt().contains("behavior")) {
                var behavior = entries.get(stack.getNbt().getString("behavior"));
                return behavior.use(player, stack, hand);
            }
        }
        return TypedActionResult.pass(stack);
    }

    public interface Behavior {
        TypedActionResult<ItemStack> use(ServerPlayerEntity player, ItemStack stack, Hand hand);
    }
}
