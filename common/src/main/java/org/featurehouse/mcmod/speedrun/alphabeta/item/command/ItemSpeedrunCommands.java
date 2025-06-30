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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ItemSpeedrunCommands {
    static Command<CommandSourceStack> command(ToIntFunction<Permissions> permission, Function<HelperEnv, Command<CommandSourceStack>> wrapped) {
        return s -> {
            if (!s.getSource().hasPermission(permission.applyAsInt(AlphabetSpeedrunConfigData.getInstance().getPermissions()))) {
                s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.no_permission"));
                return 0;
            }
            Command<CommandSourceStack> c;
            try {
                c = wrapped.apply(new HelperEnv());
                return c.run(s);
            } catch (HelperEnv.PlayerNotFoundException e) {
                return 0;
            }
        };
    }

    private static SuggestionProvider<CommandSourceStack> suggestGoal() {
        return (context, builder) -> {
            ItemSpeedrun.DataLoader.getCurrentData().keySet()
                    .forEach(id -> builder.suggest(id.toString()));
            return builder.buildFuture();
        };
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("itemspeedrun")
                .then(literal("start")
                        .then(argument("goal", ResourceLocationArgument.id())
                                .suggests(suggestGoal())
                                .executes(command(a -> 0 /* placeholder - this is much complex*/,
                                        env -> s -> {
                                            ServerPlayer player = env.getPlayer(s);
                                            return startCmd(Collections.singleton(player), DefaultItemSpeedrunDifficulty.UU).run(s);
                                        }))
                                .then(argument("players", EntityArgument.players())
                                        .executes(command(a -> 0,
                                                env -> s -> startCmd(EntityArgument.getPlayers(s, "players"), DefaultItemSpeedrunDifficulty.UU).run(s)
                                        ))
                                        .then(argument("difficulty", ResourceLocationArgument.id())
                                                .suggests((ctx, builder) -> {
                                                    DefaultItemSpeedrunDifficulty.getIdToObjMap().keySet()
                                                            .forEach(id -> builder.suggest(id.toString()));
                                                    return builder.buildFuture();
                                                })
                                                .executes(command(a -> 0,
                                                        env -> s -> {
                                                            ResourceLocation difficultyId = ResourceLocationArgument.getId(s, "difficulty");
                                                            ItemSpeedrunDifficulty difficulty = DefaultItemSpeedrunDifficulty.getDifficulty(difficultyId);
                                                            return startCmd(EntityArgument.getPlayers(s, "players"), difficulty).run(s);
                                                        }))
                                        )
                                )
                        )
                )
                .then(literal("draft")
                        .then(literal("create")
                                .executes(command(Permissions::getDraft, env -> s -> {
                                    final Optional<Component> err = DraftManager.get().createDraft(env.getPlayer(s));
                                    if (err.isPresent()) {
                                        s.getSource().sendFailure(err.get());
                                        return 0;
                                    } else {
                                        s.getSource().sendSuccess(() -> Component.translatable("command.speedrun.alphabet.draft"), false);
                                        return 1;
                                    }
                                }))
                        )
                        .then(literal("query")
                                .executes(command(Permissions::getDraft, env -> s -> {
                                    final Draft draft = DraftManager.get().get(env.getPlayer(s));
                                    if (draft == null) {
                                        s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.draft.not_found"));
                                        return 0;
                                    }
                                    s.getSource().sendSuccess(() -> Component.translatable("command.speedrun.alphabet.draft.query", draft.snapshot().asText()), false);
                                    return 1;
                                }))
                        )
                        .then(literal("setgoal")
                                .then(argument("goal", ResourceLocationArgument.id())
                                        .suggests(suggestGoal())
                                        .executes(command(Permissions::getDraft, env -> s -> {
                                            final ServerPlayer player = env.getPlayer(s);
                                            final Draft draft = DraftManager.get().get(player);
                                            if (draft == null) {
                                                s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.draft.not_found"));
                                                return 0;
                                            }
                                            ResourceLocation id = ResourceLocationArgument.getId(s, "goal");
                                            draft.setGoal(id);
                                            s.getSource().sendSuccess(() -> Component.translatable("command.speedrun.alphabet.draft.set_goal", id), false);
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
                                            final ServerPlayer player = env.getPlayer(s);
                                            final Draft draft = DraftManager.get().get(player);
                                            if (draft == null) {
                                                s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.draft.not_found"));
                                                return 0;
                                            }
                                            draft.setPlayType(PlayType.PVP);
                                            player.sendSystemMessage(Component.translatable("command.speedrun.alphabet.draft.set_play_type", PlayType.PVP.getText()));
                                            return 1;
                                        }))
                                )
                                .then(literal("coop")
                                        .executes(command(Permissions::getDraft, env -> s -> {
                                            final ServerPlayer player = env.getPlayer(s);
                                            final Draft draft = DraftManager.get().get(player);
                                            if (draft == null) {
                                                s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.draft.not_found"));
                                                return 0;
                                            }
                                            draft.setPlayType(PlayType.COOP);
                                            player.sendSystemMessage(Component.translatable("command.speedrun.alphabet.draft.set_play_type", PlayType.COOP.getText()));
                                            return 1;
                                        }))
                                )
                        )
                        .then(literal("setdifficulty")
                                .then(argument("difficulty", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> {
                                            DefaultItemSpeedrunDifficulty.getIdToObjMap().keySet()
                                                    .forEach(id -> builder.suggest(id.toString()));
                                            return builder.buildFuture();
                                        })
                                        .executes(command(Permissions::getDraft, env -> s -> {
                                            final ServerPlayer player = env.getPlayer(s);
                                            final Draft draft = DraftManager.get().get(player);
                                            if (draft == null) {
                                                s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.draft.not_found"));
                                                return 0;
                                            }
                                            ResourceLocation difficulty0 = ResourceLocationArgument.getId(s, "difficulty");
                                            final ItemSpeedrunDifficulty difficulty = DefaultItemSpeedrunDifficulty.getDifficulty(difficulty0);
                                            draft.setDifficulty(difficulty);
                                            player.sendSystemMessage(Component.translatable("command.speedrun.alphabet.draft.set_difficulty", difficulty.asText()));
                                            return 1;
                                        }))
                                )
                        )
                        .then(literal("op")
                                .then(literal("add")
                                        .then(argument("players", EntityArgument.players())
                                                .executes(command(Permissions::getDraft, env -> s -> {
                                                    final ServerPlayer player = env.getPlayer(s);
                                                    final Draft draft = DraftManager.get().get(player);
                                                    if (draft == null) {
                                                        s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.draft.not_found"));
                                                        return 0;
                                                    }
                                                    if (draft.getPlayType() != PlayType.COOP) {
                                                        s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.draft.op.add.not_coop"));
                                                        return 0;
                                                    }
                                                    EntityArgument.getPlayers(s, "players").forEach(p ->
                                                            draft.getOperators().add(p.getUUID()));
                                                    return 1;
                                                }))
                                        )
                                )
                                .then(literal("remove")
                                        .then(argument("players", EntityArgument.players())
                                                .executes(command(Permissions::getDraft, env -> s -> {
                                                    final ServerPlayer player = env.getPlayer(s);
                                                    final Draft draft = DraftManager.get().get(player);
                                                    if (draft == null) {
                                                        s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.draft.not_found"));
                                                        return 0;
                                                    }
                                                    if (draft.getPlayType() != PlayType.COOP) {
                                                        s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.draft.op.remove.not_coop"));
                                                        return 0;
                                                    }
                                                    EntityArgument.getPlayers(s, "players").forEach(p ->
                                                            draft.getOperators().remove(p.getUUID()));
                                                    return 1;
                                                }))
                                        )
                                )
                        )
                )
                .then(literal("invite")
                        .then(literal("send")
                                .then(argument("players", EntityArgument.players())
                                        .executes(command(Permissions::getInvite,
                                                env -> s -> MultiplayerRecords.invite(env.getPlayer(s),
                                                        EntityArgument.getPlayers(s, "players"), s.getSource()::sendFailure)))
                                )
                        )
                        .then(literal("respond")
                                .then(argument("type", IntegerArgumentType.integer(1, 6))
                                        .then(argument("host", EntityArgument.player())
                                                .then(argument("session", UuidArgument.uuid())
                                                        .executes(command(Permissions::getJoin, env -> s -> {
                                                            ServerPlayer host = EntityArgument.getPlayer(s, "host");
                                                            UUID session = UuidArgument.getUuid(s, "session");
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
                            ServerPlayer player = env.getPlayer(s);
                            return ItemSpeedrunCommandHandle.quit(s.getSource()::sendFailure, player, true);
                        }))
                )
                .then(literal("stop")
                        .executes(command(Permissions::getStop, env -> s -> {
                            ServerPlayer player = env.getPlayer(s);
                            return ItemSpeedrunCommandHandle.stop(s.getSource(), Collections.singleton(player));
                        }))
                        .then(argument("players", EntityArgument.players())
                                .executes(command(Permissions::getStopOthers, env -> s -> {
                                    final Collection<ServerPlayer> players = EntityArgument.getPlayers(s, "players");
                                    final CommandSourceStack source = s.getSource();
                                    return ItemSpeedrunCommandHandle.stop(source, players);
                                }))
                        )
                )
                .then(literal("exposeto")
                        .then(argument("player", EntityArgument.player())
                                .executes(command(a->0, env -> s -> {
                                    final ServerPlayer player = env.getPlayer(s);
                                    final ItemRecordAccess rec = player.alphabetSpeedrun$getItemRecordAccess();
                                    if (rec == null || rec.isCoop()) {
                                        s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.expose.not_found"));
                                        return 0;
                                    }
                                    rec.addTrust(EntityArgument.getPlayer(s, "player").getUUID());
                                    return 1;
                                }))
                        )
                )
                .then(literal("resume")
                        .then(literal("local")
                                .executes(command(Permissions::getResume, env -> s -> {
                                    ServerPlayer player = env.getPlayer(s);
                                    return ItemSpeedrunCommandHandle.resumeLocal(s.getSource(), Collections.singleton(player));
                                }))
                                .then(argument("players", EntityArgument.players())
                                        .executes(command(Permissions::getResumeOthers, env -> s -> {
                                            final Collection<ServerPlayer> players = EntityArgument.getPlayers(s, "players");
                                            return ItemSpeedrunCommandHandle.resumeLocal(s.getSource(), players);
                                        }))
                                )
                        )
                        .then(argument("record", UuidArgument.uuid())
                                .executes(command(Permissions::getResume, env -> s -> {
                                    ServerPlayer p = env.getPlayer(s);
                                    ConcurrentUtils.run(StoredItemRecords.resumeRecord(p, UuidArgument.getUuid(s, "record")), e -> {
                                        LOGGER.error("Failed to resume", e);
                                        s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.resume.interrupted"));
                                    });
                                    return 1;
                                }))
                                .then(argument("player", EntityArgument.player())
                                        .executes(command(Permissions::getResumeOthers, env -> s -> {
                                            final ServerPlayer player = EntityArgument.getPlayer(s, "player");
                                            ConcurrentUtils.run(StoredItemRecords.resumeRecord(player,
                                                    UuidArgument.getUuid(s, "record")), e -> {
                                                LOGGER.error("Failed to resume", e);
                                                s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.resume.interrupted"));
                                            });
                                            return 1;
                                        }))
                                )
                        )
                )
                .then(literal("view")
                        .executes(command(Permissions::getView, env -> s -> {
                            final ServerPlayer player = env.getPlayer(s);
                            return ItemSpeedrunCommandHandle.viewCurrentRecord(s.getSource()::sendFailure, player);
                        }))
                        .then(argument("player", EntityArgument.player())
                                .executes(command(a->0, env -> s -> {
                                    final ServerPlayer player = EntityArgument.getPlayer(s, "player");
                                    final CommandSourceStack source = s.getSource();
                                    // Check access
                                    int accessLevel;
                                    final @Nullable ServerPlayer maybeExecutor = s.getSource().getPlayer();
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
                                    if (!s.getSource().hasPermission(accessLevel)) {
                                        s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.no_permission"));
                                        return 0;
                                    }
                                    return ItemSpeedrunCommandHandle.viewCurrentRecord(source::sendFailure, player);
                                }))
                        )
                )
                .then(literal("archive")
                        .executes(command(Permissions::getArchive, env -> s -> {
                            ServerPlayer p = env.getPlayer(s);
                            ConcurrentUtils.run(StoredItemRecords.archiveRecord(p, p::alphabetSpeedrun$getHistory), e -> {
                                LOGGER.error("Failed to archive", e);
                                s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.archive.interrupted"));
                            });
                            return 1;
                        }))
                        .then(argument("players", EntityArgument.players())
                                .executes(command(Permissions::getArchiveOthers, env -> s -> {
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(s, "players");
                                    ConcurrentUtils.run(CompletableFuture.allOf(players.stream().map(p -> StoredItemRecords.archiveRecord(
                                                    p, p::alphabetSpeedrun$getHistory))
                                            .toArray(CompletableFuture[]::new)), e -> {
                                        LOGGER.error("Failed to archive", e);
                                        s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.archive.interrupted"));
                                    });
                                    return 1;
                                }))
                        )
                )
                .then(literal("delete")
                        .then(argument("player", EntityArgument.player())
                                .then(argument("record", UuidArgument.uuid())
                                        .executes(command(a -> 0, env -> s -> {
                                            final ServerPlayer player = EntityArgument.getPlayer(s, "player");
                                            final CommandSourceStack source = s.getSource();
                                            Permissions permissions = AlphabetSpeedrunConfigData.getInstance().getPermissions();
                                            IntSupplier sup = (player == source.getEntity()) ? permissions::getDelete : permissions::getDeleteOthers;
                                            if (!source.hasPermission(sup.getAsInt())) {
                                                source.sendFailure(Component.translatable("command.speedrun.alphabet.no_permission"));
                                                return 0;
                                            }

                                            UUID record = UuidArgument.getUuid(s, "record");
                                            ConcurrentUtils.run(StoredItemRecords.deleteRecord(player, record), e -> {
                                                LOGGER.error("Failed to delete", e);
                                                s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.delete.interrupted"));
                                            });
                                            return 1;
                                        }))
                                )
                        )
                )
                .then(literal("list")
                        .executes(command(Permissions::getList, env -> s -> {
                            ServerPlayer p = env.getPlayer(s);
                            ConcurrentUtils.run(StoredItemRecords.listRecords(p), e -> {
                                LOGGER.error("Failed to delete", e);
                                s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.list.interrupted"));
                            });
                            return 1;
                        }))
                        .then(argument("player", EntityArgument.player())
                                .executes(command(a -> 0, env -> s -> {
                                    final ServerPlayer player = EntityArgument.getPlayer(s, "player");
                                    final CommandSourceStack source = s.getSource();
                                    Permissions permissions = AlphabetSpeedrunConfigData.getInstance().getPermissions();
                                    IntSupplier sup = (player == source.getEntity() ? permissions::getList : permissions::getListOthers);
                                    if (!source.hasPermission(sup.getAsInt())) {
                                        source.sendFailure(Component.translatable("command.speedrun.alphabet.no_permission"));
                                        return 0;
                                    }

                                    ConcurrentUtils.run(StoredItemRecords.listRecords(player), e -> {
                                        LOGGER.error("Failed to delete", e);
                                        s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.list.interrupted"));
                                    });
                                    return 1;
                                }))
                        )
                )
        );

        // Historical compatibility
        // Shit mountain, keep
        if (AlphabetSpeedrunConfigData.getInstance().isEnableLegacyCommands()) {
            dispatcher.register(Commands.literal("speedabc")
                    .then(Commands.argument("letter", StringArgumentType.word())
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
                                            Component.translatable("command.speedrun.alphabet.legacy.letter.expected", letter));
                                final ServerPlayer player = s.getSource().getPlayer();
                                if (player == null) {
                                    s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.players_empty"));
                                    return 0;
                                }
                                final ResourceLocation goal = ResourceLocation.fromNamespaceAndPath("speedabc", letter);
                                s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.outdated_warning",
                                        "/itemspeedrun start speedabc:" + letter));
                                return ItemSpeedrunCommandHandle.start(s.getSource(), goal, Collections.singleton(player));
                            })
                    )
            );
            dispatcher.register(Commands.literal("hannumspeed")
                    .then(Commands.argument("length", IntegerArgumentType.integer(1, 10))
                            //.requires(ItemSpeedrunEvents::isOp)
                            .executes(s -> {
                                final ServerPlayer player = s.getSource().getPlayer();
                                if (player == null) {
                                    s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.players_empty"));
                                    return 0;
                                }
                                final String sLen = Integer.toString(IntegerArgumentType.getInteger(s, "length"));
                                final ResourceLocation goal = ResourceLocation.fromNamespaceAndPath("hannumspeed", sLen);
                                s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.outdated_warning",
                                        "/itemspeedrun start hannumspeed:" + sLen));
                                return ItemSpeedrunCommandHandle.start(s.getSource(), goal, Collections.singleton(player));
                            })
                    )
            );
        } else {
            // Legacy commands are disabled.
            // sending command.speedrun.alphabet.outdated_warning only.
            dispatcher.register(Commands.literal("speedabc")
                    .then(Commands.argument("letter", StringArgumentType.word())
                            .executes(s -> {
                                final String letter = StringArgumentType.getString(s, "letter");
                                if (!letter.matches("^[a-tvwyz]$"))
                                    throw new CommandSyntaxException(CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect(),
                                            Component.translatable("command.speedrun.alphabet.legacy.letter.expected", letter));
                                s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.outdated_warning",
                                        "/itemspeedrun start speedabc:" + letter));
                                return 0;
                            })
                    )
            );
            dispatcher.register(Commands.literal("hannumspeed")
                    .then(Commands.argument("length", IntegerArgumentType.integer(1, 10))
                            .executes(s -> {
                                final int len = (IntegerArgumentType.getInteger(s, "length"));
                                s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.outdated_warning",
                                        "/itemspeedrun start hannumspeed:" + len));
                                return 0;
                            })
                    )
            );
        }
    }

    private static Command<CommandSourceStack> startCmd(Collection<? extends ServerPlayer> players, ItemSpeedrunDifficulty difficulty) {
        return s -> {
            AlphabetSpeedrunConfigData instance = AlphabetSpeedrunConfigData.getInstance();
            Collection<ItemSpeedrunDifficulty> c = instance.getDifficultDifficulties();
            IntSupplier sup;
            if (c.contains(difficulty))
                sup = instance.getPermissions()::getDifficultStart;
            else sup = instance.getPermissions()::getNormalStart;
            CommandSourceStack source = s.getSource();
            if (!source.hasPermission(sup.getAsInt())) {
                source.sendFailure(Component.translatable("command.speedrun.alphabet.no_permission"));
                return 0;
            }
            return ItemSpeedrunCommandHandle.start(source, ResourceLocationArgument.getId(s, "goal"), players, difficulty);
        };
    }

    static final class HelperEnv {
        ServerPlayer getPlayer(CommandContext<CommandSourceStack> s) throws PlayerNotFoundException {
            final ServerPlayer player = s.getSource().getPlayer();
            if (player == null) {
                s.getSource().sendFailure(Component.translatable("command.speedrun.alphabet.players_empty"));
                throw new PlayerNotFoundException();
            }
            return player;
        }

        private static final class PlayerNotFoundException extends RuntimeException {}
    }

    private static final Logger LOGGER = LogUtils.getLogger();
}
