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
import com.google.gson.JsonObject;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import java.io.*;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class JsonYYDS {
    private static final Gson GSON = new Gson();

    public static JsonObject fromByteArray(byte[] arr) {
        try (var reader = new InputStreamReader(new GZIPInputStream(new ByteArrayInputStream(arr)))) {
            return GSON.fromJson(reader, JsonObject.class);
        } catch (IOException impossible) { throw new IncompatibleClassChangeError(); }
    }

    public static Optional<JsonObject> getFromNbtByteArray(NbtCompound root, String key) {
        if (root.contains(key, NbtElement.BYTE_ARRAY_TYPE)) {
            return Optional.of(fromByteArray(root.getByteArray(key)));
        } else return Optional.empty();
    }

    public static NbtByteArray toByteArray(JsonObject obj) {
        var buffer = new ByteArrayOutputStream();
        try (var writer = new OutputStreamWriter(new GZIPOutputStream(buffer))) {
            GSON.toJson(obj, writer);
        } catch (IOException e) { throw new IncompatibleClassChangeError(); }
        return new NbtByteArray(buffer.toByteArray());
    }
}
