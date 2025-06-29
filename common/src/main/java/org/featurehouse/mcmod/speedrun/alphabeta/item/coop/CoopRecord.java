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

package org.featurehouse.mcmod.speedrun.alphabeta.item.coop;

import com.google.gson.JsonObject;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import org.featurehouse.mcmod.speedrun.alphabeta.item.ItemSpeedrunRecord;
import org.featurehouse.mcmod.speedrun.alphabeta.item.SingleSpeedrunPredicate;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.ItemSpeedrunDifficulty;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;

public class CoopRecord implements CoopRecordAccess {
    private final ItemSpeedrunRecord wrapped;
    private final List<UUID> operators;
    private final List<UUID> players;

    public CoopRecord(ItemSpeedrunRecord wrapped, List<UUID> operators, List<UUID> players) {
        this.wrapped = wrapped;
        this.operators = operators;
        this.players = players;
    }


    @Override
    public Collection<ServerPlayer> getMates(PlayerList manager, ServerPlayer self) {
        return players.parallelStream()
                .map(manager::getPlayer)
                .filter(p -> p != null && !p.equals(self))
                .collect(Collectors.toSet());
    }

    @Override
    public void onStart(ServerPlayer player) {
        getPlayers().add(player.getUUID());
        player.alphabetSpeedrun$setItemRecordAccess(this);
        difficulty().onStart(player);
        // TODO: multiplayer onStart: write in difficulty.onStart
    }

    ///////////

    public List<UUID> getPlayers() {
        return players;
    }

    @Override
    public UUID recordId() {
        return wrapped.recordId();
    }

    @Override
    public ResourceLocation goalId() {
        return wrapped.goalId();
    }

    public List<UUID> getOperators() {
        return operators;
    }

    @Override
    public List<SingleSpeedrunPredicate> predicates() {
        return wrapped.predicates();
    }

    @Override
    public long[] collected() {
        return wrapped.collected();
    }

    @Override
    public long startTime() {
        return wrapped.startTime();
    }

    @Override
    public long finishTime() {
        return wrapped.finishTime();
    }

    @Override
    public void setFinishTime(long finishTime) {
        wrapped.setFinishTime(finishTime);
    }

    @Override
    public long lastQuitTime() {
        return wrapped.lastQuitTime();
    }

    @Override
    public void setLastQuitTime(long lastQuitTime) {
        wrapped.setLastQuitTime(lastQuitTime);
    }

    @Override
    public long vacantTime() {
        return wrapped.vacantTime();
    }

    @Override
    public void setVacantTime(long vacantTime) {
        wrapped.setVacantTime(vacantTime);
    }

    @Override
    public ItemSpeedrunDifficulty difficulty() {
        return wrapped.difficulty();
    }

    @Override
    public List<ItemStack> displayedStacks() {
        return wrapped.displayedStacks();
    }

    @Override
    public boolean tryMarkDone(long currentTime) {
        return wrapped.tryMarkDone(currentTime);
    }

    @Override
    public boolean isAllRequirementsPassed() {
        return wrapped.isAllRequirementsPassed();
    }

    @Override
    public boolean isFinished() {
        return wrapped.isFinished();
    }

    @Override
    public boolean isRequirementPassed(int idx) {
        return wrapped.isRequirementPassed(idx);
    }

    @Override
    public void setRequirementPassedTime(int index, long time) {
        wrapped.setRequirementPassedTime(index, time);
    }

    @Override
    public int getCollectedCount() {
        return wrapped.getCollectedCount();
    }

    public static @Nullable CoopRecordAccess tryParseMeta(CoopRecordManager manager, JsonObject obj) {
        if (!GsonHelper.getAsBoolean(obj, "is_coop", false))
            return null;
        UUID uuid = UUID.fromString(GsonHelper.getAsString(obj, "coop_uuid"));
        return manager.get(uuid);
    }

    public static MapCodec<CoopRecordAccess> metaCodec(CoopRecordManager manager) {
        return RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                        UUIDUtil.STRING_CODEC.xmap(manager::get, CoopRecordAccess::recordId).fieldOf("coop_uuid").forGetter(Function.identity())
                ).apply(instance, Function.identity())
        );
    }

    public static final Codec<CoopRecord> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    ItemSpeedrunRecord.CODEC.fieldOf("record").forGetter(r -> r.wrapped),
                    UUIDUtil.STRING_CODEC.listOf().fieldOf("operators").forGetter(CoopRecord::getOperators),
                    UUIDUtil.STRING_CODEC.listOf().fieldOf("player").forGetter(CoopRecord::getPlayers)
            ).apply(instance, CoopRecord::new)
    );

    @Override
    public long timeSince(long current) {
        return wrapped.timeSince(current);
    }

    @Override
    public void onStop(Collection<? extends ServerPlayer> players) {
        CoopRecordAccess.super.onStop(players);
    }

    @Override
    public void sudoJoin(UUID hostId, Collection<? extends ServerPlayer> players) {
        // what's it for
    }
}
