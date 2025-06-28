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

import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.featurehouse.mcmod.speedrun.alphabeta.item.coop.CoopRecordManager;
import org.featurehouse.mcmod.speedrun.alphabeta.item.coop.CoopablePlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
abstract class PlayerListMixin implements CoopablePlayerList {
    @DontObfuscate
    @Unique
    private CoopRecordManager alphabetSpeedrun$coopRecordManager;

    @Inject(at = @At("RETURN"), method = "<init>")
    private void postInit(MinecraftServer server, LayeredRegistryAccess<?> registryManager, PlayerDataStorage saveHandler, int maxPlayers, CallbackInfo ci) {
        alphabetSpeedrun$coopRecordManager = new CoopRecordManager(server.getWorldPath(LevelResource.ROOT).resolve("alphabet-speedrun-records/coop/item"));
    }

    @Inject(at = @At("RETURN"), method = "saveAll")
    private void onSave(CallbackInfo ci) {
        alphabetSpeedrun$coopRecordManager.safeSave();
    }

    @Override
    public CoopRecordManager alphabetSpeedrun$getCoopManager() {
        return alphabetSpeedrun$coopRecordManager;
    }
}
