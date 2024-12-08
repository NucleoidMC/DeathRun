package io.github.foundationgames.deathrun.game.state;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import io.github.foundationgames.deathrun.game.DeathRunConfig;
import io.github.foundationgames.deathrun.game.map.DeathRunMap;
import io.github.foundationgames.deathrun.game.state.logic.DRItemLogic;
import io.github.foundationgames.deathrun.game.state.logic.DRPlayerLogic;
import io.github.foundationgames.deathrun.util.DRUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.GameRules;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameResult;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.event.GameWaitingLobbyEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.util.ItemStackBuilder;
import xyz.nucleoid.stimuli.event.EventResult;
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

            map.applyFeatures(world);

            DRUtil.setBaseGameRules(game);

            game.listen(GameWaitingLobbyEvents.BUILD_UI_LAYOUT, (layout, player) -> {
                if (cfg.runnersOnly() || game.getGameSpace().getPlayers().spectators().contains(player)) {
                    return;
                }
                layout.addLeading(() -> GuiElementBuilder.from(DRUtil.createRunnerHead())
                        .setName(Text.translatable("item.deathrun.request_runner").styled(style -> style.withColor(0x6bffc1).withItalic(false)))
                        .setCallback(() -> {
                            player.sendMessage(Text.translatable("message.deathrun.requested_runner").formatted(Formatting.GOLD), false);
                            if (waiting.players.get(player) instanceof DRWaiting.Player wp) wp.requestedTeam = DRTeam.RUNNERS;
                            player.swingHand(Hand.MAIN_HAND, true);
                        })
                        .build());
                layout.addLeading(() -> GuiElementBuilder.from(DRUtil.createDeathHead())
                        .setName(Text.translatable("item.deathrun.request_death").styled(style -> style.withColor(0x6bffc1).withItalic(false)))
                        .setCallback(() -> {
                            player.sendMessage(Text.translatable("message.deathrun.requested_death").formatted(Formatting.GOLD), false);
                            if (waiting.players.get(player) instanceof DRWaiting.Player wp) wp.requestedTeam = DRTeam.DEATHS;
                            player.swingHand(Hand.MAIN_HAND, true);
                        })
                        .build());
                layout.addLeading(() -> GuiElementBuilder.from(DRUtil.createClearHead())
                        .setName(Text.translatable("item.deathrun.request_clear").styled(style -> style.withColor(0xff6e42).withItalic(false)))
                        .setCallback(() -> {
                            player.sendMessage(Text.translatable("message.deathrun.cleared_requests").formatted(Formatting.GREEN), false);
                            if (waiting.players.get(player) instanceof DRWaiting.Player wp) wp.requestedTeam = null;
                            player.swingHand(Hand.MAIN_HAND, true);
                        })
                        .build());
            });

            game.listen(GameActivityEvents.REQUEST_START, waiting::requestStart);
            game.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
            game.listen(GamePlayerEvents.ACCEPT, waiting.players::acceptWaiting);
            game.listen(GamePlayerEvents.LEAVE, waiting.players::onLeave);
            game.listen(PlayerDeathEvent.EVENT, (player, source) -> {
                player.setHealth(20f);
                waiting.players.resetWaiting(player);
                return EventResult.DENY;
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
