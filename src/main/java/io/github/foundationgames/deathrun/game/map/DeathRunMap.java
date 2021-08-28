package io.github.foundationgames.deathrun.game.map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.foundationgames.deathrun.game.deathtrap.DeathTrap;
import io.github.foundationgames.deathrun.util.DRUtil;
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
import java.util.concurrent.atomic.AtomicReference;

public class DeathRunMap {
    public final MapTemplate template;
    public final Map<BlockPos, DeathTrapZone> deathTraps;
    public final List<BlockBounds> checkpoints;
    public final BlockBounds spawn;
    public final BlockBounds runnerStart;
    public final BlockBounds deathStart;
    public final BlockBounds gate;
    public final BlockBounds finish;
    public final int time;

    public DeathRunMap(MapTemplate template, Map<BlockPos, DeathTrapZone> deathTraps, List<BlockBounds> checkpoints, BlockBounds spawn, BlockBounds runnerStart, BlockBounds deathStart, BlockBounds gate, BlockBounds finish, int time) {
        this.template = template;
        this.deathTraps = deathTraps;
        this.checkpoints = checkpoints;
        this.spawn = spawn;
        this.runnerStart = runnerStart;
        this.deathStart = deathStart;
        this.gate = gate;
        this.finish = finish;
        this.time = time;
    }

    public static DeathRunMap create(MinecraftServer server, DRMapConfig cfg) throws GameOpenException {
        try {
            MapTemplate template = MapTemplateSerializer.loadFromResource(server, cfg.mapId());

            var deathTraps = ImmutableMap.<BlockPos, DeathTrapZone>builder();

            // Amazing error handling for "throwing" inside the lambda because
            // I didn't want to collect the stream and do a normal for each
            AtomicReference<GameOpenException> error = new AtomicReference<>();

            template.getMetadata().getRegions("death_trap").forEach(reg -> {
                var data = reg.getData();
                try {
                    deathTraps.put(DRUtil.blockPos(data.getCompound("button")), new DeathTrapZone(reg, data.getString("type")));
                } catch (IllegalStateException ex) {
                    error.set(new GameOpenException(new LiteralText(ex.getMessage())));
                }
            });

            if (error.get() != null) {
                throw error.get();
            }

            var checkpoints = ImmutableList.<BlockBounds>builder();
            template.getMetadata().getRegions("checkpoint").forEach(reg -> checkpoints.add(reg.getBounds()));

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

            return new DeathRunMap(template, builtDeathTraps, checkpoints.build(), spawn, runnerStart, deathStart, gate, finish, cfg.time());
        } catch (IOException e) {
            throw new GameOpenException(new LiteralText(String.format("Map %s was not found", cfg.mapId())));
        }
    }

    public ChunkGenerator createGenerator(MinecraftServer server) {
        return new TemplateChunkGenerator(server, template);
    }

    public static class DeathTrapZone {
        public final BlockBounds bounds;
        public final DeathTrap deathTrap;

        public DeathTrapZone(TemplateRegion zone, String type) {
            this.bounds = zone.getBounds();
            this.deathTrap = DeathTrap.get(type);
        }
    }
}
