package io.github.foundationgames.deathrun.game.state;

import net.minecraft.server.network.ServerPlayerEntity;

public class DRGame {
    // TODO: add deathrun

    public static class Player extends DRPlayer {
        private final DRTeam team;

        public Player(ServerPlayerEntity player, DRTeam team) {
            super(player);
            this.team = team;
        }
    }
}
