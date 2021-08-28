package io.github.foundationgames.deathrun.game.deathtrap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.map_templates.BlockBounds;

public class DeathTrapZone {
    public static final Codec<DeathTrapZone> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("button").forGetter(trap -> trap.button),
            DeathTraps.CODEC.forGetter(trap -> trap.trap)
    ).apply(instance, DeathTrapZone::new));

    private final BlockPos button;
    private final DeathTrap trap;

    private BlockBounds zone;

    public DeathTrapZone(BlockPos trigger, DeathTrap trap) {
        this.button = trigger;
        this.trap = trap;
    }

    public BlockBounds getZone() {
        return zone;
    }

    public BlockPos getButton() {
        return button;
    }

    public DeathTrap getTrap() {
        return trap;
    }

    public void setZone(BlockBounds zone) {
        this.zone = zone;
    }
}
