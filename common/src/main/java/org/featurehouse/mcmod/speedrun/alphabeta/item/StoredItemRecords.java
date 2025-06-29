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

package org.featurehouse.mcmod.speedrun.alphabeta.item;

import com.google.common.base.Suppliers;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import org.featurehouse.mcmod.speedrun.alphabeta.item.command.ItemSpeedrunCommandHandle;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

public class StoredItemRecords {
    private static final Gson GSON = new Gson();

    public static Path getPath(Path root, UUID playerUuid, @Nullable UUID recordUuid) {
        Path playerRoot = root.resolve("alphabet-speedrun-records/item")
                .resolve(playerUuid.toString());
        return recordUuid == null ? playerRoot : playerRoot.resolve(recordUuid + ".json");
    }

    public static CompletableFuture<ItemSpeedrunRecord> readRecord(Path path) {
        return CompletableFuture.<Either<ItemSpeedrunRecord, Throwable>>supplyAsync(() -> {
            try (final BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonObject obj = GSON.fromJson(reader, JsonObject.class);
                return ItemSpeedrunRecord.CODEC.parse(JsonOps.INSTANCE, obj).mapOrElse(
                        record -> Either.left(record.resetUuid()),
                        e -> Either.right(new JsonParseException(e.message()))
                );
            } catch (IOException | RuntimeException e) {
                return Either.right(e);
            }
        }).thenCompose(either -> either.map(CompletableFuture::completedFuture, CompletableFuture::failedFuture));
    }

    public static CompletableFuture<Void> resumeRecord(ServerPlayer player, UUID recordUuid) {
        return readRecord(getPath(rootPath(player), player.getUUID(), recordUuid))
                .thenAcceptAsync(record -> {
                    if (record.isFinished()) {
                        player.sendSystemMessage(Component.translatable("command.speedrun.alphabet.resume.done", recordUuid).withStyle(ChatFormatting.RED));
                    } else {
                        synchronized (player) {
                            final ItemRecordAccess old = player.alphabetSpeedrun$getItemRecordAccess();
                            if (old != null && old.isCoop()) {
                                if (ItemSpeedrunCommandHandle.quit(t -> player.sendSystemMessage(t.copy().withStyle(ChatFormatting.RED)), player, true) == 0) {
                                    return;
                                }
                            } else {
                                player.alphabetSpeedrun$moveRecordToHistory();
                            }
                            player.alphabetSpeedrun$setItemRecordAccess(record);
                            //record.setLastQuitTime(-1);
                            player.sendSystemMessage(Component.translatable("command.speedrun.alphabet.resume",
                                    record.goalId(), record.recordId()));
                            ItemSpeedrunEvents.START_RUNNING_EVENT.invoker().onStartRunning(player, record, ItemSpeedrunEvents.StartRunning.FROM_DISK);
                        }
                    }
                });
    }

    // Record: the one in history
    public static CompletableFuture<Void> archiveRecord(ServerPlayer player, Supplier<ItemSpeedrunRecord> record0) {
        Supplier<ItemSpeedrunRecord> record = Suppliers.memoize(record0::get);
        return CompletableFuture.<Either<Unit, Throwable>>supplyAsync(() -> {
            if (record.get() == null) {
                // TODO: change message receiver to command source
                player.sendSystemMessage(Component.translatable("command.speedrun.alphabet.archive.empty").withStyle(ChatFormatting.RED));
                return Either.left(Unit.INSTANCE);
            }
            final JsonObject json = ItemSpeedrunRecord.CODEC.encodeStart(JsonOps.INSTANCE, record.get()).flatMap(e -> {
                if (!e.isJsonObject()) return DataResult.error(() -> "Not a JSON object");
                return DataResult.success(e.getAsJsonObject());
            }).getOrThrow();
            Path path = getPath(rootPath(player), player.getUUID(), record.get().recordId());
            try {
                Files.createDirectories(path.getParent());
                try (BufferedWriter bw = Files.newBufferedWriter(path)){
                    GSON.toJson(json, bw);
                }
            } catch (IOException e) {
                return Either.right(e);
            }
            return Either.left(Unit.INSTANCE);
        }).thenCompose(either -> either.map(CompletableFuture::completedFuture, CompletableFuture::failedFuture))
                .thenAccept($ -> {
                    synchronized (player) {
                        player.alphabetSpeedrun$clearItemHistory();
                        player.sendSystemMessage(Component.translatable("command.speedrun.alphabet.archive",
                                record.get().goalId(), record.get().recordId()));
                    }
                });
    }

    public static CompletableFuture<Void> deleteRecord(ServerPlayer player, UUID uuid) {
        return CompletableFuture.<Either<Unit, Throwable>>supplyAsync(() -> {
            Path path = getPath(rootPath(player), player.getUUID(), uuid);
            try {
                if (Files.deleteIfExists(path)) {
                    player.sendSystemMessage(Component.translatable("command.speedrun.alphabet.delete", uuid));
                } else {
                    player.sendSystemMessage(Component.translatable("command.speedrun.alphabet.delete.not_found", uuid));
                }
            } catch (IOException e) {
                return Either.right(e);
            }
            return Either.left(Unit.INSTANCE);
        }).thenCompose(either -> either.map(CompletableFuture::completedFuture, CompletableFuture::failedFuture)).thenAccept($->{});
    }

    public static CompletableFuture<Void> listRecords(ServerPlayer player) {
        return CompletableFuture.<Either<Unit, Throwable>>supplyAsync(() -> {
            Path path = getPath(rootPath(player), player.getUUID(), null);
            if (Files.notExists(path)) {
                player.sendSystemMessage(Component.translatable("command.speedrun.alphabet.list.empty").withStyle(ChatFormatting.RED));
                return Either.left(Unit.INSTANCE);
            }
            try (Stream<Path> paths = Files.list(path)) {
                List<Path> strings = paths.filter(p -> FILENAME_PATTERN.asMatchPredicate().test(p.getFileName().toString()))
                        .toList();
                player.sendSystemMessage(Component.translatable("command.speedrun.alphabet.list.header", player.getDisplayName()));
                for (Path p : strings) {
                    JsonObject obj = GSON.fromJson(Files.newBufferedReader(p), JsonObject.class);
                    RecordSnapshot record = RecordSnapshot.fromPvpRecordJson(obj, player.getServer().overworld().getGameTime());
                    player.sendSystemMessage(Component.literal(" * ").append(record.asText()));
                }
                player.sendSystemMessage(Component.translatable("command.speedrun.alphabet.list.footer", strings.size()));
            } catch (IOException e) {
                return Either.right(e);
            }
            return Either.left(Unit.INSTANCE);
        }).thenCompose(either -> either.map(CompletableFuture::completedFuture, CompletableFuture::failedFuture)).thenAccept($->{});
    }

    static Path rootPath(ServerPlayer player) {
        return player.getServer().getWorldPath(LevelResource.ROOT);
    }
    public static final Pattern FILENAME_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}\\u002d[0-9a-fA-F]{4}\\u002d[0-9a-fA-F]{4}\\u002d[0-9a-fA-F]{4}\\u002d[0-9a-fA-F]{12}\\u002ejson$");
}
