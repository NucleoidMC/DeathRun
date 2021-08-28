package io.github.foundationgames.deathrun.game.state;

import io.github.foundationgames.deathrun.game.state.logic.DRPlayerLogic;
import net.minecraft.server.network.ServerPlayerEntity;

public abstract class DRPlayer {
    protected final ServerPlayerEntity player;
    protected final DRPlayerLogic logic;

    protected DRPlayer(ServerPlayerEntity player, DRPlayerLogic logic) {
        this.player = player;
        this.logic = logic;
    }

    public ServerPlayerEntity getPlayer() {
        return player;
    }

    public void tick() {
    }
}
