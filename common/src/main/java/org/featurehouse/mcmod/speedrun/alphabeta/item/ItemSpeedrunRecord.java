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

package org.featurehouse.mcmod.speedrun.alphabeta.item;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.nbt.visitor.StringNbtWriter;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.Util;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.featurehouse.mcmod.speedrun.alphabeta.item.command.Draft;
import org.featurehouse.mcmod.speedrun.alphabeta.item.command.ItemSpeedrunCommandHandle;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.DefaultItemSpeedrunDifficulty;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.ItemSpeedrunDifficulty;
import org.featurehouse.mcmod.speedrun.alphabeta.util.MixinSensitive;
import org.featurehouse.mcmod.speedrun.alphabeta.util.hooks.MultiverseHooks;

import java.util.*;
import java.util.stream.Collectors;

@MixinSensitive
public final class ItemSpeedrunRecord implements ItemRecordAccess {
    private final Identifier goalId;
    private final UUID recordId;
    private final List<SingleSpeedrunPredicate> predicates;
    private final long[] collected;
    private final long startTime;
    private long finishTime;
    private long lastQuitTime;
    private long vacantTime;
    private final ItemSpeedrunDifficulty difficulty;
    private final Map<UUID, UUID> mates;

    public ItemSpeedrunRecord(
            Identifier goalId,
            UUID recordId,
            List<SingleSpeedrunPredicate> predicates,
            long startTime,
            ItemSpeedrunDifficulty difficulty
    ) {
        this(goalId, recordId, predicates, initLA(predicates.size()),
                startTime, -1, -1, 0, difficulty, Maps.newHashMap());
    }

    private static long[] initLA(int size) {
        long[] l = new long[size];
        Arrays.fill(l, -1);
        return l;
    }

    ItemSpeedrunRecord(
            Identifier goalId,
            UUID recordId,
            List<SingleSpeedrunPredicate> predicates,
            /*Mutable*/long[] collected,
            long startTime,
            long finishTime,
            long lastQuitTime,
            long vacantTime,
            ItemSpeedrunDifficulty difficulty,
            Map<UUID, UUID> mates
    ) {
        this.goalId = goalId;
        this.recordId = recordId;
        this.predicates = predicates;
        this.collected = collected;
        this.startTime = startTime;
        this.finishTime = finishTime;
        this.lastQuitTime = lastQuitTime;
        this.vacantTime = vacantTime;
        this.difficulty = difficulty;
        this.mates = mates;
    }

    public List<ItemStack> displayedStacks() {
        //return displayedStacks.stream().map(ItemStack::copy).toList();
        return predicates.stream().map(SingleSpeedrunPredicate::getIcon).toList();
    }

    public boolean tryMarkDone(long currentOverworldTime) {
        if (isAllRequirementsPassed()) {
            setFinishTime(currentOverworldTime);
            return true;
        }
        return false;
    }

    public boolean isAllRequirementsPassed() {
        return Arrays.stream(collected).allMatch(l -> l >= 0);
    }

    public boolean isFinished() {
        return finishTime >= 0;
    }

    public boolean isRequirementPassed(int index) {
        return collected[index] >= 0;
    }

    public void setRequirementPassedTime(int index, long time) {
        collected[index] = time;
    }

    public int getCollectedCount() {
        if (isFinished()) return predicates.size();
        int c = 0;
        for (long l : collected) {
            if (l > 0) c++;
        }
        return c;
    }

    // Serializations START
    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("goal_id", goalId().toString());
        obj.addProperty("record_id", recordId().toString());
        JsonArray col = new JsonArray();
        Arrays.stream(collected()).forEach(col::add);
        obj.add("collected", col);
        obj.addProperty("start_time", startTime());
        //obj.addProperty("finish_time", finishTime == null ? -1 : finishTime);
        obj.addProperty("finish_time", finishTime);
        JsonArray dps = new JsonArray();
        predicates.forEach(p -> dps.add(p.serialize()));
        //displayedStacks.forEach(i -> dps.add(stackToJson(i)));
        obj.add("predicates", dps);
        obj.addProperty("last_quit_time", lastQuitTime);
        obj.addProperty("vacant_time", vacantTime);
        obj.addProperty("difficulty", difficulty.getId().toString());
        JsonObject mates = new JsonObject();
        this.mates.forEach((playerId, recId) -> mates.addProperty(playerId.toString(), recId.toString()));
        obj.add("pvp_mates_v2", mates);
        return obj;
    }

    public static ItemSpeedrunRecord fromJson(JsonElement element, boolean resetUuid) {
        JsonObject root = JsonHelper.asObject(element, "root");
        Identifier goalId = new Identifier(JsonHelper.getString(root, "goal_id"));
        UUID recordId = resetUuid ? UUID.randomUUID() : UUID.fromString(JsonHelper.getString(root, "record_id"));
        JsonArray arr;

        List<SingleSpeedrunPredicate> itemPredicates;
        if (JsonHelper.hasArray(root, "displayed_stacks")) {
            // schema: v3.0.x
            // assert: only OfItemPredicate is used
            arr = JsonHelper.getArray(root, "requirements");
            final JsonArray dps0 = JsonHelper.getArray(root, "displayed_stacks");
            int minSize = Math.min(arr.size(), dps0.size());
            itemPredicates = new ArrayList<>(minSize);
            for (int i = 0; i < minSize; i++) {
                JsonElement r0 = arr.get(i), d0 = dps0.get(i);
                ItemStack stack = jsonToStack(JsonHelper.asObject(d0, "item_legacy"));
                ItemPredicate predicate = ItemPredicate.fromJson(r0);
                itemPredicates.add(new SingleSpeedrunPredicate.OfItemPredicate(predicate, stack));
            }
        } else {
            arr = JsonHelper.getArray(root, "predicates");
            itemPredicates = new ArrayList<>(arr.size());
            arr.forEach(e -> itemPredicates.add(SingleSpeedrunPredicate.deserialize(JsonHelper.asObject(e, "predicate"))));
        }


        arr = JsonHelper.getArray(root, "collected");
        long[] collected = new long[arr.size()];
        for (int i = 0; i < arr.size(); i++)
            collected[i] = (JsonHelper.asLong(arr.get(i), "collected[" + i + ']'));
        collected = Arrays.copyOf(collected, itemPredicates.size());
        long startTime = JsonHelper.getLong(root, "start_time");
        long finishTime = JsonHelper.getLong(root, "finish_time", -1);
        long lastQuitTime = JsonHelper.getLong(root, "last_quit_time", -1);
        long vacantTime = JsonHelper.getLong(root, "vacant_time", 0);
        JsonObject obj = JsonHelper.getObject(root, "pvp_mates_v2", null);
        final Map<UUID, UUID> mates = Maps.newHashMap();
        if (obj != null) {
            obj.entrySet().forEach(e -> mates.put(UUID.fromString(e.getKey()),
                    UUID.fromString(JsonHelper.asString(e.getValue(), "mate_record_uuid"))));
            //obj.forEach(e -> mates.add(UUID.fromString(JsonHelper.asString(e, "uuid"))));
        }

        ItemSpeedrunDifficulty difficulty1 = DefaultItemSpeedrunDifficulty.getDifficulty(new Identifier(JsonHelper.getString(root, "difficulty", "speedabc:empty")));
        return new ItemSpeedrunRecord(goalId, recordId, itemPredicates, collected,
                startTime, finishTime, lastQuitTime, vacantTime, difficulty1, mates);
    }

    static JsonObject stackToJson(ItemStack stack) {
        JsonObject obj = new JsonObject();
        obj.addProperty("item", MultiverseHooks.itemId(stack.getItem()).toString());
        obj.addProperty("Count", stack.getCount());
        if (stack.hasNbt()) {
            obj.addProperty("nbt_c", new StringNbtWriter().apply(stack.getNbt()));
        }
        return obj;
    }

    static ItemStack jsonToStack(JsonObject obj) {
        final Item item = JsonHelper.getItem(obj, "item");
        final int count = JsonHelper.getInt(obj, "Count");
        ItemStack stack = new ItemStack(item, count);
        if (JsonHelper.hasString(obj, "nbt_c")) {
            final String c = JsonHelper.getString(obj, "nbt_c");
            try {
                stack.setNbt(StringNbtReader.parse(c));
            } catch (CommandSyntaxException ignore0) {
            }
        }
        return stack;
    }
    // Serializations END

    public long timeSince(long current) {
        long l = this.finishTime();
        if (l < 0)
            l = this.lastQuitTime();
        if (l >= 0) // is absent or finished
            return l - startTime();
        return current - startTime() - vacantTime();
    }

    public Map<UUID, UUID> mates() {
        return mates;
    }

    public Identifier goalId() {
        return goalId;
    }

    public UUID recordId() {
        return recordId;
    }

    public List<SingleSpeedrunPredicate> predicates() {
        return predicates;
    }

    public long[] collected() {
        return collected;
    }

    public long startTime() {
        return startTime;
    }

    public long finishTime() {
        return finishTime;
    }

    public void setFinishTime(long finishTime) {
        this.finishTime = finishTime;
    }

    public long lastQuitTime() {
        return lastQuitTime;
    }

    public void setLastQuitTime(long lastQuitTime) {
        this.lastQuitTime = lastQuitTime;
    }

    public long vacantTime() {
        return vacantTime;
    }

    public void setVacantTime(long vacantTime) {
        this.vacantTime = vacantTime;
    }

    public ItemSpeedrunDifficulty difficulty() {
        return difficulty;
    }

    // Coop compatibility


    @Override
    public void onStart(ServerPlayerEntity player) {
        player.alphabetSpeedrun$setItemRecordAccess(this);
        difficulty().onStart(player);
    }

    @Override
    public Collection<ServerPlayerEntity> getMates(PlayerManager manager, ServerPlayerEntity self) {
        List<ServerPlayerEntity> l = mates().keySet().stream()
                .map(manager::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        //l.add(self);
        if (self != null) l.add(self);
        return l;
    }

    @Override
    public void sudoJoin(UUID hostId, Collection<? extends ServerPlayerEntity> joint) {
        final Draft draft = asDraft();
        for (ServerPlayerEntity player : joint) {
            MutableBoolean failed = new MutableBoolean();
            ItemSpeedrunCommandHandle.startFromDraft(t -> {
                player.sendMessage(t.copy().formatted(Formatting.RED));
                failed.setTrue();
            }, player, draft);
            if (failed.isFalse()) {
                final ItemRecordAccess acc = player.alphabetSpeedrun$getItemRecordAccess();
                if (acc == null || acc.isCoop()) continue;
                final ItemSpeedrunRecord rec = (ItemSpeedrunRecord) acc;
                final UUID that = rec.recordId();
                // Trust each other
                this.mates.put(player.getUuid(), that);
                rec.mates.put(hostId, this.recordId());
            }
        }
    }

    public Draft asDraft() {
        return Draft.createPVP(goalId(), difficulty());
    }

    @Override
    public void addTrust(UUID other) {
        this.mates.put(other, Util.NIL_UUID);
    }

    // Object methods //

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemSpeedrunRecord that = (ItemSpeedrunRecord) o;
        return  startTime == that.startTime &&
                finishTime == that.finishTime &&
                lastQuitTime == that.lastQuitTime &&
                vacantTime == that.vacantTime &&
                Objects.equals(goalId, that.goalId) &&
                Objects.equals(recordId, that.recordId) &&
                Objects.equals(predicates, that.predicates) &&
                Arrays.equals(collected, that.collected) &&
                Objects.equals(difficulty, that.difficulty) &&
                Objects.equals(mates, that.mates);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(goalId, recordId, predicates, startTime, finishTime, lastQuitTime, vacantTime, difficulty, mates);
        result = 31 * result + Arrays.hashCode(collected);
        return result;
    }

    @Override
    public String toString() {
        return "ItemSpeedrunRecord{" +
                "goalId=" + goalId +
                ", recordId=" + recordId +
                ", predicates=" + predicates +
                ", collected=" + Arrays.toString(collected) +
                ", startTime=" + startTime +
                ", finishTime=" + finishTime +
                ", lastQuitTime=" + lastQuitTime +
                ", vacantTime=" + vacantTime +
                ", difficulty=" + difficulty +
                ", mates=" + mates +
                '}';
    }
}
