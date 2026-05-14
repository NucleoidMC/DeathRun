package io.github.foundationgames.deathrun.game.state.logic;

import io.github.foundationgames.deathrun.game.DeathRunConfig;
import io.github.foundationgames.deathrun.game.map.DeathRunMap;
import io.github.foundationgames.deathrun.game.state.DRGame;
import io.github.foundationgames.deathrun.game.state.DRPlayer;
import io.github.foundationgames.deathrun.game.state.DRTeam;
import io.github.foundationgames.deathrun.game.state.DRWaiting;
import io.github.foundationgames.deathrun.util.DRUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinIntent;
import xyz.nucleoid.plasmid.api.game.player.PlayerSet;
import xyz.nucleoid.plasmid.api.util.ItemStackBuilder;

import java.util.*;

public class DRPlayerLogic implements PlayerSet {
    private final ServerLevel level;
    private final GameActivity game;
    private final DeathRunMap map;
    private final DeathRunConfig config;
    private final Map<ServerPlayer, DRPlayer> players = new HashMap<>();

    public DRPlayerLogic(ServerLevel level, GameActivity game, DeathRunMap map, DeathRunConfig config) {
        this.level = level;
        this.game = game;
        this.map = map;
        this.config = config;
    }

    public Collection<DRPlayer> getPlayers() {
        return players.values();
    }

    public List<DRPlayer> getPlayers(RandomSource random) {
        var list = new ObjectArrayList<>(getPlayers());
        Util.shuffle(list, random);
        return list;
    }

    public void resetWaiting(ServerPlayer player) {
        var spawn = map.spawn;
        var min = spawn.min();
        var max = spawn.max();
        var x = min.getX() + level.getRandom().nextInt(max.getX() - min.getX()) + 0.5;
        var z = min.getZ() + level.getRandom().nextInt(max.getZ() - min.getZ()) + 0.5;
        player.teleportTo(level, x, min.getY(), z, Set.of(), 0f, 0f, false);
        player.setGameMode(GameType.ADVENTURE);
    }

    public void resetActive(ServerPlayer player) {
        var pl = get(player);

        if (pl instanceof DRGame.Player gamePlayer) {
            var spawn = map.deathStart;
            float spawnYaw = 0;
            boolean randomPos = true;
            if (gamePlayer.team == DRTeam.RUNNERS && !gamePlayer.isFinished()) {
                var checkpoint = gamePlayer.getCheckpoint();
                if (checkpoint == null) spawn = map.runnerStart;
                else {
                    spawn = checkpoint.bounds();
                    spawnYaw = checkpoint.yaw();
                    randomPos = false;
                }
            }
            double x;
            double z;
            if (randomPos) {
                var min = spawn.min();
                var max = spawn.max();
                x = min.getX() + level.getRandom().nextInt(max.getX() - min.getX()) + 0.5;
                z = min.getZ() + level.getRandom().nextInt(max.getZ() - min.getZ()) + 0.5;
            } else {
                var center = spawn.center();
                x = center.x;
                z = center.z;
            }
            player.teleportTo(level, x, spawn.min().getY(), z, Set.of(), spawnYaw, 0f, false);
            pl.getPlayer().getInventory().clearContent();
            if (gamePlayer.team == DRTeam.RUNNERS && !gamePlayer.isFinished()) {
                var boostItem = ItemStackBuilder.of(Items.FEATHER)
                        .setName(Component.translatable("item.deathrun.boost_feather").withStyle(style -> style.withColor(0x9ce3ff).withItalic(false))).build();
                DRItemLogic.apply("boost", boostItem);

                player.getInventory().setItem(0, boostItem);

                if (gamePlayer.game.config.runnersOnly()) {
                    var activatorItem = ItemStackBuilder.of(Items.TRIDENT)
                            .setName(Component.translatable("item.deathrun.activator_trident").withStyle(style -> style.withColor(0xffe747).withItalic(false))).build();
                    DRItemLogic.apply("activator", activatorItem);
                    player.getInventory().setItem(1, activatorItem);
                }
            }
        }
        player.setGameMode(GameType.ADVENTURE);
    }

    public void resetSpectator(ServerPlayer player) {
        player.setGameMode(GameType.SPECTATOR);
    }

    public static void sortTeams(RandomSource random, DRPlayerLogic waiting, DRGame game) {
        var gamePlayers = game.players;
        var waitingPlayers = waiting.getPlayers(random);
        // Runners only team sorting (put everyone on runners team)
        if (game.config.runnersOnly()) {
            for (var player : waitingPlayers) {
                gamePlayers.add(new DRGame.Player(player.getPlayer(), gamePlayers, DRTeam.RUNNERS, game));
            }
            return;
        }
        // Normal team sorting (distribute runners and deaths evenly, based on requests as well)
        int maxDeaths = Math.min(3, (int)Math.ceil(waitingPlayers.size() * 0.17));
        var runners = new ArrayList<DRWaiting.Player>();
        var deaths = new ArrayList<DRWaiting.Player>();
        for (var p : waitingPlayers) {
            if (p instanceof DRWaiting.Player player) {
                if (player.requestedTeam == DRTeam.DEATHS && deaths.size() < maxDeaths) {
                    deaths.add(player);
                } else if (player.requestedTeam == DRTeam.RUNNERS) {
                    runners.add(player);
                }
            }
        }
        for (var p : waitingPlayers) {
            if (p instanceof DRWaiting.Player player &&
                    (!deaths.contains(player) && !runners.contains(player))
            ) {
                if (deaths.size() < maxDeaths) {
                    deaths.add(player);
                } else {
                    runners.add(player);
                }
            }
        }
        for (var player : runners) {
            gamePlayers.add(new DRGame.Player(player.getPlayer(), gamePlayers, DRTeam.RUNNERS, game));
        }
        for (var player : deaths) {
            gamePlayers.add(new DRGame.Player(player.getPlayer(), gamePlayers, DRTeam.DEATHS, game));
        }
    }

    public void tick() {
        this.getPlayers().forEach(DRPlayer::tick);
    }

    public void onLeave(ServerPlayer player) {
        this.players.remove(player);
    }

    public JoinAcceptorResult acceptWaiting(JoinAcceptor offer) {
        return offer.teleport(level, map.spawn.centerBottom())
                .thenRunForEach((player, intent) -> {
                    if (intent == JoinIntent.PLAY) {
                        this.add(new DRWaiting.Player(player, this));
                    }
                    this.resetWaiting(player);
                });
    }


    public JoinAcceptorResult acceptSpectator(JoinAcceptor offer) {
        return offer.teleport(level, map.spawn.centerBottom())
                .thenRunForEach((player, intent) -> {
                    this.resetSpectator(player);
                });
    }

    public void remove(DRPlayer player) {
        this.players.remove(player.getPlayer());
    }

    public void add(DRPlayer player) {
        this.players.put(player.getPlayer(), player);
    }

    public DRPlayer get(ServerPlayer player) {
        return this.players.get(player);
    }

    @Override
    public boolean contains(UUID id) {
        var player = level.getPlayerByUUID(id);
        return player instanceof ServerPlayer && players.containsKey(player);
    }

    @Override
    public @Nullable ServerPlayer getEntity(UUID id) {
        var player = level.getPlayerByUUID(id);
        return player instanceof ServerPlayer sPlayer ? sPlayer : null;
    }

    @Override
    public int size() {
        return players.size();
    }

    @Override
    public Iterator<ServerPlayer> iterator() {
        return players.keySet().iterator();
    }
}
