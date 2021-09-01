package io.github.foundationgames.deathrun.game.element;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public record MapText(Vec3d pos, TextData text) {
    public record TextData(List<MutableText> lines) {
        public static final Codec<MutableText> JSON_TEXT_CODEC = new PrimitiveCodec<>() {
            @Override
            public <T> DataResult<MutableText> read(DynamicOps<T> ops, T input) {
                return ops.getStringValue(input).map(Text.Serializer::fromJson);
            }

            @Override
            public <T> T write(DynamicOps<T> ops, MutableText value) {
                return ops.createString(Text.Serializer.toJson(value));
            }
        };

        public static final Codec<TextData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.list(JSON_TEXT_CODEC).fieldOf("lines").forGetter(TextData::lines)
        ).apply(instance, TextData::new));
    }
}
