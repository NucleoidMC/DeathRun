package io.github.foundationgames.deathrun.game.map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import io.github.foundationgames.deathrun.game.element.CheckpointZone;
import io.github.foundationgames.deathrun.game.element.DeathTrapZone;
import io.github.foundationgames.deathrun.game.element.EffectZone;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTemplateSerializer;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.game.GameOpenException;
import xyz.nucleoid.plasmid.game.world.generator.TemplateChunkGenerator;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DeathRunMap {
    public final MapTemplate template;
    public final Map<BlockPos, DeathTrapZone> trapZones;
    public final List<CheckpointZone> checkpoints;
    public final List<EffectZone> effectZones;
    public final BlockBounds spawn;
    public final BlockBounds runnerStart;
    public final BlockBounds deathStart;
    public final BlockBounds gate;
    public final BlockBounds finish;
    public final int time;

    public DeathRunMap(MapTemplate template, Map<BlockPos, DeathTrapZone> deathTraps, List<CheckpointZone> checkpoints, List<EffectZone> effectZones, BlockBounds spawn, BlockBounds runnerStart, BlockBounds deathStart, BlockBounds gate, BlockBounds finish, int time) {
        this.template = template;
        this.trapZones = deathTraps;
        this.checkpoints = checkpoints;
        this.effectZones = effectZones;
        this.spawn = spawn;
        this.runnerStart = runnerStart;
        this.deathStart = deathStart;
        this.gate = gate;
        this.finish = finish;
        this.time = time;
    }

    public static DeathRunMap create(MinecraftServer server, DRMapConfig cfg) throws GameOpenException {
        MapTemplate template;
        try {
            template = MapTemplateSerializer.loadFromResource(server, cfg.mapId());
        } catch (IOException e) {
            throw new GameOpenException(new LiteralText(String.format("Map %s was not found", cfg.mapId())));
        }

        var deathTraps = ImmutableMap.<BlockPos, DeathTrapZone>builder();

        for (TemplateRegion reg : template.getMetadata().getRegions("death_trap").collect(Collectors.toList())) {
            DataResult<DeathTrapZone> result = DeathTrapZone.CODEC.decode(NbtOps.INSTANCE, reg.getData()).map(Pair::getFirst);

            result.result().ifPresent(deathTrapZone -> {
                deathTrapZone.setZone(reg.getBounds());
                deathTraps.put(deathTrapZone.getButton(), deathTrapZone);
            });
            result.error().ifPresent(ex -> {
                throw new GameOpenException(new LiteralText("Failed to decode death trap zone: " + ex));
            });
        }

        var effectZones = ImmutableList.<EffectZone>builder();

        for (TemplateRegion reg : template.getMetadata().getRegions("effect_zone").collect(Collectors.toList())) {
            DataResult<EffectZone.Effect> result = EffectZone.Effect.CODEC.decode(NbtOps.INSTANCE, reg.getData()).map(Pair::getFirst);

            result.result().ifPresent(effect -> effectZones.add(new EffectZone(reg.getBounds(), effect)));
            result.error().ifPresent(ex -> {
                throw new GameOpenException(new LiteralText("Failed to decode effect zone data: " + ex));
            });
        }

        var checkpoints = ImmutableList.<CheckpointZone>builder();
        template.getMetadata().getRegions("checkpoint").forEach(reg -> {
            var bounds = reg.getBounds();
            float yaw = 0;
            if (reg.getData().contains("yaw")) yaw = reg.getData().getFloat("yaw");
            checkpoints.add(new CheckpointZone(bounds, yaw));
        });

        var spawn = template.getMetadata().getFirstRegionBounds("spawn");
        var runnerStart = template.getMetadata().getFirstRegionBounds("runner_start");
        var deathStart = template.getMetadata().getFirstRegionBounds("death_start");
        var gate = template.getMetadata().getFirstRegionBounds("gate");
        var finish = template.getMetadata().getFirstRegionBounds("finish");

        if (spawn == null) throw new GameOpenException(new LiteralText("Missing spawn region!"));
        if (runnerStart == null) throw new GameOpenException(new LiteralText("Missing runner_start region!"));
        if (deathStart == null) throw new GameOpenException(new LiteralText("Missing death_start region!"));
        if (gate == null) throw new GameOpenException(new LiteralText("Missing gate region!"));
        if (finish == null) throw new GameOpenException(new LiteralText("Missing finish region!"));

        Map<BlockPos, DeathTrapZone> builtDeathTraps;
        try {
            builtDeathTraps = deathTraps.build();
        } catch (IllegalArgumentException ex) {
            throw new GameOpenException(new LiteralText("Two death zones may not share the same button"));
        }

        return new DeathRunMap(template, builtDeathTraps, checkpoints.build(), effectZones.build(), spawn, runnerStart, deathStart, gate, finish, cfg.time());
    }

    public ChunkGenerator createGenerator(MinecraftServer server) {
        return new TemplateChunkGenerator(server, template);
    }
}
