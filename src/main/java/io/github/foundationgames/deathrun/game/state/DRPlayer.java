package io.github.foundationgames.deathrun.game.state;

import net.minecraft.server.network.ServerPlayerEntity;

public abstract class DRPlayer {
    protected final ServerPlayerEntity player;

    protected DRPlayer(ServerPlayerEntity player) {
        this.player = player;
    }

    public ServerPlayerEntity getPlayer() {
        return player;
    }
}
