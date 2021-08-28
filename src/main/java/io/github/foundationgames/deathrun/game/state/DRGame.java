package io.github.foundationgames.deathrun.game.state;

import com.google.common.collect.Lists;
import io.github.foundationgames.deathrun.game.DeathRunConfig;
import io.github.foundationgames.deathrun.game.map.DeathRunMap;
import io.github.foundationgames.deathrun.game.state.logic.DRItemLogic;
import io.github.foundationgames.deathrun.game.state.logic.DRPlayerLogic;
import io.github.foundationgames.deathrun.util.DRUtil;
import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.Blocks;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

import java.util.List;
import java.util.function.Predicate;

public class DRGame {
    public final ServerWorld world;
    public final GameActivity game;
    public final DeathRunMap map;
    public final DeathRunConfig config;
    public final DRPlayerLogic players;
    private final DRItemLogic items = new DRItemLogic();

    private static final int DEATH_TRAP_COOLDOWN = 10 * 20; // 10 seconds

    private int timer = 10 * 20; // 10 seconds

    public DRGame(DRWaiting waiting) {
        this.world = waiting.world;
        this.game = waiting.game;
        this.map = waiting.map;
        this.config = waiting.config;
        this.players = new DRPlayerLogic(this.world, game, map, config);

        game.listen(ItemUseEvent.EVENT, items::processUse);
    }

    public static void open(GameSpace space, DRWaiting waiting) {
        space.setActivity(game -> {
            var deathRun = new DRGame(waiting);

            DRUtil.setBaseGameRules(game);

            DRPlayerLogic.sortTeams(deathRun.world.random, waiting.players, deathRun.players);
            deathRun.players.forEach(deathRun.players::resetActive);

            deathRun.items.addBehavior("boost", (player, stack, hand) -> {
                // TODO: add boost feather functionality
                return TypedActionResult.consume(stack);
            });

            game.listen(GamePlayerEvents.OFFER, offer -> offer.reject(new TranslatableText("status.deathrun.in_progress")));
            game.listen(GamePlayerEvents.LEAVE, deathRun.players::onLeave);
            game.listen(PlayerDamageEvent.EVENT, (player, source, amount) -> ActionResult.FAIL);
            game.listen(PlayerDeathEvent.EVENT, (player, source) -> {
                player.setHealth(20f);
                deathRun.players.resetWaiting(player);
                return ActionResult.FAIL;
            });
            game.listen(GameActivityEvents.TICK, deathRun::tick);
            game.listen(BlockUseEvent.EVENT, deathRun::useBlock);
            game.listen(GameActivityEvents.TICK, deathRun.players::tick);
        });
    }

    private ActionResult useBlock(ServerPlayerEntity player, Hand hand, BlockHitResult hit) {
        if (this.players.get(player) instanceof Player gamePlayer) {
            if (gamePlayer.team == DRTeam.DEATHS) {
                var pos = hit.getBlockPos();
                var state = world.getBlockState(pos);
                if (state.getBlock() instanceof AbstractButtonBlock button && !state.get(Properties.POWERED)) {
                    var deathTrap = map.deathTraps.get(pos);
                    if (deathTrap != null) {
                        world.setBlockState(pos, state.with(Properties.POWERED, true));
                        world.getBlockTickScheduler().schedule(pos, button, DEATH_TRAP_COOLDOWN);
                        deathTrap.deathTrap.trigger(world, deathTrap.bounds);
                        // TODO: death trap resetting
                        return ActionResult.SUCCESS;
                    }
                }
            }
        }
        return ActionResult.PASS;
    }

    public void openGate() {
        for (BlockPos pos : map.gate) {
            if (world.getBlockState(pos).isOf(Blocks.IRON_BARS)) world.removeBlock(pos, false);
        }
    }

    public void tick() {
        if (timer > 0) {
            if (timer % 20 == 0) {
                int sec = timer / 20;
                var format = sec <= 3 ? Formatting.GREEN : Formatting.DARK_GREEN;
                players.showTitle(new LiteralText(Integer.toString(sec)).formatted(Formatting.BOLD, format), 19);
            }
            timer--;
            if (timer == 0) {
                players.showTitle(new TranslatableText("title.deathrun.run").formatted(Formatting.BOLD, Formatting.GOLD), 40);
                openGate();
            }
        }
    }

    public static final List<Predicate<Player>> DEATH_CONDITIONS = Lists.newArrayList(
            // Void death
            player -> {
                var serverP = player.getPlayer();
                return serverP.getPos().y < 0;
            },
            // Water death
            player -> {
                var serverP = player.getPlayer();
                var world = serverP.world;
                return world.getBlockState(new BlockPos(serverP.getPos().add(0, 0.65, 0))).isOf(Blocks.WATER);
            },
            // Lightning death
            player -> {
                var serverP = player.getPlayer();
                var world = serverP.world;
                return world.getEntitiesByClass(LightningEntity.class, serverP.getBoundingBox().expand(1, 0, 1), e -> true).size() > 0;
            },
            // Falling hazard death
            player -> {
                var serverP = player.getPlayer();
                var world = serverP.world;
                return world.getEntitiesByClass(FallingBlockEntity.class, serverP.getBoundingBox(),
                        e -> e.getBlockState().isOf(Blocks.POINTED_DRIPSTONE)).size() > 0;
            }
    );

    public static class Player extends DRPlayer {
        public final DRTeam team;

        public Player(ServerPlayerEntity player, DRPlayerLogic logic, DRTeam team) {
            super(player, logic);
            this.team = team;
        }

        @Override
        public void tick() {
            for (var predicate : DEATH_CONDITIONS) {
                if (predicate.test(this)) {
                    logic.resetActive(this.getPlayer());
                }
            }
        }
    }
}
