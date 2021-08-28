package io.github.foundationgames.deathrun.game.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;

public record DRMapConfig(int time, Identifier mapId) {
    public static final Codec<DRMapConfig> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    Codec.INT.optionalFieldOf("time", 6000).forGetter(DRMapConfig::time),
                    Identifier.CODEC.fieldOf("map_id").forGetter(DRMapConfig::mapId)
            ).apply(inst, DRMapConfig::new)
    );
}
