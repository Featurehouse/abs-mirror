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

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import org.featurehouse.mcmod.speedrun.alphabeta.item.coop.CoopRecord;
import org.featurehouse.mcmod.speedrun.alphabeta.item.coop.CoopRecordAccess;
import org.featurehouse.mcmod.speedrun.alphabeta.item.coop.CoopRecordManager;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.ItemSpeedrunDifficulty;
import org.featurehouse.mcmod.speedrun.alphabeta.util.MixinSensitive;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.item.ItemStack;

@MixinSensitive
public interface ItemRecordAccess {
    Pattern SHORT_NAME_PATTERN = Pattern.compile("^\\u0023[0-9A-F]{4}$");

    static Collection<? extends Path> pathFromShortName(Path rootPath, String shortName) throws IOException {
        Preconditions.checkArgument(SHORT_NAME_PATTERN.matcher(shortName).matches(), "short name should be like #1234");
        var shortName0 = shortName.substring(1);

        try (Stream<Path> s = Files.list(rootPath)) {
            return s.filter(p -> {
                String fn = p.getFileName().toString();
                return StoredItemRecords.FILENAME_PATTERN.matcher(fn).matches() &&
                        fn.toUpperCase(Locale.ROOT).startsWith(shortName0);
            }).toList();
        }
    }

    static Codec<ItemRecordAccess> metaCodec(CoopRecordManager coopMgr) {
        return Codec.BOOL.orElse(Boolean.FALSE).dispatch("is_coop", ItemRecordAccess::isCoop, isCoop -> {
            if (isCoop) {
                return CoopRecord.metaCodec(coopMgr);
            } else {
                return ItemSpeedrunRecord.MAP_CODEC;
            }
        });
    }

    List<ItemStack> displayedStacks();
    boolean tryMarkDone(long currentTime);
    boolean isAllRequirementsPassed();
    boolean isFinished();
    boolean isRequirementPassed(int idx);
    void setRequirementPassedTime(int index, long time);
    int getCollectedCount();

    long timeSince(long current);
    ResourceLocation goalId();
    UUID recordId();
    List<SingleSpeedrunPredicate> predicates();
    long[] collected();
    long startTime();
    long finishTime();
    void setFinishTime(long finishTime);
    long lastQuitTime();
    void setLastQuitTime(long lastQuitTime);
    long vacantTime();
    void setVacantTime(long vacantTime);
    ItemSpeedrunDifficulty difficulty();

    // Coop Compatibility
    default boolean isCoop() { return false; }
    default CoopRecordAccess asCoop() throws IllegalStateException {
        throw new IllegalStateException();
    }

    Collection<ServerPlayer> getMates(PlayerList manager, @Nullable ServerPlayer self);
    void onStart(ServerPlayer player);
    default void onStop(Collection<? extends ServerPlayer> players) {}

    //@Deprecated
    void sudoJoin(UUID hostId, Collection<? extends ServerPlayer> players);
    default boolean trusts(@Nullable ServerPlayer player) {
        if (player == null)
            return false;
        if (this.isCoop()) {
            return this.asCoop().isOp(player);
        } else {
            if (!(this instanceof ItemSpeedrunRecord record)) return false;
            final @Nullable UUID expectedRecordId = record.mates().get(player.getUUID());
            if (Util.NIL_UUID.equals(expectedRecordId)) return true;    // always trust the player
            var recOther = player.alphabetSpeedrun$getItemRecordAccess();
            return recOther != null && recOther.recordId().equals(expectedRecordId);
        }
    }

    default void addTrust(UUID other) {
    }
}
