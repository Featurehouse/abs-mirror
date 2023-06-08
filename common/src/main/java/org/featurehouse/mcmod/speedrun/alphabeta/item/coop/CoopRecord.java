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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.featurehouse.mcmod.speedrun.alphabeta.item.ItemSpeedrunRecord;
import org.featurehouse.mcmod.speedrun.alphabeta.item.SingleSpeedrunPredicate;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.ItemSpeedrunDifficulty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class CoopRecord implements CoopRecordAccess {
    private final ItemSpeedrunRecord wrapped;
    private final Collection<UUID> operators;
    private final Collection<UUID> players;

    public CoopRecord(ItemSpeedrunRecord wrapped, Collection<UUID> operators, Collection<UUID> players) {
        this.wrapped = wrapped;
        this.operators = operators;
        this.players = players;
    }


    @Override
    public Collection<ServerPlayerEntity> getMates(PlayerManager manager, ServerPlayerEntity self) {
        return players.parallelStream()
                .map(manager::getPlayer)
                .filter(p -> p != null && !p.equals(self))
                .collect(Collectors.toSet());
    }

    @Override
    public void onStart(ServerPlayerEntity player) {
        getPlayers().add(player.getUuid());
        player.alphabetSpeedrun$setItemRecordAccess(this);
        difficulty().onStart(player);
        // TODO: multiplayer onStart: write in difficulty.onStart
    }

    ///////////

    public Collection<UUID> getPlayers() {
        return players;
    }

    @Override
    public UUID recordId() {
        return wrapped.recordId();
    }

    @Override
    public Identifier goalId() {
        return wrapped.goalId();
    }

    public Collection<UUID> getOperators() {
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

    public JsonObject toJsonMeta() {
        final JsonObject obj = new JsonObject();
        obj.addProperty("is_coop", true);
        obj.addProperty("coop_uuid", this.recordId().toString());
        return obj;
    }

    public static @Nullable CoopRecordAccess tryParseMeta(CoopRecordManager manager, JsonObject obj) {
        if (!JsonHelper.getBoolean(obj, "is_coop", false))
            return null;
        UUID uuid = UUID.fromString(JsonHelper.getString(obj, "coop_uuid"));
        return manager.get(uuid);
    }

    @Override
    public JsonObject toJson() {
        final JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();
        for (UUID operator : operators)
            arr.add(operator.toString());
        obj.add("operators", arr);
        arr = new JsonArray();
        for (UUID player : players)
            arr.add(player.toString());
        obj.add("player", arr);
        obj.add("record", wrapped.toJson());
        return obj;
    }

    @Override
    public long timeSince(long current) {
        return wrapped.timeSince(current);
    }

    public static CoopRecord fromJson(@NotNull JsonObject obj) {
        Objects.requireNonNull(obj);
        final Set<UUID> operators = new HashSet<>();
        JsonHelper.getArray(obj, "operators").forEach(e ->
                operators.add(UUID.fromString(JsonHelper.asString(e, "uuid"))));
        final Set<UUID> players = new HashSet<>();
        JsonHelper.getArray(obj, "players").forEach(e ->
                players.add(UUID.fromString(JsonHelper.asString(e, "uuid"))));
        ItemSpeedrunRecord record = ItemSpeedrunRecord.fromJson(obj.get("record"), false);
        return new CoopRecord(record, operators, players);
    }

    @Override
    public void onStop(Collection<? extends ServerPlayerEntity> players) {
        CoopRecordAccess.super.onStop(players);
    }

    @Override
    public void sudoJoin(UUID hostId, Collection<? extends ServerPlayerEntity> players) {
        // what's it for
    }
}
