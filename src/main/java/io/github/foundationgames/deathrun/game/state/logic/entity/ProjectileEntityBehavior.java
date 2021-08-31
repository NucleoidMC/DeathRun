package io.github.foundationgames.deathrun.game.state.logic.entity;

import io.github.foundationgames.deathrun.game.state.DRGame;
import io.github.foundationgames.deathrun.mixin.PersistentProjectileEntityAccess;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;

public abstract class ProjectileEntityBehavior<P extends ProjectileEntity> extends EntityBehavior<P> {
    @Override
    public void tick(P entity, DRGame game) {
        if (entity instanceof PersistentProjectileEntity proj && ((PersistentProjectileEntityAccess)proj).deathrun$inGround()) {
            entity.remove(Entity.RemovalReason.DISCARDED);
        }
    }

    public static class Arrow extends ProjectileEntityBehavior<ArrowEntity> {
        @Override
        public Class<ArrowEntity> getEntityClass() {
            return ArrowEntity.class;
        }
    }
}
