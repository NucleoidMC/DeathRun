package io.github.foundationgames.deathrun.game.state;

import io.github.foundationgames.deathrun.game.state.logic.DRPlayerLogic;
import net.minecraft.server.level.ServerPlayer;

public abstract class DRPlayer {
    protected final ServerPlayer player;
    protected final DRPlayerLogic logic;

    protected DRPlayer(ServerPlayer player, DRPlayerLogic logic) {
        this.player = player;
        this.logic = logic;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public void tick() {
    }
}
