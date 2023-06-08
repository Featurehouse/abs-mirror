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

package org.featurehouse.mcmod.speedrun.alphabeta.util.pack.fabric;

import com.google.common.base.Suppliers;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.util.Identifier;
import org.featurehouse.mcmod.speedrun.alphabeta.util.pack.BuiltinABSPackProvider;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;
import java.util.function.Supplier;

@ApiStatus.Internal
public class BuiltinABSPackProviderImpl {
    private static final Supplier<ModContainer> MOD = Suppliers.memoize(() -> FabricLoader.getInstance()
            .getModContainer("alphabet_speedrun")
            .orElseThrow(() -> new IllegalStateException("Mod not found")));

    public static Path readMeta() {
        return MOD.get().findPath("abs_builtin_packs.json").orElse(null);
    }

    public static void registerPacks() {
        for (String s : BuiltinABSPackProvider.SUB_PATHS.get()) {
            ResourceManagerHelper.registerBuiltinResourcePack(
                    new Identifier("alphabet_speedrun", s),
                    MOD.get(),
                    ResourcePackActivationType.DEFAULT_ENABLED);
        }
    }
}
