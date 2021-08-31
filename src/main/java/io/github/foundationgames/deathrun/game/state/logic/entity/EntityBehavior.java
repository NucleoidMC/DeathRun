package io.github.foundationgames.deathrun.game.state.logic.entity;

import io.github.foundationgames.deathrun.game.state.DRGame;
import net.minecraft.entity.Entity;

public abstract class EntityBehavior<E extends Entity> {

    public abstract Class<E> getEntityClass();

    public void tick(E entity, DRGame game) {}
}
