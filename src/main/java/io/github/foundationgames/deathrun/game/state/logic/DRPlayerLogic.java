package io.github.foundationgames.deathrun.game.state.logic;

import com.google.common.collect.Lists;
import io.github.foundationgames.deathrun.game.DeathRunConfig;
import io.github.foundationgames.deathrun.game.map.DeathRunMap;
import io.github.foundationgames.deathrun.game.state.DRGame;
import io.github.foundationgames.deathrun.game.state.DRPlayer;
import io.github.foundationgames.deathrun.game.state.DRTeam;
import io.github.foundationgames.deathrun.game.state.DRWaiting;
import io.github.foundationgames.deathrun.util.DRUtil;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

import java.util.*;

public class DRPlayerLogic {
    private final ServerWorld world;
    private final GameActivity game;
    private final DeathRunMap map;
    private final DeathRunConfig config;
    private final Map<ServerPlayerEntity, DRPlayer> players = new HashMap<>();

    public DRPlayerLogic(ServerWorld world, GameActivity game, DeathRunMap map, DeathRunConfig config) {
        this.world = world;
        this.game = game;
        this.map = map;
        this.config = config;
    }

    public Collection<DRPlayer> getPlayers() {
        return players.values();
    }

    public List<DRPlayer> getPlayers(Random random) {
        var list = Lists.newArrayList(getPlayers());
        Collections.shuffle(list, random);
        return list;
    }

    public void resetWaiting(ServerPlayerEntity player) {
        var spawn = map.spawn;
        var min = spawn.min();
        var max = spawn.max();
        var x = min.getX() + world.random.nextInt(max.getX() - min.getX()) + 0.5;
        var z = min.getZ() + world.random.nextInt(max.getZ() - min.getZ()) + 0.5;
        player.teleport(world, x, min.getY(), z, 0f, 0f);

        var leaveItem = ItemStackBuilder.of(Items.MAGENTA_GLAZED_TERRACOTTA)
                .setName(new TranslatableText("item.deathrun.leave_game").styled(style -> style.withColor(0x896bff).withItalic(false))).build();
        DRItemLogic.apply("leave_game", leaveItem);

        if (!config.runnersOnly()) {
            var runnerItem = ItemStackBuilder.of(DRUtil.createRunnerHead())
                    .setName(new TranslatableText("item.deathrun.request_runner").styled(style -> style.withColor(0x6bffc1).withItalic(false))).build();
            DRItemLogic.apply("request_runner", runnerItem);

            var deathItem = ItemStackBuilder.of(DRUtil.createDeathHead())
                    .setName(new TranslatableText("item.deathrun.request_death").styled(style -> style.withColor(0x6bffc1).withItalic(false))).build();
            DRItemLogic.apply("request_death", deathItem);

            var clearItem = ItemStackBuilder.of(DRUtil.createClearHead())
                    .setName(new TranslatableText("item.deathrun.request_clear").styled(style -> style.withColor(0xff6e42).withItalic(false))).build();
            DRItemLogic.apply("request_clear", clearItem);

            player.getInventory().setStack(3, runnerItem);
            player.getInventory().setStack(4, clearItem);
            player.getInventory().setStack(5, deathItem);
        } else {
            var runnerItem = ItemStackBuilder.of(DRUtil.createRunnerHeadB())
                    .setName(new TranslatableText("item.deathrun.runners_only").styled(style -> style.withColor(0xffca38).withItalic(false))).build();
            player.getInventory().setStack(4, runnerItem);
        }
        player.getInventory().setStack(8, leaveItem);
        player.changeGameMode(GameMode.ADVENTURE);
    }

    public static void sortTeams(Random random, DRPlayerLogic waiting, DRPlayerLogic game) {
        var waitingPlayers = waiting.getPlayers(random);
        int maxDeaths = Math.min(3, (int)(waitingPlayers.size() * 0.17));
        var runners = new ArrayList<DRWaiting.Player>();
        var deaths = new ArrayList<DRWaiting.Player>();
        for (DRPlayer p : waitingPlayers) {
            if (p instanceof DRWaiting.Player player) {
                if (player.requestedTeam == DRTeam.DEATHS && deaths.size() < maxDeaths) {
                    deaths.add(player);
                } else if (player.requestedTeam == DRTeam.RUNNERS) {
                    runners.add(player);
                }
            }
        }
        for (DRPlayer p : waitingPlayers) {
            if (p instanceof DRWaiting.Player player &&
                    (!deaths.contains(player) && !runners.contains(player))
            ) {
                var death = random.nextBoolean();
                if (death && deaths.size() < maxDeaths) {
                    deaths.add(player);
                } else {
                    runners.add(player);
                }
            }
        }
        for (DRWaiting.Player player : runners) {
            game.add(new DRGame.Player(player.getPlayer(), DRTeam.RUNNERS));
        }
        for (DRWaiting.Player player : deaths) {
            game.add(new DRGame.Player(player.getPlayer(), DRTeam.DEATHS));
        }
    }

    public void onRemove(ServerPlayerEntity player) {
        this.players.remove(player);
    }

    public PlayerOfferResult offerWaiting(PlayerOffer offer) {
        this.add(new DRWaiting.Player(offer.player()));
        return offer.accept(world, map.spawn.centerBottom())
                .and(() -> this.resetWaiting(offer.player()));
    }

    public void remove(DRPlayer player) {
        this.players.remove(player.getPlayer());
    }

    public void add(DRPlayer player) {
        this.players.put(player.getPlayer(), player);
    }

    public DRPlayer get(ServerPlayerEntity player) {
        return this.players.get(player);
    }
}
