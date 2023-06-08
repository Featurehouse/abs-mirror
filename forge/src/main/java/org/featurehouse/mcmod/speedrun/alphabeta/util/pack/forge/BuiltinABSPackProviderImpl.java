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

package org.featurehouse.mcmod.speedrun.alphabeta.util.pack.forge;

import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackProvider;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.resource.PathPackResources;
import org.apache.commons.lang3.ArrayUtils;
import org.featurehouse.mcmod.speedrun.alphabeta.util.pack.BuiltinABSPackProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

@ApiStatus.Internal
public class BuiltinABSPackProviderImpl implements ResourcePackProvider {
    final String modId;
    final List<String> packs;
    private static final ResourcePackSource SOURCE = ResourcePackSource.BUILTIN;

    public static ResourcePackProvider ofABS() {
        return new BuiltinABSPackProviderImpl("alphabet_speedrun", BuiltinABSPackProvider.SUB_PATHS.get());
    }

    BuiltinABSPackProviderImpl(String modId, List<String> packs) {
        this.modId = modId;
        this.packs = packs;
    }

    @Override
    public void register(Consumer<ResourcePackProfile> profileAdder) {
        //PathPackResources res = ResourcePackLoader.getPackFor(modId).orElseThrow(() -> new IllegalStateException("mod not found"));

        IModFile modFile = ModList.get().getModFileById(modId).getFile();

        for (String packId: packs) {
            ResourcePackProfile profile = ResourcePackProfile.create(
                    packId,
                    Text.literal(packId),
                    false,
                    id -> new PathPackResources("alphabet_speedrun/" + id, false, modFile.findResource("resourcepacks", packId)) {
                        @Override
                        protected @NotNull Path resolve(String @NotNull ... paths) {
                            return modFile.findResource(ArrayUtils.insert(0, paths, "resourcepacks", packId));
                        }
                    },
                    ResourceType.SERVER_DATA,
                    ResourcePackProfile.InsertionPosition.TOP,
                    SOURCE
            );
            //org.featurehouse.mcmod.speedrun.alphabeta.util.AlphaBetaDebug.log(3,l->l.info("ABSForge ProfileName: {}", profile.getName()));
            profileAdder.accept(profile);
        }
    }

    public static Path readMeta() {
        return ModList.get().getModFileById("alphabet_speedrun").getFile().findResource("abs_builtin_packs.json");
    }

    public static void registerPacks() {
        // NO-OP
    }
}
