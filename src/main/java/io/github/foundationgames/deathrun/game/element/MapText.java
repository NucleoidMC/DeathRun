package io.github.foundationgames.deathrun.game.element;

import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public record MapText(Vec3 pos, TextData text) {
    private static final HolderLookup.Provider LOOKUP = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    public record TextData(List<Component> lines) {
        public static final Codec<Component> JSON_TEXT_CODEC = new Codec<Component>() {
            @Override
            public <T> DataResult<Pair<Component, T>> decode(DynamicOps<T> ops, T input) {
                var decoded = ComponentSerialization.CODEC.decode(ops, input);

                if (decoded.isSuccess()) {
                    var val = decoded.getOrThrow().getFirst();
                    if (val.getSiblings().isEmpty() && val.getStyle().isEmpty() && val.getContents() instanceof PlainTextContents.LiteralContents literal) {
                        if (literal.text().length() > 2 && literal.text().charAt(0) == '"' && literal.text().charAt(literal.text().length() - 1) == '"') {
                            return DataResult.success(new Pair<>(Component.literal(literal.text().substring(1, literal.text().length() - 2)), decoded.getOrThrow().getSecond()));
                        } else if (literal.text().length() > 2 && literal.text().charAt(0) == '{' && literal.text().charAt(literal.text().length() - 1) == '}') {
                            try {
                                var json = JsonParser.parseString(literal.text());
                                return DataResult.success(new Pair<>(ComponentSerialization.CODEC.decode(ops instanceof RegistryOps<T> registryOps ? registryOps.withParent(JsonOps.INSTANCE) : JsonOps.INSTANCE, json).getOrThrow().getFirst(), decoded.getOrThrow().getSecond()));
                            } catch (Throwable throwable) {
                                // ignored
                            }

                        }
                    }
                }

                return decoded;
            }

            @Override
            public <T> DataResult<T> encode(Component input, DynamicOps<T> ops, T prefix) {
                return ComponentSerialization.CODEC.encode(input.copy().setStyle(input.getStyle().withInsertion("")), ops, prefix);
            }
        };

        public static final Codec<TextData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.list(JSON_TEXT_CODEC).fieldOf("lines").forGetter(TextData::lines)
        ).apply(instance, TextData::new));
    }
}
