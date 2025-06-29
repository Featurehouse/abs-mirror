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
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.featurehouse.mcmod.speedrun.alphabeta.item.command.Draft;
import org.featurehouse.mcmod.speedrun.alphabeta.item.command.ItemSpeedrunCommandHandle;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.DefaultItemSpeedrunDifficulty;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.ItemSpeedrunDifficulty;
import org.featurehouse.mcmod.speedrun.alphabeta.util.MixinSensitive;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.item.ItemStack;

@MixinSensitive
public final class ItemSpeedrunRecord implements ItemRecordAccess {
    private final ResourceLocation goalId;
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
            ResourceLocation goalId,
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

    public static final MapCodec<ItemSpeedrunRecord> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    ResourceLocation.CODEC.fieldOf("goal_id").forGetter(ItemSpeedrunRecord::goalId),
                    UUIDUtil.STRING_CODEC.fieldOf("record_id").forGetter(ItemSpeedrunRecord::recordId),
                    SingleSpeedrunPredicate.CODEC.listOf().fieldOf("predicates").forGetter(ItemSpeedrunRecord::predicates),
                    Codec.LONG_STREAM.xmap(LongStream::toArray, Arrays::stream).fieldOf("collected").forGetter(ItemSpeedrunRecord::collected),
                    Codec.LONG.fieldOf("start_time").forGetter(ItemSpeedrunRecord::startTime),
                    Codec.LONG.fieldOf("finish_time").orElse(-1L).forGetter(ItemSpeedrunRecord::finishTime),
                    Codec.LONG.fieldOf("last_quit_time").orElse(-1L).forGetter(ItemSpeedrunRecord::lastQuitTime),
                    Codec.LONG.fieldOf("vacant_time").orElse(0L).forGetter(ItemSpeedrunRecord::vacantTime),
                    ResourceLocation.CODEC.xmap(DefaultItemSpeedrunDifficulty::getDifficulty, ItemSpeedrunDifficulty::getId).fieldOf("difficulty").orElseGet(() -> DefaultItemSpeedrunDifficulty.NN).forGetter(ItemSpeedrunRecord::difficulty),
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, UUIDUtil.STRING_CODEC).fieldOf("pvp_mates").orElseGet(Maps::newLinkedHashMap).forGetter(ItemSpeedrunRecord::mates)
            ).apply(instance, ItemSpeedrunRecord::new)
    );

    public static final Codec<ItemSpeedrunRecord> CODEC = MAP_CODEC.codec();

    ItemSpeedrunRecord(
            ResourceLocation goalId,
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
        return predicates.stream().map(SingleSpeedrunPredicate::icon).toList();
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

    public ItemSpeedrunRecord resetUuid() {
        // Shallow copy
        UUID uuid = UUID.randomUUID();
        return new ItemSpeedrunRecord(
                goalId,
                uuid,
                predicates,
                collected,
                startTime,
                finishTime,
                lastQuitTime,
                vacantTime,
                difficulty,
                mates
        );
    }

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

    public ResourceLocation goalId() {
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
    public void onStart(ServerPlayer player) {
        player.alphabetSpeedrun$setItemRecordAccess(this);
        difficulty().onStart(player);
    }

    @Override
    public Collection<ServerPlayer> getMates(PlayerList manager, ServerPlayer self) {
        List<ServerPlayer> l = mates().keySet().stream()
                .map(manager::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        //l.add(self);
        if (self != null) l.add(self);
        return l;
    }

    @Override
    public void sudoJoin(UUID hostId, Collection<? extends ServerPlayer> joint) {
        final Draft draft = asDraft();
        for (ServerPlayer player : joint) {
            MutableBoolean failed = new MutableBoolean();
            ItemSpeedrunCommandHandle.startFromDraft(t -> {
                player.sendSystemMessage(t.copy().withStyle(ChatFormatting.RED));
                failed.setTrue();
            }, player, draft);
            if (failed.isFalse()) {
                final ItemRecordAccess acc = player.alphabetSpeedrun$getItemRecordAccess();
                if (acc == null || acc.isCoop()) continue;
                final ItemSpeedrunRecord rec = (ItemSpeedrunRecord) acc;
                final UUID that = rec.recordId();
                // Trust each other
                this.mates.put(player.getUUID(), that);
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
