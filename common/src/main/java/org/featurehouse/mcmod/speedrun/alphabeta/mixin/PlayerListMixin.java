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

package org.featurehouse.mcmod.speedrun.alphabeta.mixin;

import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.registry.CombinedDynamicRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.WorldSaveHandler;
import org.featurehouse.mcmod.speedrun.alphabeta.item.coop.CoopRecordManager;
import org.featurehouse.mcmod.speedrun.alphabeta.item.coop.CoopablePlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
abstract class PlayerListMixin implements CoopablePlayerList {
    @DontObfuscate private CoopRecordManager alphabetSpeedrun$coopRecordManager;

    @Inject(at = @At("RETURN"), method = "<init>")
    private void postInit(MinecraftServer server, @Coerce Object arg2, WorldSaveHandler saveHandler, int maxPlayers, CallbackInfo ci) {
        alphabetSpeedrun$coopRecordManager = new CoopRecordManager(server.getSavePath(WorldSavePath.ROOT).resolve("alphabet-speedrun-records/coop/item"));
    }

    @Inject(at = @At("RETURN"), method = "saveAllPlayerData")
    private void onSave(CallbackInfo ci) {
        alphabetSpeedrun$coopRecordManager.safeSave();
    }

    @Override
    public CoopRecordManager alphabetSpeedrun$getCoopManager() {
        return alphabetSpeedrun$coopRecordManager;
    }
}
