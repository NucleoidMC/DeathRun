package io.github.foundationgames.deathrun.game.element;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import xyz.nucleoid.map_templates.BlockBounds;

public record EffectZone(BlockBounds bounds, Effect effect) {

    public record Effect(Identifier id, int amplifier) {
        public static final Codec<Effect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.fieldOf("effect").forGetter(Effect::id),
                Codec.INT.fieldOf("amplifier").forGetter(Effect::amplifier)
        ).apply(instance, Effect::new));

        public StatusEffectInstance createEffect() {
            if (!Registries.STATUS_EFFECT.containsId(id)) return null;
            return new StatusEffectInstance(Registries.STATUS_EFFECT.get(id), 2, amplifier, true, false, true);
        }
    }
}
