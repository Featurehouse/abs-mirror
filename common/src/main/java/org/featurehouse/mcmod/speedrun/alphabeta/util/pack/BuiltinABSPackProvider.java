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

package org.featurehouse.mcmod.speedrun.alphabeta.util.pack;

import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.util.JsonHelper;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@ApiStatus.Internal
public class BuiltinABSPackProvider {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Supplier<List<String>> SUB_PATHS = Suppliers.memoize(BuiltinABSPackProvider::initSubPaths);

    public static void init() {
        SUB_PATHS.get();
        registerPacks();
    }

    @ExpectPlatform
    private static void registerPacks() {
        throw new IncompatibleClassChangeError("ExpectPlatform");
    }

    private static List<String> initSubPaths() {
        final Path meta = readMeta();
        if (meta != null && Files.isRegularFile(meta)) {
            try (var r = GSON.newJsonReader(Files.newBufferedReader(meta))) {
                final JsonObject o = GSON.fromJson(r, JsonObject.class);
                final JsonArray packs = JsonHelper.getArray(o, "packs");
                List<String> l = Lists.newArrayList();
                packs.forEach(e -> l.add(JsonHelper.asString(e, "pack_name")));
                LOGGER.info("ABS: loaded packs: {}", l);
                return l;
            } catch (Exception e) {
                LOGGER.error("ABS: Can't read ABS builtin packs");
            }
        }
        return Collections.emptyList();
    }

    @ExpectPlatform
    private static Path readMeta() {
        throw new IncompatibleClassChangeError("ExpectPlatform");
    }
}
