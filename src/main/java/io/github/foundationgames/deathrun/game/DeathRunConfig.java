package io.github.foundationgames.deathrun.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.foundationgames.deathrun.game.map.DRMapConfig;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;

public record DeathRunConfig(boolean runnersOnly, DRMapConfig map, PlayerConfig players) {
    public static final Codec<DeathRunConfig> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    Codec.BOOL.fieldOf("runners_only").forGetter(DeathRunConfig::runnersOnly),
                    DRMapConfig.CODEC.fieldOf("map").forGetter(DeathRunConfig::map),
                    PlayerConfig.CODEC.fieldOf("players").forGetter(DeathRunConfig::players)
            ).apply(inst, DeathRunConfig::new)
    );
}
