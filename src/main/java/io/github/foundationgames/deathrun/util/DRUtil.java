package io.github.foundationgames.deathrun.util;

import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import eu.pb4.polymer.core.api.utils.PolymerUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;

import java.util.UUID;

public enum DRUtil {
    ;

    public static ItemStack createHead(String texture) {
        return PolymerUtils.createPlayerHead(texture);
    }

    public static ItemStack createDeathHead() {
        return createHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzI5YWJmMWI5N2FmOGFlNmY4Yjc3YWVjN2QyNjU1N2Q4MWJhNGVlMjUzZmM2MmViNDdhYTVlOTk3ZWE0In19fQ==");
    }

    public static ItemStack createRunnerHead() {
        return createHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGNjZTUxZWNkOTJlOGVjNzExMWRhM2UzZmQ2YjdlOTUxZmY4OTg2ODY1NjYzYmQyNzVjNDc2ZTIzMDhlMjAifX19");
    }

    public static ItemStack createRunnerHeadB() {
        return createHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTU0MjBkYjNkYWM4NzFkYmJkZThiYzcwODJkOGM3MDJmMTk4NGE5NmU2ZTczNDg5NDRiZDE3ZmQwNzdiNmViNyJ9fX0=");
    }

    public static ItemStack createClearHead() {
        return createHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWZlNWNmMjZkZGJiZTc4NTI2MGY3ZTFjOWRmN2JjMmY1NTFiYWRjNGFlNGEyMTE4YjFjZTFkYjljYWZiMjYzMyJ9fX0=");
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
                .deny(GameRuleType.MODIFY_INVENTORY)
                .deny(GameRuleType.MODIFY_ARMOR);
    }
}
