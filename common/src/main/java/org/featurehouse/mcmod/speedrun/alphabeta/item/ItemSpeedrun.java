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

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.profiler.Profiler;
import org.featurehouse.mcmod.speedrun.alphabeta.util.JsonYYDS;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

public record ItemSpeedrun(
        Identifier id,
        ItemStack icon,
        Text display,
        List<ItemPredicateProvider> items
) {
    @Override
    public ItemStack icon() {
        return icon.copy();
    }

    @Override
    public Text display() {
        return display.copy();
    }

    @Nullable
    public static ItemSpeedrun get(Identifier id) {
        return DataLoader.getCurrentData().get(id);
    }

    public static class DataLoader extends SinglePreparationResourceReloader<Map<Identifier, JsonElement>> {
        private static Map<Identifier, ItemSpeedrun> currentData;
        private static final Logger LOGGER = LogUtils.getLogger();

        private static final Object LOCK = new Object();

        @Override
        protected Map<Identifier, JsonElement> prepare(ResourceManager manager, Profiler profiler) {
            return JsonYYDS.loadJsonResources(manager, ResourceFinder.json("speedrun_goals/item"));
        }

        @Override
        protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
            Map<Identifier, ItemSpeedrun> m = new HashMap<>();
            prepared.forEach((id, json) -> {
                final JsonObject root = JsonHelper.asObject(json, id.toString());
                ItemStack icon = iconFromJson(JsonHelper.getObject(root, "icon"));
                Text display = TextCodecs.CODEC.parse(JsonOps.INSTANCE, Objects.requireNonNull(root.get("display"))).getOrThrow(JsonParseException::new);
                //TagKey<Item> tagKey = TagKey.of(Registry.ITEM_KEY, Identifier.of(JsonHelper.getString(root, "items")));
                List<ItemPredicateProvider> providers = ItemPredicateProvider.fromJson(Objects.requireNonNull(root.get("items")));
                m.put(id, new ItemSpeedrun(id, icon, display, providers));
            });
            synchronized (LOCK) {
                currentData = m;
            }
        }

        public static Map<Identifier, ItemSpeedrun> getCurrentData() {
            synchronized (LOCK) {
                if (currentData == null) {
                    LOGGER.warn("Trying to query current data which is not initialized");
                    return Collections.emptyMap();
                } else {
                    return Collections.unmodifiableMap(currentData);
                }
            }
        }

        static ItemStack iconFromJson(JsonObject json) {
            ItemStack stack = ItemStack.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(JsonParseException::new);
            stack.setCount(1);
            return stack;
        }
    }
}
