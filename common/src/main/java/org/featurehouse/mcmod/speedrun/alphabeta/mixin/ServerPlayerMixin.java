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
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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

import java.util.function.Function;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player implements ItemCollector, InternalItemCollector {
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

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void onReadFromNbt(ValueInput view, CallbackInfo ci) {
        this.alphabetSpeedrun$currentRecord = JsonYYDS.getFromReadView(view, "AlphabetSpeedrun_CurrentRecord")
                .map(obj -> ItemRecordAccess.metaCodec(CoopRecordManager.fromServer(alphabetSpeedrun$getServer()))
                        .parse(JsonOps.INSTANCE, obj)
                        .mapOrElse(Function.identity(), e -> {
                            ItemSpeedrunEvents.LOGGER.error("Failed to read player custom data from {}: {}", this.stringUUID, e.message());
                            return null;
                        })
                ).orElse(null);
        this.alphabetSpeedrun$itemRecordHistory = JsonYYDS.getFromReadView(view, "AlphabetSpeedrun_HistoryRecord").orElse(null);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void onWriteToNbt(ValueOutput view, CallbackInfo ci) {
        if (alphabetSpeedrun$currentRecord != null) {
            ItemRecordAccess.metaCodec(CoopRecordManager.fromServer(alphabetSpeedrun$getServer()))
                    .encodeStart(JsonOps.INSTANCE, alphabetSpeedrun$currentRecord)
                    .ifError(e -> {
                        ItemSpeedrunEvents.LOGGER.error("Failed to write player custom data to {}: {}", this.stringUUID, e.message());
                        org.featurehouse.mcmod.speedrun.alphabeta.util.AlphaBetaDebug.log(4, l -> l.info("SPMixin(onWriteToNbt) PartialValue: {}", e.partialValue().orElse(com.google.gson.JsonNull.INSTANCE)));
                    })
                    .ifSuccess(e -> {
                        JsonObject jsonMeta = GsonHelper.convertToJsonObject(e, "AlphabetSpeedrun_CurrentRecord");
                        JsonYYDS.writeToWriteView(jsonMeta, view, "AlphabetSpeedrun_CurrentRecord");
                    });
        }

        if (this.alphabetSpeedrun$itemRecordHistory != null) {
            JsonYYDS.writeToWriteView(alphabetSpeedrun$itemRecordHistory, view, "AlphabetSpeedrun_HistoryRecord");
        }
    }

    @Override
    public boolean alphabetSpeedrun$moveRecordToHistory() {
        if (alphabetSpeedrun$currentRecord == null || alphabetSpeedrun$currentRecord.isCoop()) return false;
        ItemRecordAccess.metaCodec(CoopRecordManager.fromServer(alphabetSpeedrun$getServer())).encodeStart(JsonOps.INSTANCE, alphabetSpeedrun$currentRecord)
                .flatMap(e -> {
                    if (!e.isJsonObject()) return DataResult.error(() -> "Not a JSON object");
                    return DataResult.success(e.getAsJsonObject());
                })
                .ifError(e -> ItemSpeedrunEvents.LOGGER.error("Failed to move record to history for {}: {}", this.stringUUID, e.message()))
                .ifSuccess(obj -> {
                    alphabetSpeedrun$itemRecordHistory = obj;
                    alphabetSpeedrun$currentRecord = null;
                });
        return true;
    }

    @Override
    public boolean alphabetSpeedrun$resumeLocalHistory() {
        if (alphabetSpeedrun$itemRecordHistory == null) return false;
        
        ItemSpeedrunRecord.CODEC.parse(JsonOps.INSTANCE, alphabetSpeedrun$itemRecordHistory)
                .ifError(e -> ItemSpeedrunEvents.LOGGER.error("Failed to resume local history for {}: {}", this.stringUUID, e.message()))
                .ifSuccess(r -> {
                    alphabetSpeedrun$currentRecord = r;
                    alphabetSpeedrun$itemRecordHistory = null;
                });
        
        return true;
    }

    @Override
    public void alphabetSpeedrun$clearItemHistory() {
        alphabetSpeedrun$itemRecordHistory = null;
    }

    @Override
    public ItemSpeedrunRecord alphabetSpeedrun$getHistory() {
        if (alphabetSpeedrun$itemRecordHistory == null) return null;
        return ItemSpeedrunRecord.CODEC.parse(JsonOps.INSTANCE, alphabetSpeedrun$itemRecordHistory).getOrThrow();
    }

    @Inject(at = @At("RETURN"), method = "initMenu")
    @SuppressWarnings("all")
    private void onOpenMenu(AbstractContainerMenu screenHandler, CallbackInfo ci) {
        screenHandler.addSlotListener(new InventoryListener(((ServerPlayer) (Object) this)));
    }

    @Inject(method = "restoreFrom", at = @At("RETURN"))
    private void copyMyself(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        this.alphabetSpeedrun$currentRecord = oldPlayer.alphabetSpeedrun$getItemRecordAccess();
        this.alphabetSpeedrun$itemRecordHistory = oldPlayer.alphabetSpeedrun$internal$getHistoryRaw();
    }

    @Override
    public JsonObject alphabetSpeedrun$internal$getHistoryRaw() {
        return this.alphabetSpeedrun$itemRecordHistory;
    }
}
