package io.github.foundationgames.deathrun.game.state.logic.entity;

import io.github.foundationgames.deathrun.game.element.DeathTrapZone;
import io.github.foundationgames.deathrun.game.state.DRGame;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;

import java.util.concurrent.atomic.AtomicReference;

public class ActivatorTridentEntityBehavior extends ProjectileEntityBehavior<ThrownTrident> {
    @Override
    public void onHitBlock(ThrownTrident entity, DRGame game) {
        AtomicReference<DeathTrapZone> nearestZone = new AtomicReference<>();
        var ePos = entity.position();
        game.map.trapZones.forEach((pos, zone) -> {
            if (nearestZone.get() == null || zone.getZone().center().distanceTo(ePos) < nearestZone.get().getZone().center().distanceTo(ePos)) {
                nearestZone.set(zone);
            }
        });
        if (nearestZone.get() != null) {
            game.trigger(nearestZone.get());
        }
        super.onHitBlock(entity, game);
    }

    @Override
    public Class<ThrownTrident> getEntityClass() {
        return ThrownTrident.class;
    }
}
