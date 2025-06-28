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

package org.featurehouse.mcmod.speedrun.alphabeta.util.hooks;

import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;

@ApiStatus.Internal
public class MultiverseHooks {
    @DontObfuscate  // #5: getstatic #0 #11 #2; aload 0; invokevirtual #0 #15 #5; areturn
    public static HolderSet.Named<Item> itemTagHolders(TagKey<Item> tagKey) {
        return BuiltInRegistries.ITEM.getOrThrow(tagKey);
    }

    @DontObfuscate  // #7: getstatic #0 #11 #2; aload 0; invokevirtual #0 #16 #6; areturn
    public static ResourceLocation itemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    @DontObfuscate  // #9: getstatic #0 #11 #2; aload 0; invokevirtual #0 #17 #8; checkcast #1; areturn
    public static Item getItem(ResourceLocation id) {
        return BuiltInRegistries.ITEM.getValue(id);
    }

    @DontObfuscate
    public static Item getItem(ResourceKey<Item> registryKey) {
        return BuiltInRegistries.ITEM.getValue(registryKey);
    }

    @DontObfuscate  // #10: getstatic #0 #11 #2; aload 0; invokevirtual #0 #18 #10; areturn
    public static Optional<Item> getOptionalItem(ResourceLocation id) {
        return BuiltInRegistries.ITEM.getOptional(id);
    }

    @DontObfuscate  // ()#3: getstatic #0 #12 #3; areturn
    public static ResourceKey<Registry<Item>> itemKey() {
        return Registries.ITEM;
    }

    @DontObfuscate  // ()#3: getstatic #0 #14 #3; areturn
    public static ResourceKey<Registry<MenuType<?>>> menuKey() {
        return Registries.MENU;
    }
}
