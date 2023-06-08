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

package org.featurehouse.mcmod.speedrun.alphabeta.mixin.forge;

import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProvider;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.resource.PathPackResources;
import net.minecraftforge.resource.ResourcePackLoader;
import org.featurehouse.mcmod.speedrun.alphabeta.util.pack.forge.BuiltinABSPackProviderImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.Function;

@Mixin(value = ResourcePackLoader.class)
public class ForgePackLoaderMixin {
    @Inject(at = @At("RETURN"), method = "loadResourcePacks(Lnet/minecraft/resource/ResourcePackManager;Ljava/util/function/Function;)V")
    private static void addAbsPacks(ResourcePackManager resourcePacks, Function<Map<IModFile, ? extends PathPackResources>, ? extends ResourcePackProvider> packFinder, CallbackInfo ci) {
        resourcePacks.addPackFinder(BuiltinABSPackProviderImpl.ofABS());
        org.featurehouse.mcmod.speedrun.alphabeta.util.AlphaBetaDebug.log(3,l->l.info("Added ABSPacks"));
    }
}
