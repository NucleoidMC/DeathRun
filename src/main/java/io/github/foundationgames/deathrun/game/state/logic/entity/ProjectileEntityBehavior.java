package io.github.foundationgames.deathrun.game.state.logic.entity;

import io.github.foundationgames.deathrun.game.state.DRGame;
import io.github.foundationgames.deathrun.mixin.AbstractArrowAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;

public abstract class ProjectileEntityBehavior<P extends Projectile> extends EntityBehavior<P> {
    @Override
    public void tick(P entity, DRGame game) {
        if (entity instanceof AbstractArrow proj && ((AbstractArrowAccess)proj).deathrun$inGround()) {
            onHitBlock(entity, game);
        }
    }

    public void onHitBlock(P entity, DRGame game) {
        entity.remove(Entity.RemovalReason.DISCARDED);
    }

    public static class Arrow extends ProjectileEntityBehavior<net.minecraft.world.entity.projectile.arrow.Arrow> {
        @Override
        public Class<net.minecraft.world.entity.projectile.arrow.Arrow> getEntityClass() {
            return net.minecraft.world.entity.projectile.arrow.Arrow.class;
        }
    }
}
