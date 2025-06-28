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

package org.featurehouse.mcmod.speedrun.alphabeta.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.StrictJsonParser;
import org.slf4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class JsonYYDS {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogUtils.getLogger();

    public static JsonObject fromByteArray(byte[] arr) {
        try (var reader = new InputStreamReader(new GZIPInputStream(new ByteArrayInputStream(arr)))) {
            return GSON.fromJson(reader, JsonObject.class);
        } catch (IOException impossible) { throw new IncompatibleClassChangeError(); }
    }

    public static Optional<JsonObject> getFromReadView(ReadView view, String key) {
        return view.read(key, Codec.BYTE_BUFFER).map(ByteBuffer::array).map(JsonYYDS::fromByteArray);
    }

    public static void writeToWriteView(JsonObject obj, WriteView view, String key) {
        var buffer = new ByteArrayOutputStream();
        try (var writer = new OutputStreamWriter(new GZIPOutputStream(buffer))) {
            GSON.toJson(obj, writer);
        } catch (IOException e) { throw new IncompatibleClassChangeError(); }
        view.put(key, Codec.BYTE_BUFFER, ByteBuffer.wrap(buffer.toByteArray()));
    }

    public static Map<Identifier, JsonElement> loadJsonResources(ResourceManager manager, ResourceFinder finder) {
        Map<Identifier, JsonElement> map = new LinkedHashMap<>();

        for (Map.Entry<Identifier, Resource> entry : finder.findResources(manager).entrySet()) {
            Identifier id = entry.getKey();
            Identifier key = finder.toResourceId(id);
            Resource resource = entry.getValue();

            try (Reader reader = resource.getReader()) {
                if (map.putIfAbsent(key, StrictJsonParser.parse(reader)) != null)
                    throw new IllegalStateException("Duplicate data file ignored with ID " + key);
            } catch (IllegalArgumentException | IOException | JsonParseException e) {
                LOGGER.error("Couldn't parse data file '{}' from '{}'", key, id, e);
            }
        }
        return map;
    }
}
