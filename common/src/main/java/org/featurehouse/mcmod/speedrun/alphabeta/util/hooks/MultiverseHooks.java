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

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;

@ApiStatus.Internal
public class MultiverseHooks {
    @DontObfuscate  // #5: getstatic #0 #11 #2; aload 0; invokevirtual #0 #15 #5; areturn
    public static RegistryEntryList.Named<Item> itemTagHolders(TagKey<Item> tagKey) {
        return Registries.ITEM.getOrCreateEntryList(tagKey);
    }

    @DontObfuscate  // #7: getstatic #0 #11 #2; aload 0; invokevirtual #0 #16 #6; areturn
    public static Identifier itemId(Item item) {
        return Registries.ITEM.getId(item);
    }

    @DontObfuscate  // #9: getstatic #0 #11 #2; aload 0; invokevirtual #0 #17 #8; checkcast #1; areturn
    public static Item getItem(Identifier id) {
        return Registries.ITEM.get(id);
    }

    @DontObfuscate  // #10: getstatic #0 #11 #2; aload 0; invokevirtual #0 #18 #10; areturn
    public static Optional<Item> getOptionalItem(Identifier id) {
        return Registries.ITEM.getOrEmpty(id);
    }

    @DontObfuscate  // #10: getstatic #0 #13 #4; aload 0; invokevirtual #0 #18 #10; areturn
    public static Optional<Enchantment> getOptionalEnchantment(Identifier id) {
        return Registries.ENCHANTMENT.getOrEmpty(id);
    }

    @DontObfuscate  // ()#3: getstatic #0 #12 #3; areturn
    public static RegistryKey<Registry<Item>> itemKey() {
        return RegistryKeys.ITEM;
    }

    @DontObfuscate  // ()#3: getstatic #0 #14 #3; areturn
    public static RegistryKey<Registry<ScreenHandlerType<?>>> menuKey() {
        return RegistryKeys.SCREEN_HANDLER;
    }
}
