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
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Unit;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;
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
                return Either.left(ItemSpeedrunRecord.fromJson(GSON.fromJson(reader, JsonObject.class), true));
            } catch (IOException | RuntimeException e) {
                return Either.right(e);
            }
        }).thenCompose(either -> either.map(CompletableFuture::completedFuture, CompletableFuture::failedFuture));
    }

    public static CompletableFuture<Void> resumeRecord(ServerPlayerEntity player, UUID recordUuid) {
        return readRecord(getPath(rootPath(player), player.getUuid(), recordUuid))
                .thenAcceptAsync(record -> {
                    if (record.isFinished()) {
                        player.sendMessage(Text.translatable("command.speedrun.alphabet.resume.done", recordUuid).formatted(Formatting.RED));
                    } else {
                        synchronized (player) {
                            final ItemRecordAccess old = player.alphabetSpeedrun$getItemRecordAccess();
                            if (old != null && old.isCoop()) {
                                if (ItemSpeedrunCommandHandle.quit(t -> player.sendMessage(t.copy().formatted(Formatting.RED)), player, true) == 0) {
                                    return;
                                }
                            } else {
                                player.alphabetSpeedrun$moveRecordToHistory();
                            }
                            player.alphabetSpeedrun$setItemRecordAccess(record);
                            //record.setLastQuitTime(-1);
                            player.sendMessage(Text.translatable("command.speedrun.alphabet.resume",
                                    record.goalId(), record.recordId()));
                            ItemSpeedrunEvents.START_RUNNING_EVENT.invoker().onStartRunning(player, record, ItemSpeedrunEvents.StartRunning.FROM_DISK);
                        }
                    }
                });
    }

    // Record: the one in history
    public static CompletableFuture<Void> archiveRecord(ServerPlayerEntity player, Supplier<ItemSpeedrunRecord> record0) {
        Supplier<ItemSpeedrunRecord> record = Suppliers.memoize(record0::get);
        return CompletableFuture.<Either<Unit, Throwable>>supplyAsync(() -> {
            if (record.get() == null) {
                // TODO: change message receiver to command source
                player.sendMessage(Text.translatable("command.speedrun.alphabet.archive.empty").formatted(Formatting.RED));
                return Either.left(Unit.INSTANCE);
            }
            final JsonObject json = record.get().toJson();
            Path path = getPath(rootPath(player), player.getUuid(), record.get().recordId());
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
                        player.sendMessage(Text.translatable("command.speedrun.alphabet.archive",
                                record.get().goalId(), record.get().recordId()));
                    }
                });
    }

    public static CompletableFuture<Void> deleteRecord(ServerPlayerEntity player, UUID uuid) {
        return CompletableFuture.<Either<Unit, Throwable>>supplyAsync(() -> {
            Path path = getPath(rootPath(player), player.getUuid(), uuid);
            try {
                if (Files.deleteIfExists(path)) {
                    player.sendMessage(Text.translatable("command.speedrun.alphabet.delete", uuid));
                } else {
                    player.sendMessage(Text.translatable("command.speedrun.alphabet.delete.not_found", uuid));
                }
            } catch (IOException e) {
                return Either.right(e);
            }
            return Either.left(Unit.INSTANCE);
        }).thenCompose(either -> either.map(CompletableFuture::completedFuture, CompletableFuture::failedFuture)).thenAccept($->{});
    }

    public static CompletableFuture<Void> listRecords(ServerPlayerEntity player) {
        return CompletableFuture.<Either<Unit, Throwable>>supplyAsync(() -> {
            Path path = getPath(rootPath(player), player.getUuid(), null);
            if (Files.notExists(path)) {
                player.sendMessage(Text.translatable("command.speedrun.alphabet.list.empty").formatted(Formatting.RED));
                return Either.left(Unit.INSTANCE);
            }
            try (Stream<Path> paths = Files.list(path)) {
                List<Path> strings = paths.filter(p -> FILENAME_PATTERN.asMatchPredicate().test(p.getFileName().toString()))
                        .toList();
                player.sendMessage(Text.translatable("command.speedrun.alphabet.list.header", player.getDisplayName()));
                for (Path p : strings) {
                    JsonObject obj = GSON.fromJson(Files.newBufferedReader(p), JsonObject.class);
                    RecordSnapshot record = RecordSnapshot.fromPvpRecordJson(obj, player.server.getOverworld().getTime());
                    player.sendMessage(Text.literal(" * ").append(record.asText()));
                }
                player.sendMessage(Text.translatable("command.speedrun.alphabet.list.footer", strings.size()));
            } catch (IOException e) {
                return Either.right(e);
            }
            return Either.left(Unit.INSTANCE);
        }).thenCompose(either -> either.map(CompletableFuture::completedFuture, CompletableFuture::failedFuture)).thenAccept($->{});
    }

    static Path rootPath(ServerPlayerEntity player) {
        return player.server.getSavePath(WorldSavePath.ROOT);
    }
    public static final Pattern FILENAME_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}\\u002d[0-9a-fA-F]{4}\\u002d[0-9a-fA-F]{4}\\u002d[0-9a-fA-F]{4}\\u002d[0-9a-fA-F]{12}\\u002ejson$");
}
