package io.github.foundationgames.deathrun.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.foundationgames.deathrun.game.map.DRMapConfig;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;

public record DeathRunConfig(boolean runnersOnly, DRMapConfig map, WaitingLobbyConfig players) {
    public static final MapCodec<DeathRunConfig> CODEC = RecordCodecBuilder.mapCodec(inst ->
            inst.group(
                    Codec.BOOL.fieldOf("runners_only").forGetter(DeathRunConfig::runnersOnly),
                    DRMapConfig.CODEC.fieldOf("map").forGetter(DeathRunConfig::map),
                    WaitingLobbyConfig.CODEC.fieldOf("players").forGetter(DeathRunConfig::players)
            ).apply(inst, DeathRunConfig::new)
    );
}
