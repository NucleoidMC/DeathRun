package io.github.foundationgames.deathrun;

import com.mojang.serialization.Codec;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import io.github.foundationgames.deathrun.game.DeathRunConfig;
import io.github.foundationgames.deathrun.game.state.DRWaiting;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nucleoid.plasmid.api.game.GameType;
import xyz.nucleoid.plasmid.api.game.GameTypes;

public class DeathRun implements ModInitializer {
    public static final String MOD_ID = "deathrun";

    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    public static final GameType<DeathRunConfig> TYPE = GameTypes.register(
            id("deathrun"),
            DeathRunConfig.CODEC,
            DRWaiting::open
    );

    public static final DataComponentType<String> BEHAVIOR = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id("behavior"), DataComponentType.<String>builder().persistent(Codec.STRING).build());

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        PolymerComponent.registerDataComponent(BEHAVIOR);
    }
}
