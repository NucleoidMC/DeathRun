package io.github.foundationgames.deathrun.game.state;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import io.github.foundationgames.deathrun.game.DeathRunConfig;
import io.github.foundationgames.deathrun.game.map.DeathRunMap;
import io.github.foundationgames.deathrun.game.state.logic.DRItemLogic;
import io.github.foundationgames.deathrun.game.state.logic.DRPlayerLogic;
import io.github.foundationgames.deathrun.util.DRUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.gamerules.GameRules;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
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
    public final ServerLevel level;
    public final GameActivity game;
    public final DeathRunMap map;
    public final DeathRunConfig config;
    public final DRPlayerLogic players;
    private final DRItemLogic items = new DRItemLogic();

    public DRWaiting(ServerLevel level, GameActivity game, DeathRunMap map, DeathRunConfig config) {
        this.level = level;
        this.game = game;
        this.map = map;
        this.config = config;
        this.players = new DRPlayerLogic(this.level, game, map, config);

        game.listen(ItemUseEvent.EVENT, items::processUse);
    }

    public static GameOpenProcedure open(GameOpenContext<DeathRunConfig> ctx) {
        var server = ctx.server();
        var cfg = ctx.config();
        var mapCfg = cfg.map();
        var map = DeathRunMap.create(server, mapCfg);
        //.setTimeOfDay(mapCfg.time())
        var levelCfg = new RuntimeLevelConfig().setGenerator(map.createGenerator(server));

        levelCfg.setGameRule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0);

        return ctx.openWithLevel(levelCfg, (game, level) -> {
            var waiting = new DRWaiting(level, game, map, cfg);

            GameWaitingLobby.addTo(game, cfg.players());

            map.applyFeatures(level);

            DRUtil.setBaseGameRules(game);

            game.listen(GameWaitingLobbyEvents.BUILD_UI_LAYOUT, (layout, player) -> {
                if (cfg.runnersOnly() || game.getGameSpace().getPlayers().spectators().contains(player)) {
                    return;
                }
                layout.addLeading(() -> GuiElementBuilder.from(DRUtil.createRunnerHead())
                        .setName(Component.translatable("item.deathrun.request_runner").withStyle(style -> style.withColor(0x6bffc1).withItalic(false)))
                        .setCallback(() -> {
                            player.sendSystemMessage(Component.translatable("message.deathrun.requested_runner").withStyle(ChatFormatting.GOLD), false);
                            if (waiting.players.get(player) instanceof DRWaiting.Player wp) wp.requestedTeam = DRTeam.RUNNERS;
                            player.swing(InteractionHand.MAIN_HAND, true);
                        })
                        .build());
                layout.addLeading(() -> GuiElementBuilder.from(DRUtil.createDeathHead())
                        .setName(Component.translatable("item.deathrun.request_death").withStyle(style -> style.withColor(0x6bffc1).withItalic(false)))
                        .setCallback(() -> {
                            player.sendSystemMessage(Component.translatable("message.deathrun.requested_death").withStyle(ChatFormatting.GOLD), false);
                            if (waiting.players.get(player) instanceof DRWaiting.Player wp) wp.requestedTeam = DRTeam.DEATHS;
                            player.swing(InteractionHand.MAIN_HAND, true);
                        })
                        .build());
                layout.addLeading(() -> GuiElementBuilder.from(DRUtil.createClearHead())
                        .setName(Component.translatable("item.deathrun.request_clear").withStyle(style -> style.withColor(0xff6e42).withItalic(false)))
                        .setCallback(() -> {
                            player.sendSystemMessage(Component.translatable("message.deathrun.cleared_requests").withStyle(ChatFormatting.GREEN), false);
                            if (waiting.players.get(player) instanceof DRWaiting.Player wp) wp.requestedTeam = null;
                            player.swing(InteractionHand.MAIN_HAND, true);
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

        public Player(ServerPlayer player, DRPlayerLogic logic) {
            super(player, logic);
        }
    }
}
