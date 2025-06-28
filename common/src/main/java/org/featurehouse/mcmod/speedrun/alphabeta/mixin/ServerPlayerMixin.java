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

import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import org.featurehouse.mcmod.speedrun.alphabeta.item.*;
import org.featurehouse.mcmod.speedrun.alphabeta.item.coop.CoopRecordManager;
import org.featurehouse.mcmod.speedrun.alphabeta.util.JsonYYDS;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerMixin extends PlayerEntity implements ItemCollector, InternalItemCollector {
    @Unique
    @DontObfuscate
    @Nullable
    ItemRecordAccess alphabetSpeedrun$currentRecord;
    @Unique
    @DontObfuscate
    @Nullable
    private JsonObject alphabetSpeedrun$itemRecordHistory;

    @SuppressWarnings("all") ServerPlayerMixin() {super(null, null);}

    @Accessor("server") @DontObfuscate public abstract MinecraftServer alphabetSpeedrun$getServer();

    @Override
    public ItemRecordAccess alphabetSpeedrun$getItemRecordAccess() {
        return alphabetSpeedrun$currentRecord;
    }

    @Override
    public void alphabetSpeedrun$setItemRecordAccess(@Nullable ItemRecordAccess record) {
        alphabetSpeedrun$currentRecord = record;
    }

    @Inject(method = "readCustomData", at = @At("RETURN"))
    private void onReadFromNbt(ReadView view, CallbackInfo ci) {
        try {
            this.alphabetSpeedrun$currentRecord = JsonYYDS.getFromReadView(view, "AlphabetSpeedrun_CurrentRecord")
                    .map(obj -> ItemRecordAccess.fromJsonMeta(obj, CoopRecordManager.fromServer(alphabetSpeedrun$getServer())))
                    .orElse(null);
        } catch (RuntimeException e) {
            ItemSpeedrunEvents.LOGGER.error("Failed to read player custom data from {}", this.uuidString, e);
        }
        this.alphabetSpeedrun$itemRecordHistory = JsonYYDS.getFromReadView(view, "AlphabetSpeedrun_HistoryRecord").orElse(null);
    }

    @Inject(method = "writeCustomData", at = @At("RETURN"))
    private void onWriteToNbt(WriteView view, CallbackInfo ci) {
        if (alphabetSpeedrun$currentRecord != null) {
            JsonYYDS.writeToWriteView(alphabetSpeedrun$currentRecord.toJsonMeta(), view, "AlphabetSpeedrun_CurrentRecord");
        }
        if (this.alphabetSpeedrun$itemRecordHistory != null) {
            JsonYYDS.writeToWriteView(alphabetSpeedrun$itemRecordHistory, view, "AlphabetSpeedrun_HistoryRecord");
        }
    }

    @Override
    public boolean alphabetSpeedrun$moveRecordToHistory() {
        if (alphabetSpeedrun$currentRecord == null || alphabetSpeedrun$currentRecord.isCoop()) return false;
        alphabetSpeedrun$itemRecordHistory = alphabetSpeedrun$currentRecord.toJson();
        alphabetSpeedrun$currentRecord = null;
        return true;
    }

    @Override
    public boolean alphabetSpeedrun$resumeLocalHistory() {
        if (alphabetSpeedrun$itemRecordHistory == null) return false;
        alphabetSpeedrun$currentRecord = ItemSpeedrunRecord.fromJson(alphabetSpeedrun$itemRecordHistory, false);
        alphabetSpeedrun$itemRecordHistory = null;
        return true;
    }

    @Override
    public void alphabetSpeedrun$clearItemHistory() {
        alphabetSpeedrun$itemRecordHistory = null;
    }

    @Override
    public ItemSpeedrunRecord alphabetSpeedrun$getHistory() {
        return alphabetSpeedrun$itemRecordHistory == null ? null :
                ItemSpeedrunRecord.fromJson(alphabetSpeedrun$itemRecordHistory, false);
    }

    @Inject(at = @At("RETURN"), method = "onScreenHandlerOpened")
    @SuppressWarnings("all")
    private void onOpenMenu(ScreenHandler screenHandler, CallbackInfo ci) {
        screenHandler.addListener(new InventoryListener(((ServerPlayerEntity) (Object) this)));
    }

    @Inject(method = "copyFrom", at = @At("RETURN"))
    private void copyMyself(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        this.alphabetSpeedrun$currentRecord = oldPlayer.alphabetSpeedrun$getItemRecordAccess();
        this.alphabetSpeedrun$itemRecordHistory = oldPlayer.alphabetSpeedrun$internal$getHistoryRaw();
    }

    @Override
    public JsonObject alphabetSpeedrun$internal$getHistoryRaw() {
        return this.alphabetSpeedrun$itemRecordHistory;
    }
}
