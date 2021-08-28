package io.github.foundationgames.deathrun.game.state;

import io.github.foundationgames.deathrun.game.DeathRunConfig;
import io.github.foundationgames.deathrun.game.map.DeathRunMap;
import io.github.foundationgames.deathrun.game.state.logic.DRItemLogic;
import io.github.foundationgames.deathrun.game.state.logic.DRPlayerLogic;
import io.github.foundationgames.deathrun.util.DRUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.GameRules;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameResult;
import xyz.nucleoid.plasmid.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class DRWaiting {
    public final ServerWorld world;
    public final GameActivity game;
    public final DeathRunMap map;
    public final DeathRunConfig config;
    public final DRPlayerLogic players;
    private final DRItemLogic items = new DRItemLogic();

    public DRWaiting(ServerWorld world, GameActivity game, DeathRunMap map, DeathRunConfig config) {
        this.world = world;
        this.game = game;
        this.map = map;
        this.config = config;
        this.players = new DRPlayerLogic(this.world, game, map, config);

        game.listen(ItemUseEvent.EVENT, items::processUse);
    }

    public static GameOpenProcedure open(GameOpenContext<DeathRunConfig> ctx) {
        var server = ctx.server();
        var cfg = ctx.config();
        var mapCfg = cfg.map();
        var map = DeathRunMap.create(server, mapCfg);
        var worldCfg = new RuntimeWorldConfig().setTimeOfDay(mapCfg.time()).setGenerator(map.createGenerator(server));

        worldCfg.setGameRule(GameRules.DO_FIRE_TICK, false);

        return ctx.openWithWorld(worldCfg, (game, world) -> {
            var waiting = new DRWaiting(world, game, map, cfg);

            GameWaitingLobby.addTo(game, cfg.players());

            DRUtil.setBaseGameRules(game);

            waiting.items.addBehavior("leave_game", (player, stack, hand) -> {
                player.sendMessage(new TranslatableText("message.deathrun.left_game").formatted(Formatting.RED), false);
                game.getGameSpace().kickPlayer(player);
                return TypedActionResult.success(stack);
            });

            waiting.items.addBehavior("request_runner", (player, stack, hand) -> {
                player.sendMessage(new TranslatableText("message.deathrun.requested_runner").formatted(Formatting.GOLD), false);
                if (waiting.players.get(player) instanceof DRWaiting.Player wp) wp.requestedTeam = DRTeam.RUNNERS;
                return TypedActionResult.success(stack);
            });

            waiting.items.addBehavior("request_death", (player, stack, hand) -> {
                player.sendMessage(new TranslatableText("message.deathrun.requested_death").formatted(Formatting.GOLD), false);
                if (waiting.players.get(player) instanceof DRWaiting.Player wp) wp.requestedTeam = DRTeam.DEATHS;
                return TypedActionResult.success(stack);
            });

            waiting.items.addBehavior("request_clear", (player, stack, hand) -> {
                player.sendMessage(new TranslatableText("message.deathrun.cleared_requests").formatted(Formatting.GREEN), false);
                if (waiting.players.get(player) instanceof DRWaiting.Player wp) wp.requestedTeam = null;
                return TypedActionResult.success(stack);
            });

            game.listen(GameActivityEvents.REQUEST_START, waiting::requestStart);
            game.listen(GamePlayerEvents.OFFER, waiting.players::offerWaiting);
            game.listen(GamePlayerEvents.LEAVE, waiting.players::onLeave);
            game.listen(PlayerDeathEvent.EVENT, (player, source) -> {
                player.setHealth(20f);
                waiting.players.resetWaiting(player);
                return ActionResult.FAIL;
            });
            game.listen(GameActivityEvents.TICK, waiting.players::tick);
        });
    }

    private GameResult requestStart() {
        DRGame.open(game.getGameSpace(), this);
        return GameResult.ok();
    }

    public static class Player extends DRPlayer {
        public DRTeam requestedTeam = null;

        public Player(ServerPlayerEntity player, DRPlayerLogic logic) {
            super(player, logic);
        }
    }
}
