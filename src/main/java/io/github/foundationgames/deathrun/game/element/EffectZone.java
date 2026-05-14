package io.github.foundationgames.deathrun.game.element;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import xyz.nucleoid.map_templates.BlockBounds;

public record EffectZone(BlockBounds bounds, Effect effect) {

    public record Effect(Identifier id, int amplifier) {
        public static final Codec<Effect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.fieldOf("effect").forGetter(Effect::id),
                Codec.INT.fieldOf("amplifier").forGetter(Effect::amplifier)
        ).apply(instance, Effect::new));

        public MobEffectInstance createEffect() {
            if (!BuiltInRegistries.MOB_EFFECT.containsKey(id)) return null;
            return new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.get(id).get(), 2, amplifier, true, false, true);
        }
    }
}
