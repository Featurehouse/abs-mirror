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
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.profiler.Profiler;
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

    public static class DataLoader extends JsonDataLoader {
        private static Map<Identifier, ItemSpeedrun> currentData;
        private static final Logger LOGGER = LogUtils.getLogger();

        private static final Gson GSON = new Gson();
        private static final Object LOCK = new Object();

        public DataLoader() {
            super(GSON, "speedrun_goals/item");
        }

        @Override
        protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
            Map<Identifier, ItemSpeedrun> m = new HashMap<>();
            prepared.forEach((id, json) -> {
                final JsonObject root = JsonHelper.asObject(json, id.toString());
                ItemStack icon = iconFromJson(JsonHelper.getObject(root, "icon"));
                Text display = Text.Serializer.fromJson(Objects.requireNonNull(root.get("display")));
                //TagKey<Item> tagKey = TagKey.of(Registry.ITEM_KEY, new Identifier(JsonHelper.getString(root, "items")));
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
            if (!json.has("item")) {
                throw new JsonSyntaxException("Unsupported icon type, currently only items are supported (add 'item' key)");
            } else {
                Item item = JsonHelper.getItem(json, "item");
                if (json.has("data")) {
                    throw new JsonParseException("Disallowed data tag found");
                } else {
                    ItemStack itemStack = new ItemStack(item);
                    if (json.has("nbt")) {
                        try {
                            NbtCompound nbtCompound = StringNbtReader.parse(JsonHelper.asString(json.get("nbt"), "nbt"));
                            itemStack.setNbt(nbtCompound);
                        } catch (CommandSyntaxException var4) {
                            throw new JsonSyntaxException("Invalid nbt tag: " + var4.getMessage());
                        }
                    }

                    return itemStack;
                }
            }
        }
    }
}
