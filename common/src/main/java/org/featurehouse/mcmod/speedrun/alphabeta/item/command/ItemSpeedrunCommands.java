/*
 * This file is part of αβspeedrun.
 * Copyright (C) 2022 Pigeonia Featurehouse
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.featurehouse.mcmod.speedrun.alphabeta.item.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.logging.LogUtils;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.featurehouse.mcmod.speedrun.alphabeta.config.AlphabetSpeedrunConfigData;
import org.featurehouse.mcmod.speedrun.alphabeta.config.AlphabetSpeedrunConfigData.Permissions;
import org.featurehouse.mcmod.speedrun.alphabeta.item.ItemRecordAccess;
import org.featurehouse.mcmod.speedrun.alphabeta.item.ItemSpeedrun;
import org.featurehouse.mcmod.speedrun.alphabeta.item.MultiplayerRecords;
import org.featurehouse.mcmod.speedrun.alphabeta.item.StoredItemRecords;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.DefaultItemSpeedrunDifficulty;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.ItemSpeedrunDifficulty;
import org.featurehouse.mcmod.speedrun.alphabeta.util.ConcurrentUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.ToIntFunction;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ItemSpeedrunCommands {
    static Command<ServerCommandSource> command(ToIntFunction<Permissions> permission, Function<HelperEnv, Command<ServerCommandSource>> wrapped) {
        return s -> {
            if (!s.getSource().hasPermissionLevel(permission.applyAsInt(AlphabetSpeedrunConfigData.getInstance().getPermissions()))) {
                s.getSource().sendError(Text.translatable("command.speedrun.alphabet.no_permission"));
                return 0;
            }
            Command<ServerCommandSource> c;
            try {
                c = wrapped.apply(new HelperEnv());
                return c.run(s);
            } catch (HelperEnv.PlayerNotFoundException e) {
                return 0;
            }
        };
    }

    private static SuggestionProvider<ServerCommandSource> suggestGoal() {
        return (context, builder) -> {
            ItemSpeedrun.DataLoader.getCurrentData().keySet()
                    .forEach(id -> builder.suggest(id.toString()));
            return builder.buildFuture();
        };
    }

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("itemspeedrun")
                .then(literal("start")
                        .then(argument("goal", IdentifierArgumentType.identifier())
                                .suggests(suggestGoal())
                                .executes(command(a -> 0 /* placeholder - this is much complex*/,
                                        env -> s -> {
                                            ServerPlayerEntity player = env.getPlayer(s);
                                            return startCmd(Collections.singleton(player), DefaultItemSpeedrunDifficulty.UU).run(s);
                                        }))
                                .then(argument("players", EntityArgumentType.players())
                                        .executes(command(a -> 0,
                                                env -> s -> startCmd(EntityArgumentType.getPlayers(s, "players"), DefaultItemSpeedrunDifficulty.UU).run(s)
                                        ))
                                        .then(argument("difficulty", IdentifierArgumentType.identifier())
                                                .suggests((ctx, builder) -> {
                                                    DefaultItemSpeedrunDifficulty.getIdToObjMap().keySet()
                                                            .forEach(id -> builder.suggest(id.toString()));
                                                    return builder.buildFuture();
                                                })
                                                .executes(command(a -> 0,
                                                        env -> s -> {
                                                            Identifier difficultyId = IdentifierArgumentType.getIdentifier(s, "difficulty");
                                                            ItemSpeedrunDifficulty difficulty = DefaultItemSpeedrunDifficulty.getDifficulty(difficultyId);
                                                            return startCmd(EntityArgumentType.getPlayers(s, "players"), difficulty).run(s);
                                                        }))
                                        )
                                )
                        )
                )
                .then(literal("draft")
                        .then(literal("create")
                                .executes(command(Permissions::getDraft, env -> s -> {
                                    final Optional<Text> err = DraftManager.get().createDraft(env.getPlayer(s));
                                    if (err.isPresent()) {
                                        s.getSource().sendError(err.get());
                                        return 0;
                                    } else {
                                        s.getSource().sendMessage(Text.translatable("command.speedrun.alphabet.draft"));
                                        return 1;
                                    }
                                }))
                        )
                        .then(literal("query")
                                .executes(command(Permissions::getDraft, env -> s -> {
                                    final Draft draft = DraftManager.get().get(env.getPlayer(s));
                                    if (draft == null) {
                                        s.getSource().sendError(Text.translatable("command.speedrun.alphabet.draft.not_found"));
                                        return 0;
                                    }
                                    s.getSource().sendMessage(Text.translatable("command.speedrun.alphabet.draft.query", draft.snapshot().asText()));
                                    return 1;
                                }))
                        )
                        .then(literal("setgoal")
                                .then(argument("goal", IdentifierArgumentType.identifier())
                                        .suggests(suggestGoal())
                                        .executes(command(Permissions::getDraft, env -> s -> {
                                            final ServerPlayerEntity player = env.getPlayer(s);
                                            final Draft draft = DraftManager.get().get(player);
                                            if (draft == null) {
                                                s.getSource().sendError(Text.translatable("command.speedrun.alphabet.draft.not_found"));
                                                return 0;
                                            }
                                            Identifier id = IdentifierArgumentType.getIdentifier(s, "goal");
                                            draft.setGoal(id);
                                            s.getSource().sendMessage(Text.translatable("command.speedrun.alphabet.draft.set_goal", id));
                                            return 1;
                                        }))
                                )
                        )
                        .then(literal("submit")
                                .executes(command(a->0, env -> s -> DraftManager.get().submit(s.getSource(), env.getPlayer(s))))
                        )
                        .then(literal("setplaytype")
                                .then(literal("pvp")
                                        .executes(command(Permissions::getDraft, env -> s -> {
                                            final ServerPlayerEntity player = env.getPlayer(s);
                                            final Draft draft = DraftManager.get().get(player);
                                            if (draft == null) {
                                                s.getSource().sendError(Text.translatable("command.speedrun.alphabet.draft.not_found"));
                                                return 0;
                                            }
                                            draft.setPlayType(PlayType.PVP);
                                            player.sendMessage(Text.translatable("command.speedrun.alphabet.draft.set_play_type", PlayType.PVP.getText()));
                                            return 1;
                                        }))
                                )
                                .then(literal("coop")
                                        .executes(command(Permissions::getDraft, env -> s -> {
                                            final ServerPlayerEntity player = env.getPlayer(s);
                                            final Draft draft = DraftManager.get().get(player);
                                            if (draft == null) {
                                                s.getSource().sendError(Text.translatable("command.speedrun.alphabet.draft.not_found"));
                                                return 0;
                                            }
                                            draft.setPlayType(PlayType.COOP);
                                            player.sendMessage(Text.translatable("command.speedrun.alphabet.draft.set_play_type", PlayType.COOP.getText()));
                                            return 1;
                                        }))
                                )
                        )
                        .then(literal("setdifficulty")
                                .then(argument("difficulty", IdentifierArgumentType.identifier())
                                        .suggests((ctx, builder) -> {
                                            DefaultItemSpeedrunDifficulty.getIdToObjMap().keySet()
                                                    .forEach(id -> builder.suggest(id.toString()));
                                            return builder.buildFuture();
                                        })
                                        .executes(command(Permissions::getDraft, env -> s -> {
                                            final ServerPlayerEntity player = env.getPlayer(s);
                                            final Draft draft = DraftManager.get().get(player);
                                            if (draft == null) {
                                                s.getSource().sendError(Text.translatable("command.speedrun.alphabet.draft.not_found"));
                                                return 0;
                                            }
                                            Identifier difficulty0 = IdentifierArgumentType.getIdentifier(s, "difficulty");
                                            final ItemSpeedrunDifficulty difficulty = DefaultItemSpeedrunDifficulty.getDifficulty(difficulty0);
                                            draft.setDifficulty(difficulty);
                                            player.sendMessage(Text.translatable("command.speedrun.alphabet.draft.set_difficulty", difficulty.asText()));
                                            return 1;
                                        }))
                                )
                        )
                        .then(literal("op")
                                .then(literal("add")
                                        .then(argument("players", EntityArgumentType.players())
                                                .executes(command(Permissions::getDraft, env -> s -> {
                                                    final ServerPlayerEntity player = env.getPlayer(s);
                                                    final Draft draft = DraftManager.get().get(player);
                                                    if (draft == null) {
                                                        s.getSource().sendError(Text.translatable("command.speedrun.alphabet.draft.not_found"));
                                                        return 0;
                                                    }
                                                    if (draft.getPlayType() != PlayType.COOP) {
                                                        s.getSource().sendError(Text.translatable("command.speedrun.alphabet.draft.op.add.not_coop"));
                                                        return 0;
                                                    }
                                                    EntityArgumentType.getPlayers(s, "players").forEach(p ->
                                                            draft.getOperators().add(p.getUuid()));
                                                    return 1;
                                                }))
                                        )
                                )
                                .then(literal("remove")
                                        .then(argument("players", EntityArgumentType.players())
                                                .executes(command(Permissions::getDraft, env -> s -> {
                                                    final ServerPlayerEntity player = env.getPlayer(s);
                                                    final Draft draft = DraftManager.get().get(player);
                                                    if (draft == null) {
                                                        s.getSource().sendError(Text.translatable("command.speedrun.alphabet.draft.not_found"));
                                                        return 0;
                                                    }
                                                    if (draft.getPlayType() != PlayType.COOP) {
                                                        s.getSource().sendError(Text.translatable("command.speedrun.alphabet.draft.op.remove.not_coop"));
                                                        return 0;
                                                    }
                                                    EntityArgumentType.getPlayers(s, "players").forEach(p ->
                                                            draft.getOperators().remove(p.getUuid()));
                                                    return 1;
                                                }))
                                        )
                                )
                        )
                )
                .then(literal("invite")
                        .then(literal("send")
                                .then(argument("players", EntityArgumentType.players())
                                        .executes(command(Permissions::getInvite,
                                                env -> s -> MultiplayerRecords.invite(env.getPlayer(s),
                                                        EntityArgumentType.getPlayers(s, "players"), s.getSource()::sendError)))
                                )
                        )
                        .then(literal("respond")
                                .then(argument("type", IntegerArgumentType.integer(1, 6))
                                        .then(argument("host", EntityArgumentType.player())
                                                .then(argument("session", UuidArgumentType.uuid())
                                                        .executes(command(Permissions::getJoin, env -> s -> {
                                                            ServerPlayerEntity host = EntityArgumentType.getPlayer(s, "host");
                                                            UUID session = UuidArgumentType.getUuid(s, "session");
                                                            final int type = IntegerArgumentType.getInteger(s, "type");
                                                            MultiplayerRecords.respond(type & 3, host, env.getPlayer(s), session, type > Invitation.ACCEPT);
                                                            return 1;
                                                        }))
                                                )
                                        )
                                )
                        )
                )
                .then(literal("quit")
                        .executes(command(a->0, env -> s -> {
                            ServerPlayerEntity player = env.getPlayer(s);
                            return ItemSpeedrunCommandHandle.quit(s.getSource()::sendError, player, true);
                        }))
                )
                .then(literal("stop")
                        .executes(command(Permissions::getStop, env -> s -> {
                            ServerPlayerEntity player = env.getPlayer(s);
                            return ItemSpeedrunCommandHandle.stop(s.getSource(), Collections.singleton(player));
                        }))
                        .then(argument("players", EntityArgumentType.players())
                                .executes(command(Permissions::getStopOthers, env -> s -> {
                                    final Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(s, "players");
                                    final ServerCommandSource source = s.getSource();
                                    return ItemSpeedrunCommandHandle.stop(source, players);
                                }))
                        )
                )
                .then(literal("exposeto")
                        .then(argument("player", EntityArgumentType.player())
                                .executes(command(a->0, env -> s -> {
                                    final ServerPlayerEntity player = env.getPlayer(s);
                                    final ItemRecordAccess rec = player.alphabetSpeedrun$getItemRecordAccess();
                                    if (rec == null || rec.isCoop()) {
                                        s.getSource().sendError(Text.translatable("command.speedrun.alphabet.expose.not_found"));
                                        return 0;
                                    }
                                    rec.addTrust(EntityArgumentType.getPlayer(s, "player").getUuid());
                                    return 1;
                                }))
                        )
                )
                .then(literal("resume")
                        .then(literal("local")
                                .executes(command(Permissions::getResume, env -> s -> {
                                    ServerPlayerEntity player = env.getPlayer(s);
                                    return ItemSpeedrunCommandHandle.resumeLocal(s.getSource(), Collections.singleton(player));
                                }))
                                .then(argument("players", EntityArgumentType.players())
                                        .executes(command(Permissions::getResumeOthers, env -> s -> {
                                            final Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(s, "players");
                                            return ItemSpeedrunCommandHandle.resumeLocal(s.getSource(), players);
                                        }))
                                )
                        )
                        .then(argument("record", UuidArgumentType.uuid())
                                .executes(command(Permissions::getResume, env -> s -> {
                                    ServerPlayerEntity p = env.getPlayer(s);
                                    ConcurrentUtils.run(StoredItemRecords.resumeRecord(p, UuidArgumentType.getUuid(s, "record")), e -> {
                                        LOGGER.error("Failed to resume", e);
                                        s.getSource().sendError(Text.translatable("command.speedrun.alphabet.resume.interrupted"));
                                    });
                                    return 1;
                                }))
                                .then(argument("player", EntityArgumentType.player())
                                        .executes(command(Permissions::getResumeOthers, env -> s -> {
                                            final ServerPlayerEntity player = EntityArgumentType.getPlayer(s, "player");
                                            ConcurrentUtils.run(StoredItemRecords.resumeRecord(player,
                                                    UuidArgumentType.getUuid(s, "record")), e -> {
                                                LOGGER.error("Failed to resume", e);
                                                s.getSource().sendError(Text.translatable("command.speedrun.alphabet.resume.interrupted"));
                                            });
                                            return 1;
                                        }))
                                )
                        )
                )
                .then(literal("view")
                        .executes(command(Permissions::getView, env -> s -> {
                            final ServerPlayerEntity player = env.getPlayer(s);
                            return ItemSpeedrunCommandHandle.viewCurrentRecord(s.getSource()::sendError, player);
                        }))
                        .then(argument("player", EntityArgumentType.player())
                                .executes(command(a->0, env -> s -> {
                                    final ServerPlayerEntity player = EntityArgumentType.getPlayer(s, "player");
                                    final ServerCommandSource source = s.getSource();
                                    // Check access
                                    int accessLevel;
                                    final @Nullable ServerPlayerEntity maybeExecutor = s.getSource().getPlayer();
                                    if (player == maybeExecutor) {
                                        accessLevel = AlphabetSpeedrunConfigData.getInstance().getPermissions().getView();
                                    } else {
                                        final ItemRecordAccess rec = player.alphabetSpeedrun$getItemRecordAccess();
                                        if (rec != null && !rec.isCoop() && rec.trusts(maybeExecutor)) {
                                            accessLevel = AlphabetSpeedrunConfigData.getInstance().getPermissions().getViewPvpMates();
                                        } else {
                                            accessLevel = AlphabetSpeedrunConfigData.getInstance().getPermissions().getViewOthers();
                                        }
                                    }
                                    if (!s.getSource().hasPermissionLevel(accessLevel)) {
                                        s.getSource().sendError(Text.translatable("command.speedrun.alphabet.no_permission"));
                                        return 0;
                                    }
                                    return ItemSpeedrunCommandHandle.viewCurrentRecord(source::sendError, player);
                                }))
                        )
                )
                .then(literal("archive")
                        .executes(command(Permissions::getArchive, env -> s -> {
                            ServerPlayerEntity p = env.getPlayer(s);
                            ConcurrentUtils.run(StoredItemRecords.archiveRecord(p, p::alphabetSpeedrun$getHistory), e -> {
                                LOGGER.error("Failed to archive", e);
                                s.getSource().sendError(Text.translatable("command.speedrun.alphabet.archive.interrupted"));
                            });
                            return 1;
                        }))
                        .then(argument("players", EntityArgumentType.players())
                                .executes(command(Permissions::getArchiveOthers, env -> s -> {
                                    Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(s, "players");
                                    ConcurrentUtils.run(CompletableFuture.allOf(players.stream().map(p -> StoredItemRecords.archiveRecord(
                                                    p, p::alphabetSpeedrun$getHistory))
                                            .toArray(CompletableFuture[]::new)), e -> {
                                        LOGGER.error("Failed to archive", e);
                                        s.getSource().sendError(Text.translatable("command.speedrun.alphabet.archive.interrupted"));
                                    });
                                    return 1;
                                }))
                        )
                )
                .then(literal("delete")
                        .then(argument("player", EntityArgumentType.player())
                                .then(argument("record", UuidArgumentType.uuid())
                                        .executes(command(a -> 0, env -> s -> {
                                            final ServerPlayerEntity player = EntityArgumentType.getPlayer(s, "player");
                                            final ServerCommandSource source = s.getSource();
                                            Permissions permissions = AlphabetSpeedrunConfigData.getInstance().getPermissions();
                                            IntSupplier sup = (player == source.getEntity()) ? permissions::getDelete : permissions::getDeleteOthers;
                                            if (!source.hasPermissionLevel(sup.getAsInt())) {
                                                source.sendError(Text.translatable("command.speedrun.alphabet.no_permission"));
                                                return 0;
                                            }

                                            UUID record = UuidArgumentType.getUuid(s, "record");
                                            ConcurrentUtils.run(StoredItemRecords.deleteRecord(player, record), e -> {
                                                LOGGER.error("Failed to delete", e);
                                                s.getSource().sendError(Text.translatable("command.speedrun.alphabet.delete.interrupted"));
                                            });
                                            return 1;
                                        }))
                                )
                        )
                )
                .then(literal("list")
                        .executes(command(Permissions::getList, env -> s -> {
                            ServerPlayerEntity p = env.getPlayer(s);
                            ConcurrentUtils.run(StoredItemRecords.listRecords(p), e -> {
                                LOGGER.error("Failed to delete", e);
                                s.getSource().sendError(Text.translatable("command.speedrun.alphabet.list.interrupted"));
                            });
                            return 1;
                        }))
                        .then(argument("player", EntityArgumentType.player())
                                .executes(command(a -> 0, env -> s -> {
                                    final ServerPlayerEntity player = EntityArgumentType.getPlayer(s, "player");
                                    final ServerCommandSource source = s.getSource();
                                    Permissions permissions = AlphabetSpeedrunConfigData.getInstance().getPermissions();
                                    IntSupplier sup = (player == source.getEntity() ? permissions::getList : permissions::getListOthers);
                                    if (!source.hasPermissionLevel(sup.getAsInt())) {
                                        source.sendError(Text.translatable("command.speedrun.alphabet.no_permission"));
                                        return 0;
                                    }

                                    ConcurrentUtils.run(StoredItemRecords.listRecords(player), e -> {
                                        LOGGER.error("Failed to delete", e);
                                        s.getSource().sendError(Text.translatable("command.speedrun.alphabet.list.interrupted"));
                                    });
                                    return 1;
                                }))
                        )
                )
        );

        // Historical compatibility
        // Shit mountain, keep
        dispatcher.register(CommandManager.literal("speedabc")
                .then(CommandManager.argument("letter", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (char c = 'a'; c <= 'z'; c++)
                                builder.suggest(String.valueOf(c));
                            return builder.buildFuture();
                        })
                        //.requires(ItemSpeedrunEvents::isOp)
                        .executes(s -> {
                            final String letter = StringArgumentType.getString(s, "letter");
                            if (!letter.matches("^[a-tvwyz]$"))
                                throw new CommandSyntaxException(CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect(),
                                        Text.translatable("command.speedrun.alphabet.legacy.letter.expected", letter));
                            final ServerPlayerEntity player = s.getSource().getPlayer();
                            if (player == null) {
                                s.getSource().sendError(Text.translatable("command.speedrun.alphabet.players_empty"));
                                return 0;
                            }
                            final Identifier goal = new Identifier("speedabc", letter);
                            s.getSource().sendError(Text.translatable("command.speedrun.alphabet.outdated_warning",
                                    "/itemspeedrun start speedabc:" + letter));
                            return ItemSpeedrunCommandHandle.start(s.getSource(), goal, Collections.singleton(player));
                        })
                )
        );
        dispatcher.register(CommandManager.literal("hannumspeed")
                .then(CommandManager.argument("length", IntegerArgumentType.integer(1, 10))
                        //.requires(ItemSpeedrunEvents::isOp)
                        .executes(s -> {
                            final ServerPlayerEntity player = s.getSource().getPlayer();
                            if (player == null) {
                                s.getSource().sendError(Text.translatable("command.speedrun.alphabet.players_empty"));
                                return 0;
                            }
                            final String sLen = Integer.toString(IntegerArgumentType.getInteger(s, "length"));
                            final Identifier goal = new Identifier("hannumspeed", sLen);
                            s.getSource().sendError(Text.translatable("command.speedrun.alphabet.outdated_warning",
                                    "/itemspeedrun start hannumspeed:" + sLen));
                            return ItemSpeedrunCommandHandle.start(s.getSource(), goal, Collections.singleton(player));
                        })
                )
        );
    }

    private static Command<ServerCommandSource> startCmd(Collection<? extends ServerPlayerEntity> players, ItemSpeedrunDifficulty difficulty) {
        return s -> {
            AlphabetSpeedrunConfigData instance = AlphabetSpeedrunConfigData.getInstance();
            Collection<ItemSpeedrunDifficulty> c = instance.getDifficultDifficulties();
            IntSupplier sup;
            if (c.contains(difficulty))
                sup = instance.getPermissions()::getDifficultStart;
            else sup = instance.getPermissions()::getNormalStart;
            ServerCommandSource source = s.getSource();
            if (!source.hasPermissionLevel(sup.getAsInt())) {
                source.sendError(Text.translatable("command.speedrun.alphabet.no_permission"));
                return 0;
            }
            return ItemSpeedrunCommandHandle.start(source, IdentifierArgumentType.getIdentifier(s, "goal"), players, difficulty);
        };
    }

    static final class HelperEnv {
        ServerPlayerEntity getPlayer(CommandContext<ServerCommandSource> s) throws PlayerNotFoundException {
            final ServerPlayerEntity player = s.getSource().getPlayer();
            if (player == null) {
                s.getSource().sendError(Text.translatable("command.speedrun.alphabet.players_empty"));
                throw new PlayerNotFoundException();
            }
            return player;
        }

        private static final class PlayerNotFoundException extends RuntimeException {}
    }

    private static final Logger LOGGER = LogUtils.getLogger();
}
