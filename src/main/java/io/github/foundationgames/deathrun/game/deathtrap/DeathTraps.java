package io.github.foundationgames.deathrun.game.deathtrap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.foundationgames.deathrun.DeathRun;
import io.github.foundationgames.deathrun.game.deathtrap.traps.DripleafDeathTrap;
import io.github.foundationgames.deathrun.game.deathtrap.traps.DripstoneDeathTrap;
import io.github.foundationgames.deathrun.game.deathtrap.traps.LightningDeathTrap;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.registry.TinyRegistry;

import java.util.function.Function;

public class DeathTraps {
    public static final TinyRegistry<Codec<? extends DeathTrap>> REGISTRY = TinyRegistry.create();
    public static final MapCodec<DeathTrap> CODEC = REGISTRY.dispatchMap(DeathTrap::getCodec, Function.identity());

    public static final Identifier DRIPLEAF = register("dripleaf", DripleafDeathTrap.CODEC);
    public static final Identifier LIGHTNING = register("lightning", LightningDeathTrap.CODEC);
    public static final Identifier DRIPSTONE = register("dripstone", DripstoneDeathTrap.CODEC);

    public static Identifier getId(DeathTrap piece) {
        return REGISTRY.getIdentifier(piece.getCodec());
    }

    private static Identifier register(String name, Codec<? extends DeathTrap> codec) {
        return register(DeathRun.id(name), codec);
    }

    public static Identifier register(Identifier identifier, Codec<? extends DeathTrap> codec) {
        REGISTRY.register(identifier, codec);
        return identifier;
    }
}
