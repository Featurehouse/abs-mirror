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
import org.featurehouse.mcmod.speedrun.alphabeta.util.JsonYYDS;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

public record ItemSpeedrun(
        ResourceLocation id,
        ItemStack icon,
        Component display,
        List<ItemPredicateProvider> items
) {
    @Override
    public ItemStack icon() {
        return icon.copy();
    }

    @Override
    public Component display() {
        return display.copy();
    }

    @Nullable
    public static ItemSpeedrun get(ResourceLocation id) {
        return DataLoader.getCurrentData().get(id);
    }

    @ParametersAreNonnullByDefault
    public static class DataLoader extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
        private static Map<ResourceLocation, ItemSpeedrun> currentData;
        private static final Logger LOGGER = LogUtils.getLogger();

        private static final Object LOCK = new Object();

        @Override
        @NotNull
        protected Map<ResourceLocation, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
            return JsonYYDS.loadJsonResources(manager, FileToIdConverter.json("speedrun_goals/item"));
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, ItemSpeedrun> m = new HashMap<>();
            prepared.forEach((id, json) -> {
                final JsonObject root = GsonHelper.convertToJsonObject(json, id.toString());
                ItemStack icon = iconFromJson(GsonHelper.getAsJsonObject(root, "icon"));
                Component display = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, Objects.requireNonNull(root.get("display"))).getOrThrow(JsonParseException::new);
                //TagKey<Item> tagKey = TagKey.of(Registry.ITEM_KEY, Identifier.of(JsonHelper.getString(root, "items")));
                List<ItemPredicateProvider> providers = ItemPredicateProvider.fromJson(Objects.requireNonNull(root.get("items")));
                m.put(id, new ItemSpeedrun(id, icon, display, providers));
            });
            synchronized (LOCK) {
                currentData = m;
            }
        }

        public static Map<ResourceLocation, ItemSpeedrun> getCurrentData() {
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
