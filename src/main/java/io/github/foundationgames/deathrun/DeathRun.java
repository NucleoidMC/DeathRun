package io.github.foundationgames.deathrun;

import io.github.foundationgames.deathrun.game.DeathRunConfig;
import io.github.foundationgames.deathrun.game.state.DRWaiting;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nucleoid.plasmid.game.GameType;

public class DeathRun implements ModInitializer {
    public static final String MOD_ID = "deathrun";

    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    public static final GameType<DeathRunConfig> TYPE = GameType.register(
            new Identifier(MOD_ID, "deathrun"),
            DeathRunConfig.CODEC,
            DRWaiting::open
    );

    @Override
    public void onInitialize() {}
}
