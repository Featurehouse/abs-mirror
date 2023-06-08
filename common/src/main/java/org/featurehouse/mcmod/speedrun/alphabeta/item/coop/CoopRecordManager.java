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

package org.featurehouse.mcmod.speedrun.alphabeta.item.coop;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import org.featurehouse.mcmod.speedrun.alphabeta.item.ItemRecordAccess;
import org.featurehouse.mcmod.speedrun.alphabeta.item.StoredItemRecords;
import org.featurehouse.mcmod.speedrun.alphabeta.util.SavableResource;
import org.slf4j.Logger;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class CoopRecordManager implements SavableResource {
    private final Path rootDir;
    private final Map<UUID, CoopRecordAccess> inMemoryRecords = new LinkedHashMap<>();
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogUtils.getLogger();

    public CoopRecordManager(Path rootDir) {
        this.rootDir = rootDir;
        this.safeLoad();
    }


    @CheckForNull
    public CoopRecordAccess get(UUID uuid) {
        final CoopRecordAccess acc = inMemoryRecords.get(uuid);
        if (acc != null) return acc;
        LOGGER.debug("Coop record {} is invalid or absent", uuid);
        org.featurehouse.mcmod.speedrun.alphabeta.util.AlphaBetaDebug.log(2,l->l.warn("IM={}",inMemoryRecords));
        return null;
    }

    @Deprecated
    public CoopRecordAccess readOne(UUID uuid) throws IOException {
        return readOne(getPath(uuid));
    }

    @CheckForNull   // TODO: enable short name
    @Deprecated
    public CoopRecordAccess readOne(String shortName) throws IOException {
        try {
            return readOne(getPath(shortName));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Failed to find {}", shortName, e);
            return null;
        }
    }

    private CoopRecordAccess readOne(Path p) throws IOException {
        JsonObject root;
        try (var reader = Files.newBufferedReader(p)) {
            root = GSON.fromJson(reader, JsonObject.class);
        }
        CoopRecord e = CoopRecord.fromJson(root);
        e.getPlayers().clear();
        synchronized (this) {
            inMemoryRecords.put(e.recordId(), e);
        }
        // TODO: try invite all
        return e;
    }

    public void load() throws IOException {
        if (!Files.exists(rootDir)) {
            Files.createDirectories(rootDir);
        }

        try (var l = Files.list(rootDir)) {
            l.forEach(p -> {
                if (!StoredItemRecords.FILENAME_PATTERN.matcher(p.getFileName().toString()).matches()) {
                    throw new UncheckedIOException(new IOException("Invalid coop record id"));
                }
                try {
                    readOne(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public void save() throws IOException {
        for (ItemRecordAccess rec : inMemoryRecords.values()) {
            JsonObject json = rec.toJson();
            try (var writer = Files.newBufferedWriter(getPath(rec.recordId()))) {
                GSON.toJson(json, writer);
            }
        }
        synchronized (this) {
            inMemoryRecords.clear();
        }
    }

    public Path getPath(String shortName) throws IOException, IllegalArgumentException {
        var c = ItemRecordAccess.pathFromShortName(rootDir, shortName);
        if (c.size() != 1) throw new IllegalArgumentException("Too many paths matching " + shortName);
        return c.iterator().next();
    }

    static final int DUP = 1, ABSENT = 0, INVALID = -1;

    @Deprecated
    Either<CoopRecordAccess, Integer> getShortName(String shortName) {
        if (!ItemRecordAccess.SHORT_NAME_PATTERN.matcher(shortName).matches()) {
            return Either.right(INVALID);
        }
        int i = Integer.parseInt(shortName, 16);
        CoopRecordAccess obj = null;
        for (Map.Entry<UUID, CoopRecordAccess> entry : inMemoryRecords.entrySet()) {
            if ((int) (entry.getKey().getMostSignificantBits() >>> 48) == i) {
                if (obj == null)
                    obj = entry.getValue();
                else
                    return Either.right(DUP);
            }
        }
        if (obj == null)
            return Either.right(ABSENT);
        return Either.left(obj);
    }

    private Path getPath(UUID uuid) {
        return rootDir.resolve(uuid + ".json");
    }

    public static CoopRecordManager fromServer(MinecraftServer server) {
        return ((CoopablePlayerList) server.getPlayerManager()).alphabetSpeedrun$getCoopManager();
    }
}
