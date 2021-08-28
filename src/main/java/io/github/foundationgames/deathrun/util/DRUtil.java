package io.github.foundationgames.deathrun.util;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;

public enum DRUtil {;
    public static BlockPos blockPos(NbtCompound nbt) throws IllegalStateException {
        if (!nbt.contains("x") || !nbt.contains("y") || !nbt.contains("z")) throw new IllegalStateException("Missing position components in NBT");
        return new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
    }

    public static ItemStack createHead(int[] id, String texture) {
        var stack = new ItemStack(Items.PLAYER_HEAD);
        var nbt = stack.getOrCreateSubNbt("SkullOwner");
        nbt.putIntArray("Id", id);
        var properties = new NbtCompound();
        var textures = new NbtList();
        var textureElement = new NbtCompound();
        textureElement.putString("Value", texture);
        textures.add(textureElement);
        properties.put("textures", textures);
        nbt.put("Properties", properties);
        return stack;
    }

    public static ItemStack createDeathHead() {
        return createHead(new int[]{-880247003, -1450553484, -1400946616, -1244103976}, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzI5YWJmMWI5N2FmOGFlNmY4Yjc3YWVjN2QyNjU1N2Q4MWJhNGVlMjUzZmM2MmViNDdhYTVlOTk3ZWE0In19fQ==");
    }

    public static ItemStack createRunnerHead() {
        return createHead(new int[]{863782850, -1211740562, -2001184547, -1301797217}, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGNjZTUxZWNkOTJlOGVjNzExMWRhM2UzZmQ2YjdlOTUxZmY4OTg2ODY1NjYzYmQyNzVjNDc2ZTIzMDhlMjAifX19");
    }

    public static ItemStack createRunnerHeadB() {
        return createHead(new int[]{-2096301281, 1058292398, -1896047796, 587023939}, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTU0MjBkYjNkYWM4NzFkYmJkZThiYzcwODJkOGM3MDJmMTk4NGE5NmU2ZTczNDg5NDRiZDE3ZmQwNzdiNmViNyJ9fX0=");
    }

    public static ItemStack createClearHead() {
        return createHead(new int[]{-74133895, 960054678, -1984242782, -548259249}, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWZlNWNmMjZkZGJiZTc4NTI2MGY3ZTFjOWRmN2JjMmY1NTFiYWRjNGFlNGEyMTE4YjFjZTFkYjljYWZiMjYzMyJ9fX0=");
    }

    public static void setBaseGameRules(GameActivity game) {
        game.deny(GameRuleType.PVP)
                .deny(GameRuleType.USE_BLOCKS)
                .deny(GameRuleType.FALL_DAMAGE)
                .deny(GameRuleType.HUNGER)
                .deny(GameRuleType.CRAFTING)
                .deny(GameRuleType.PORTALS)
                .deny(GameRuleType.THROW_ITEMS)
                .deny(GameRuleType.INTERACTION)
                .deny(GameRuleType.PLACE_BLOCKS)
                .deny(GameRuleType.BREAK_BLOCKS)
                .deny(GameRuleType.MODIFY_INVENTORY);
    }
}
