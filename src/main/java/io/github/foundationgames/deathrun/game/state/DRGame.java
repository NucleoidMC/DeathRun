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
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.stimuli.event.EventResult;
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
    public final ServerLevel level;
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
        this.level = waiting.level;
        this.game = game;
        this.map = waiting.map;
        this.config = waiting.config;
        this.players = new DRPlayerLogic(this.level, game, map, config);
        this.entities = new DREntityLogic(level, this);

        game.listen(ItemUseEvent.EVENT, items::processUse);
    }

    private static void playSoundToPlayer(ServerPlayer player, SoundEvent sound, SoundSource category, float volume, float pitch) {
        player.connection.send(new ClientboundSoundEntityPacket(Holder.direct(sound), category, player, volume, pitch, player.level().getRandom().nextLong()));
    }

    public static void open(GameSpace space, DRWaiting waiting) {
        space.setActivity(game -> {
            var deathRun = new DRGame(game, waiting);

            DRUtil.setBaseGameRules(game);

            DRPlayerLogic.sortTeams(deathRun.level.getRandom(), waiting.players, deathRun);
            deathRun.players.forEach(deathRun.players::resetActive);

            deathRun.items.addBehavior("boost", (player, stack, hand) -> {
                if (deathRun.players.get(player) instanceof Player gamePl && gamePl.started && !gamePl.finished && !player.getCooldowns().isOnCooldown(stack)) {
                    double yaw = Math.toRadians(-player.getYRot());
                    var vel = new Vec3(1.25 * Math.sin(yaw), 0.5, 1.25 * Math.cos(yaw));
                    player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), vel));
                    deathRun.level.players().forEach(p -> p.connection.send(new ClientboundLevelParticlesPacket(ParticleTypes.EXPLOSION, false, false, player.getX(), player.getY(), player.getZ(), 0, 0, 0, 0, 1)));
                    deathRun.players.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.75f, 0.69f);
                    player.getCooldowns().addCooldown(stack, 188);
                    return InteractionResult.SUCCESS_SERVER;
                }
                return InteractionResult.PASS;
            });

            deathRun.items.addBehavior("activator", (player, stack, hand) -> {
                if (deathRun.players.get(player) instanceof Player gamePl && gamePl.started && !gamePl.finished && !player.getCooldowns().isOnCooldown(stack)) {
                    var level = deathRun.level;
                    var trident = new ThrownTrident(level, player, stack);
                    trident.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 3, 1);
                    deathRun.spawn(trident, new ActivatorTridentEntityBehavior());
                    level.playSound(null, trident, SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1, 1);
                    player.getCooldowns().addCooldown(stack, 200);
                    return InteractionResult.SUCCESS_SERVER;
                }
                return InteractionResult.PASS;
            });

            game.listen(GamePlayerEvents.OFFER, JoinOffer::acceptSpectators);
            game.listen(GamePlayerEvents.ACCEPT, deathRun.players::acceptSpectator);
            game.listen(GamePlayerEvents.LEAVE, deathRun.players::onLeave);
            game.listen(PlayerDamageEvent.EVENT, (player, source, amount) -> EventResult.DENY);
            game.listen(PlayerDeathEvent.EVENT, (player, source) -> {
                player.setHealth(20f);
                deathRun.players.resetWaiting(player);
                return EventResult.DENY;
            });
            game.listen(GameActivityEvents.TICK, deathRun::tick);
            game.listen(GameActivityEvents.STATE_UPDATE, state -> state.canPlay(false));
            game.listen(BlockUseEvent.EVENT, deathRun::useBlock);
            game.listen(GameActivityEvents.TICK, deathRun.players::tick);
            game.listen(GameActivityEvents.TICK, deathRun.entities::tick);
        });
    }

    private InteractionResult useBlock(ServerPlayer player, InteractionHand hand, BlockHitResult hit) {
        if (this.players.get(player) instanceof Player gamePlayer) {
            if (gamePlayer.team == DRTeam.DEATHS) {
                var pos = hit.getBlockPos();
                var state = level.getBlockState(pos);
                if (state.getBlock() instanceof ButtonBlock button && !state.getValue(BlockStateProperties.POWERED)) {
                    var trapZone = map.trapZones.get(pos);
                    if (trapZone != null) {
                        level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.POWERED, true));
                        level.scheduleTick(pos, button, DEATH_TRAP_COOLDOWN);
                        trigger(trapZone);
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }

    public void trigger(DeathTrapZone trapZone) {
        var deathTrap = trapZone.getTrap();
        deathTrap.trigger(this, level, trapZone.getZone());
        if (deathTrap instanceof ResettingDeathTrap resettable) {
            scheduleReset(resettable, trapZone);
        }
    }

    public void openGate() {
        for (BlockPos pos : map.gate) {
            if (level.getBlockState(pos).is(Blocks.IRON_BARS)) level.removeBlock(pos, false);
        }
    }

    public void scheduleReset(ResettingDeathTrap deathTrap, DeathTrapZone zone) {
        this.resets.add(new ResetCandidate(this, level, deathTrap, zone));
    }

    public <E extends Entity> void spawn(E entity, EntityBehavior<E> behavior) {
        level.addFreshEntity(entity);
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
        pl.getInventory().clearContent();
        player.finished = true;
    }

    public void finish(Player player) {
        int time = player.getTime();
        finished.put(player, time);
        int place = finished.size();

        int totalSec = time / 20;
        int min = (int)Math.floor((float)totalSec / 60);
        int sec = totalSec % 60;

        var timeText = Component.translatable("insert.deathrun.time", min, sec).withStyle(ChatFormatting.DARK_GRAY);
        var text = Component.translatable("message.deathrun.finished")
                .withStyle(ChatFormatting.BLUE)
                .append(Component.translatable(getLocalizationForPlace(place), place).withStyle(style -> style.withColor(getColorForPlace(place)).withBold(true)))
                .append(timeText);
        var pl = player.getPlayer();

        pl.sendSystemMessage(text, false);
        markFinished(player);

        if (place == 1) {
            playSoundToPlayer(pl, SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.MASTER, 0.85f, 0.95f);
            playSoundToPlayer(pl, SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.MASTER, 0.85f, 0.59f);
            playSoundToPlayer(pl, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 0.85f, 0.95f);
            playSoundToPlayer(pl, SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1, 1);
            playSoundToPlayer(pl, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 0.3f, 2);

            this.endCountdown = END_COUNTDOWN;
        } else {
            playSoundToPlayer(pl, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.MASTER, 1, 0.945f);
            playSoundToPlayer(pl, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.MASTER, 1, 0.59f);
            playSoundToPlayer(pl, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 0.85f, 0.785f);
        }

        var broadcast = Component.translatable("message.deathrun.player_finished", pl.getScoreboardName()).withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.translatable(getLocalizationForPlace(place), place).withStyle(style -> style.withColor(getColorForPlace(place))))
                .append(timeText);
        players.forEach(p -> {
            if (p != pl) {
                p.sendSystemMessage(broadcast, false);
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
        var header = Component.literal("---- ").withStyle(ChatFormatting.GRAY).append(Component.translatable("message.deathrun.game_ended").withStyle(ChatFormatting.RED).append(Component.literal(" ----").withStyle(ChatFormatting.GRAY)));
        var pedestal = new ArrayList<Component>();
        var places = new ArrayList<>(finished.keySet());
        for (int i = 0; i <= 2; i++) {
            int place = i + 1;
            if (i < places.size()) {
                pedestal.add(Component.literal(Integer.toString(place))
                        .withStyle(style -> style.withColor(getColorForPlace(place)).withBold(true))
                        .append(Component.literal(" - "+places.get(i).getPlayer().getScoreboardName()).withStyle(ChatFormatting.GRAY).withStyle(style -> style.withBold(false)))
                );
            }
        }
        players.getPlayers().forEach(player -> {
            if (player instanceof Player gamePlayer) {
                var pl = player.getPlayer();
                pl.sendSystemMessage(header, false);
                for (var text : pedestal) {
                    pl.sendSystemMessage(text, false);
                }
                int idx = places.indexOf(gamePlayer);
                if (idx >= 0) {
                    int place = idx + 1;
                    pl.sendSystemMessage(Component.translatable("message.deathrun.your_place", pl.getScoreboardName())
                            .withStyle(ChatFormatting.BLUE)
                            .append(Component.translatable(getLocalizationForPlace(place), place)
                                    .withStyle(style -> style.withColor(getColorForPlace(place)).withBold(true))), false);
                } else if (gamePlayer.team == DRTeam.RUNNERS) {
                    pl.sendSystemMessage(Component.translatable("message.deathrun.did_not_finish", pl.getScoreboardName()).withStyle(ChatFormatting.BLUE), false);
                }
            }
        });
    }

    public void tick() {
        if (startTimer > 0) {
            if (startTimer % 20 == 0) {
                int sec = startTimer / 20;
                var format = sec <= 3 ? ChatFormatting.GREEN : ChatFormatting.DARK_GREEN;
                players.showTitle(Component.literal(Integer.toString(sec)).withStyle(ChatFormatting.BOLD, format), 19);
                players.playSound(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
                if (sec <= 3) players.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            startTimer--;
            if (startTimer == 0) {
                players.showTitle(Component.translatable("title.deathrun.run").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD), 40);
                players.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
                players.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0f, 0.5f);
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
                    pl.sendSystemMessage(Component.translatable(key, (int)((float)endCountdown / 20)), true);
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
                return serverP.position().y < 0;
            },
            // Water death
            player -> {
                var serverP = player.getPlayer();
                var level = serverP.level();
                var fluid = level.getFluidState(BlockPos.containing(serverP.position().add(0, 0.65, 0))).getType();
                return fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER;
            },
            // Lightning death
            player -> {
                var serverP = player.getPlayer();
                var level = serverP.level();
                return level.getEntitiesOfClass(LightningBolt.class, serverP.getBoundingBox().inflate(1.5, 1.5, 1.5), e -> true).size() > 0;
            },
            // Arrow death
            player -> {
                var serverP = player.getPlayer();
                var level = serverP.level();
                return level.getEntitiesOfClass(Arrow.class, serverP.getBoundingBox().inflate(0.08, 0.08, 0.08), e -> true).size() > 0;
            },
            // Falling hazard death
            player -> {
                var serverP = player.getPlayer();
                var level = serverP.level();
                return level.getEntitiesOfClass(FallingBlockEntity.class, serverP.getBoundingBox(),
                        e -> e.getBlockState().is(Blocks.POINTED_DRIPSTONE)).size() > 0;
            }
    );

    public static class Player extends DRPlayer {
        public final DRTeam team;
        public final DRGame game;
        private CheckpointZone checkpoint = null;

        private boolean started = false;
        private boolean finished = false;
        private int time = 0;

        public Player(ServerPlayer player, DRPlayerLogic logic, DRTeam team, DRGame game) {
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
            var pos = getPlayer().blockPosition();
            if (team == DRTeam.RUNNERS) {
                if (started && !finished) time++;
                for (var predicate : DEATH_CONDITIONS) {
                    if (predicate.test(this)) {
                        var pl = getPlayer();
                        logic.resetActive(pl);
                        playSoundToPlayer(pl, SoundEvents.GENERIC_HURT, SoundSource.PLAYERS, 1, 1);
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
                    getPlayer().addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 5, 0, true, false, false));
                } else if (game.map.finish.contains(pos)) {
                    game.finish(this);
                }
            }
            // Applies to both deaths and runners, so you can have levitation
            // or jump boost areas to help deaths get around
            for (EffectZone zone : game.map.effectZones) {
                if (zone.bounds().contains(pos.getX(), pos.getY(), pos.getZ())) {
                    getPlayer().addEffect(zone.effect().createEffect());
                }
            }
            if (team == DRTeam.DEATHS) {
                getPlayer().addEffect(new MobEffectInstance(MobEffects.SPEED, 5, 3, true, false, false));
            }
        }

        private void notifyCheckpoint() {
            var player = getPlayer();
            player.sendSystemMessage(Component.translatable("message.deathrun.checkpoint").withStyle(ChatFormatting.GREEN), false);
            playSoundToPlayer(player, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.MASTER, 0.9f, 0.79f);
            playSoundToPlayer(player, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 0.9f, 0.785f);
        }
    }

    public static class ResetCandidate {
        private final DRGame game;
        private final ServerLevel level;
        private final ResettingDeathTrap deathTrap;
        private final DeathTrapZone zone;
        private int time = DEATH_TRAP_COOLDOWN - 35;
        public boolean removed = false;

        public ResetCandidate(DRGame game, ServerLevel level, ResettingDeathTrap deathTrap, DeathTrapZone zone) {
            this.game = game;
            this.level = level;
            this.deathTrap = deathTrap;
            this.zone = zone;
        }

        public void tick() {
            this.time--;
            if (this.time <= 0) {
                deathTrap.reset(game, level, zone.getZone());
                removed = true;
            }
        }
    }
}
