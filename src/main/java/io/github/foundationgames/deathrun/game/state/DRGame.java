package io.github.foundationgames.deathrun.game.state;

import com.google.common.collect.Lists;
import io.github.foundationgames.deathrun.game.DeathRunConfig;
import io.github.foundationgames.deathrun.game.element.CheckpointZone;
import io.github.foundationgames.deathrun.game.element.DeathTrapZone;
import io.github.foundationgames.deathrun.game.element.EffectZone;
import io.github.foundationgames.deathrun.game.element.deathtrap.ResettingDeathTrap;
import io.github.foundationgames.deathrun.game.map.DeathRunMap;
import io.github.foundationgames.deathrun.game.state.logic.DRItemLogic;
import io.github.foundationgames.deathrun.game.state.logic.DRPlayerLogic;
import io.github.foundationgames.deathrun.game.state.logic.entity.ActivatorTridentEntityBehavior;
import io.github.foundationgames.deathrun.game.state.logic.entity.DREntityLogic;
import io.github.foundationgames.deathrun.game.state.logic.entity.EntityBehavior;
import io.github.foundationgames.deathrun.util.DRUtil;
import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class DRGame {
    public final ServerWorld world;
    public final GameActivity game;
    public final DeathRunMap map;
    public final DeathRunConfig config;
    public final DRPlayerLogic players;
    private final DREntityLogic entities;
    private final DRItemLogic items = new DRItemLogic();
    private final List<ResetCandidate> resets = new ArrayList<>();
    private final Map<Player, Integer> finished = new LinkedHashMap<>();

    private static final int DEATH_TRAP_COOLDOWN = 10 * 20; // 10 seconds
    private static final int END_COUNTDOWN = 100 * 20; // 100 seconds
    private static final int FINISH_TIMER = 3 * 20; // 3 seconds

    private int startTimer = 10 * 20; // 10 seconds

    // Timers, disabled (-1) until set to their time
    private int endCountdown = -1;
    private int finishTimer = -1;

    public DRGame(GameActivity game, DRWaiting waiting) {
        this.world = waiting.world;
        this.game = game;
        this.map = waiting.map;
        this.config = waiting.config;
        this.players = new DRPlayerLogic(this.world, game, map, config);
        this.entities = new DREntityLogic(world, this);

        game.listen(ItemUseEvent.EVENT, items::processUse);
    }

    public static void open(GameSpace space, DRWaiting waiting) {
        space.setActivity(game -> {
            var deathRun = new DRGame(game, waiting);

            DRUtil.setBaseGameRules(game);

            DRPlayerLogic.sortTeams(deathRun.world.random, waiting.players, deathRun);
            deathRun.players.forEach(deathRun.players::resetActive);

            deathRun.items.addBehavior("boost", (player, stack, hand) -> {
                if (deathRun.players.get(player) instanceof Player gamePl && gamePl.started && !gamePl.finished && !player.getItemCooldownManager().isCoolingDown(stack.getItem())) {
                    double yaw = Math.toRadians(-player.getYaw());
                    var vel = new Vec3d(1.25 * Math.sin(yaw), 0.5, 1.25 * Math.cos(yaw));
                    player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player.getId(), vel));
                    deathRun.world.getPlayers().forEach(p -> p.networkHandler.sendPacket(new ParticleS2CPacket(ParticleTypes.EXPLOSION, false, player.getX(), player.getY(), player.getZ(), 0, 0, 0, 0, 1)));
                    deathRun.players.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.75f, 0.69f);
                    player.getItemCooldownManager().set(stack.getItem(), 188);
                    return TypedActionResult.success(stack);
                }
                return TypedActionResult.pass(stack);
            });

            deathRun.items.addBehavior("activator", (player, stack, hand) -> {
                if (deathRun.players.get(player) instanceof Player gamePl && gamePl.started && !gamePl.finished && !player.getItemCooldownManager().isCoolingDown(stack.getItem())) {
                    var world = deathRun.world;
                    var trident = new TridentEntity(world, player, stack);
                    trident.setVelocity(player, player.getPitch(), player.getYaw(), 0, 3, 1);
                    deathRun.spawn(trident, new ActivatorTridentEntityBehavior());
                    world.playSoundFromEntity(null, trident, SoundEvents.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1, 1);
                    player.getItemCooldownManager().set(stack.getItem(), 200);
                    return TypedActionResult.success(stack);
                }
                return TypedActionResult.fail(stack);
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
            game.listen(GameActivityEvents.TICK, deathRun.entities::tick);
        });
    }

    private ActionResult useBlock(ServerPlayerEntity player, Hand hand, BlockHitResult hit) {
        if (this.players.get(player) instanceof Player gamePlayer) {
            if (gamePlayer.team == DRTeam.DEATHS) {
                var pos = hit.getBlockPos();
                var state = world.getBlockState(pos);
                if (state.getBlock() instanceof AbstractButtonBlock button && !state.get(Properties.POWERED)) {
                    var trapZone = map.trapZones.get(pos);
                    if (trapZone != null) {
                        world.setBlockState(pos, state.with(Properties.POWERED, true));
                        world.createAndScheduleBlockTick(pos, button, DEATH_TRAP_COOLDOWN);
                        trigger(trapZone);
                        return ActionResult.SUCCESS;
                    }
                }
            }
        }
        return ActionResult.PASS;
    }

    public void trigger(DeathTrapZone trapZone) {
        var deathTrap = trapZone.getTrap();
        deathTrap.trigger(this, world, trapZone.getZone());
        if (deathTrap instanceof ResettingDeathTrap resettable) {
            scheduleReset(resettable, trapZone);
        }
    }

    public void openGate() {
        for (BlockPos pos : map.gate) {
            if (world.getBlockState(pos).isOf(Blocks.IRON_BARS)) world.removeBlock(pos, false);
        }
    }

    public void scheduleReset(ResettingDeathTrap deathTrap, DeathTrapZone zone) {
        this.resets.add(new ResetCandidate(this, world, deathTrap, zone));
    }

    public <E extends Entity> void spawn(E entity, EntityBehavior<E> behavior) {
        world.spawnEntity(entity);
        entities.attach(entity, behavior);
    }

    public int getColorForPlace(int place) {
        return switch (place) {
            case 1 -> 0xeba721;
            case 2 -> 0xc3d8e8;
            case 3 -> 0xb04c00;
            default -> 0x7a7fff;
        };
    }

    public String getLocalizationForPlace(int place) {
        if (place > 10 && place < 20) return "insert.deathrun.xth_place";
        int lastDigit = place % 10;
        return switch (lastDigit) {
            case 1 -> "insert.deathrun.xst_place";
            case 2 -> "insert.deathrun.xnd_place";
            case 3 -> "insert.deathrun.xrd_place";
            default -> "insert.deathrun.xth_place";
        };
    }

    public void markFinished(Player player) {
        var pl = player.getPlayer();
        pl.getInventory().clear();
        player.finished = true;
    }

    public void finish(Player player) {
        int time = player.getTime();
        finished.put(player, time);
        int place = finished.size();

        int totalSec = time / 20;
        int min = (int)Math.floor((float)totalSec / 60);
        int sec = totalSec % 60;

        var timeText = new TranslatableText("insert.deathrun.time", min, sec).formatted(Formatting.DARK_GRAY);
        var text = new TranslatableText("message.deathrun.finished")
                .formatted(Formatting.BLUE)
                .append(new TranslatableText(getLocalizationForPlace(place), place).styled(style -> style.withColor(getColorForPlace(place)).withBold(true)))
                .append(timeText);
        var pl = player.getPlayer();

        pl.sendMessage(text, false);
        markFinished(player);

        if (place == 1) {
            pl.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HARP, SoundCategory.MASTER, 0.85f, 0.95f);
            pl.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HARP, SoundCategory.MASTER, 0.85f, 0.59f);
            pl.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS, SoundCategory.MASTER, 0.85f, 0.95f);
            pl.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1, 1);
            pl.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 0.3f, 2);

            this.endCountdown = END_COUNTDOWN;
        } else {
            pl.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1, 0.945f);
            pl.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1, 0.59f);
            pl.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS, SoundCategory.MASTER, 0.85f, 0.785f);
        }

        var broadcast = new TranslatableText("message.deathrun.player_finished", pl.getEntityName()).formatted(Formatting.LIGHT_PURPLE)
                .append(new TranslatableText(getLocalizationForPlace(place), place).styled(style -> style.withColor(getColorForPlace(place))))
                .append(timeText);
        players.forEach(p -> {
            if (p != pl) {
                p.sendMessage(broadcast, false);
            }
        });

        boolean allFinished = true;
        for (var drp : players.getPlayers()) {
            if (drp instanceof Player gamePlayer) {
                if (gamePlayer.team == DRTeam.RUNNERS && !gamePlayer.isFinished()) {
                    allFinished = false;
                    break;
                }
            }
        }
        if (allFinished) {
            end();
        }
    }

    public void start() {
        startTimer = 0;
        players.getPlayers().forEach(p -> { if (p instanceof Player pl) pl.onStart(); });
        openGate();
    }

    public void end() {
        endCountdown = -1;
        finishTimer = FINISH_TIMER;
        players.getPlayers().forEach(drp -> {
            if (drp instanceof Player player && player.team == DRTeam.RUNNERS && !player.isFinished()) {
                markFinished(player);
            }
        });
        broadcastRankings();
    }

    public void broadcastRankings() {
        var header = new LiteralText("---- ").formatted(Formatting.GRAY).append(new TranslatableText("message.deathrun.game_ended").formatted(Formatting.RED).append(new LiteralText(" ----").formatted(Formatting.GRAY)));
        var pedestal = new ArrayList<Text>();
        var places = new ArrayList<>(finished.keySet());
        for (int i = 0; i <= 2; i++) {
            int place = i + 1;
            if (i < places.size()) {
                pedestal.add(new LiteralText(Integer.toString(place))
                        .styled(style -> style.withColor(getColorForPlace(place)).withBold(true))
                        .append(new LiteralText(" - "+places.get(i).getPlayer().getEntityName()).formatted(Formatting.GRAY).styled(style -> style.withBold(false)))
                );
            }
        }
        players.getPlayers().forEach(player -> {
            if (player instanceof Player gamePlayer) {
                var pl = player.getPlayer();
                pl.sendMessage(header, false);
                for (var text : pedestal) {
                    pl.sendMessage(text, false);
                }
                int idx = places.indexOf(gamePlayer);
                if (idx >= 0) {
                    int place = idx + 1;
                    pl.sendMessage(new TranslatableText("message.deathrun.your_place", pl.getEntityName())
                            .formatted(Formatting.BLUE)
                            .append(new TranslatableText(getLocalizationForPlace(place), place)
                                    .styled(style -> style.withColor(getColorForPlace(place)).withBold(true))), false);
                } else if (gamePlayer.team == DRTeam.RUNNERS) {
                    pl.sendMessage(new TranslatableText("message.deathrun.did_not_finish", pl.getEntityName()).formatted(Formatting.BLUE), false);
                }
            }
        });
    }

    public void tick() {
        if (startTimer > 0) {
            if (startTimer % 20 == 0) {
                int sec = startTimer / 20;
                var format = sec <= 3 ? Formatting.GREEN : Formatting.DARK_GREEN;
                players.showTitle(new LiteralText(Integer.toString(sec)).formatted(Formatting.BOLD, format), 19);
                players.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HAT, SoundCategory.PLAYERS, 1.0f, 1.0f);
                if (sec <= 3) players.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1.0f, 1.0f);
            }
            startTimer--;
            if (startTimer == 0) {
                players.showTitle(new TranslatableText("title.deathrun.run").formatted(Formatting.BOLD, Formatting.GOLD), 40);
                players.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 1.0f, 1.0f);
                players.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1.0f, 0.5f);
                start();
            }
        }
        if (endCountdown > 0) {
            players.getPlayers().forEach(drPlayer -> {
                if (drPlayer instanceof Player player) {
                    var pl = player.getPlayer();
                    var key = "message.deathrun.game_ends_in";
                    if (player.team == DRTeam.RUNNERS && !player.finished) {
                        key = "message.deathrun.seconds_to_finish";
                    }
                    pl.sendMessage(new TranslatableText(key, (int)((float)endCountdown / 20)), true);
                }
            });
            endCountdown--;
            if (endCountdown == 0) {
                end();
            }
        }
        if (finishTimer > 0) {
            finishTimer--;
            if (finishTimer == 0) {
                game.getGameSpace().close(GameCloseReason.FINISHED);
            }
        }

        for (var candidate : resets) {
            candidate.tick();
        }
        resets.removeIf(r -> r.removed);
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
                var fluid = world.getFluidState(new BlockPos(serverP.getPos().add(0, 0.65, 0))).getFluid();
                return fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER;
            },
            // Lightning death
            player -> {
                var serverP = player.getPlayer();
                var world = serverP.world;
                return world.getEntitiesByClass(LightningEntity.class, serverP.getBoundingBox().expand(1.5, 1.5, 1.5), e -> true).size() > 0;
            },
            // Arrow death
            player -> {
                var serverP = player.getPlayer();
                var world = serverP.world;
                return world.getEntitiesByClass(ArrowEntity.class, serverP.getBoundingBox().expand(0.08, 0.08, 0.08), e -> true).size() > 0;
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
        public final DRGame game;
        private CheckpointZone checkpoint = null;

        private boolean started = false;
        private boolean finished = false;
        private int time = 0;

        public Player(ServerPlayerEntity player, DRPlayerLogic logic, DRTeam team, DRGame game) {
            super(player, logic);
            this.team = team;
            this.game = game;
        }

        public CheckpointZone getCheckpoint() {
            return checkpoint;
        }

        public int getTime() {
            return time;
        }

        public void onStart() {
            started = true;
        }

        public boolean isFinished() {
            return finished;
        }

        @Override
        public void tick() {
            var pos = getPlayer().getBlockPos();
            if (team == DRTeam.RUNNERS) {
                if (started && !finished) time++;
                for (var predicate : DEATH_CONDITIONS) {
                    if (predicate.test(this)) {
                        var pl = getPlayer();
                        logic.resetActive(pl);
                        pl.playSound(SoundEvents.ENTITY_GENERIC_HURT, SoundCategory.PLAYERS, 1, 1);
                    }
                }
                for (CheckpointZone zone : game.map.checkpoints) {
                    if (zone.bounds().contains(pos.getX(), pos.getY(), pos.getZ())) {
                        if (this.checkpoint != zone) notifyCheckpoint();
                        this.checkpoint = zone;
                        break;
                    }
                }
                if (finished) {
                    getPlayer().addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 5, 0, true, false, false));
                } else if (game.map.finish.contains(pos)) {
                    game.finish(this);
                }
            }
            // Applies to both deaths and runners, so you can have levitation
            // or jump boost areas to help deaths get around
            for (EffectZone zone : game.map.effectZones) {
                if (zone.bounds().contains(pos.getX(), pos.getY(), pos.getZ())) {
                    getPlayer().addStatusEffect(zone.effect().createEffect());
                }
            }
            if (team == DRTeam.DEATHS) {
                getPlayer().addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 5, 3, true, false, false));
            }
        }

        private void notifyCheckpoint() {
            var player = getPlayer();
            player.sendMessage(new TranslatableText("message.deathrun.checkpoint").formatted(Formatting.GREEN), false);
            player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 0.9f, 0.79f);
            player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS, SoundCategory.MASTER, 0.9f, 0.785f);
        }
    }

    public static class ResetCandidate {
        private final DRGame game;
        private final ServerWorld world;
        private final ResettingDeathTrap deathTrap;
        private final DeathTrapZone zone;
        private int time = DEATH_TRAP_COOLDOWN - 35;
        public boolean removed = false;

        public ResetCandidate(DRGame game, ServerWorld world, ResettingDeathTrap deathTrap, DeathTrapZone zone) {
            this.game = game;
            this.world = world;
            this.deathTrap = deathTrap;
            this.zone = zone;
        }

        public void tick() {
            this.time--;
            if (this.time <= 0) {
                deathTrap.reset(game, world, zone.getZone());
                removed = true;
            }
        }
    }
}
