package io.github.foundationgames.deathrun.game.element;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.foundationgames.deathrun.DeathRun;
import io.github.foundationgames.deathrun.game.element.deathtrap.*;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.api.util.TinyRegistry;

import java.util.function.Function;

public class DeathTraps {
    public static final TinyRegistry<MapCodec<? extends DeathTrap>> REGISTRY = TinyRegistry.create();
    public static final MapCodec<DeathTrap> CODEC = REGISTRY.dispatchMap(DeathTrap::getCodec, Function.identity());

    public static final Identifier DRIPLEAF = register("dripleaf", DripleafDeathTrap.CODEC);
    public static final Identifier LIGHTNING = register("lightning", LightningDeathTrap.CODEC);
    public static final Identifier DRIPSTONE = register("dripstone", DripstoneDeathTrap.CODEC);
    public static final Identifier POWDERED_SNOW = register("powdered_snow", PowderedSnowDeathTrap.CODEC);
    public static final Identifier INVISIBLE_PATH = register("invisible_path", InvisiblePathDeathTrap.CODEC);
    public static final Identifier BLOCK_REPLACE = register("block_replace", BlockReplaceDeathTrap.CODEC);
    public static final Identifier DISPENSER_ARROW = register("dispenser_arrow", DispenserArrowDeathTrap.CODEC);

    public static Identifier getId(DeathTrap piece) {
        return REGISTRY.getIdentifier(piece.getCodec());
    }

    private static Identifier register(String name, MapCodec<? extends DeathTrap> codec) {
        return register(DeathRun.id(name), codec);
    }

    public static Identifier register(Identifier identifier, MapCodec<? extends DeathTrap> codec) {
        REGISTRY.register(identifier, codec);
        return identifier;
    }
}
