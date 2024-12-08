package io.github.foundationgames.deathrun.game.element;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public record MapText(Vec3d pos, TextData text) {
    public record TextData(List<Text> lines) {
        public static final Codec<Text> JSON_TEXT_CODEC = TextCodecs.STRINGIFIED_CODEC;

        public static final Codec<TextData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.list(JSON_TEXT_CODEC).fieldOf("lines").forGetter(TextData::lines)
        ).apply(instance, TextData::new));
    }
}
