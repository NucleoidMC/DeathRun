package io.github.foundationgames.deathrun;

import com.mojang.serialization.Codec;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import io.github.foundationgames.deathrun.game.DeathRunConfig;
import io.github.foundationgames.deathrun.game.state.DRWaiting;
import net.fabricmc.api.ModInitializer;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nucleoid.plasmid.api.game.GameType;

public class DeathRun implements ModInitializer {
    public static final String MOD_ID = "deathrun";

    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    public static final GameType<DeathRunConfig> TYPE = GameType.register(
            id("deathrun"),
            DeathRunConfig.CODEC,
            DRWaiting::open
    );

    public static final ComponentType<String> BEHAVIOR = Registry.register(Registries.DATA_COMPONENT_TYPE, id("behavior"), ComponentType.<String>builder().codec(Codec.STRING).build());

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        PolymerComponent.registerDataComponent(BEHAVIOR);
    }
}
