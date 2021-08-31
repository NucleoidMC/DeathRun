package io.github.foundationgames.deathrun.game.state.logic.entity;

import io.github.foundationgames.deathrun.DeathRun;
import io.github.foundationgames.deathrun.game.state.DRGame;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DREntityLogic {
    private final Map<Integer, EntityBehavior<Entity>> active = new HashMap<>();
    private final Set<Integer> idCache = new HashSet<>();
    private final ServerWorld world;
    private final DRGame game;

    public DREntityLogic(ServerWorld world, DRGame game) {
        this.world = world;
        this.game = game;
    }

    @SuppressWarnings("unchecked")
    public <E extends Entity> void attach(E entity, EntityBehavior<E> behavior) {
        active.put(entity.getId(), (EntityBehavior<Entity>)behavior);
    }

    public void tick() {
        idCache.clear();
        idCache.addAll(active.keySet());
        for (int id : idCache) {
            var entity = world.getEntityById(id);
            if (entity == null || entity.isRemoved()) {
                active.remove(id);
                continue;
            }
            var behavior = active.get(id);
            if (behavior.getEntityClass() == entity.getClass()) {
                behavior.tick(entity, game);
            } else {
                active.remove(id);
                DeathRun.LOG.error("Cannot tick entity of class {} (id {}) with behavior of incompatible entity class {}, discarding", entity.getClass(), id, behavior.getEntityClass());
            }
        }
    }
}
