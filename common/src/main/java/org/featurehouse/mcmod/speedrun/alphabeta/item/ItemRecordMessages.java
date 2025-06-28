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

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

@ApiStatus.Internal
public final class ItemRecordMessages {

    public static Component itemCollected(Player player, ItemStack displayedStack,
                                     ItemRecordAccess record, long currentTime,
                                     @Nullable ItemStack actualStack) {
        final Component entityName = player.getDisplayName();
        final Component itemName = displayedStack.getDisplayName();
        final int size0 = record.getCollectedCount();
        final int size1 = record.predicates().size();
        RecordSnapshot record1 = RecordSnapshot.fromRecord(record, currentTime);
        final Component time = time(record1.duration());
        if (actualStack != null) {
            final CompoundTag nbt = Optional.ofNullable(actualStack.get(DataComponents.CUSTOM_DATA)).map(CustomData::copyTag).orElse(new CompoundTag());
            switch (nbt.getByte("AlphabetSpeedrunDisplaysReal").orElse((byte) 0)) {
                case 1 -> {
                    final Component actualName = actualStack.getDisplayName();
                    return Component.translatable("message.speedrun.alphabet.item.collected.with_actual",
                            entityName, itemName, size0, size1, time, record1.asText(), actualName);
                }
                case 2 -> {
                    final Component actualName = actualStack.getDisplayName();
                    return Component.translatable("message.speedrun.alphabet.item.collected.actual_only",
                            entityName, actualName, size0, size1, time, record1.asText());
                }
            }
        }
        return Component.translatable("message.speedrun.alphabet.item.collected",
                entityName, itemName, size0, size1, time, record1.asText());
    }

    public static Component itemCompleted(Player player, ItemRecordAccess record, long currentTime) {
        final Component entityName = player.getDisplayName();
        final int size = record.predicates().size();
        //final Text time = time(record.timeSince(currentTime));
        RecordSnapshot record1 = RecordSnapshot.fromRecord(record, currentTime);
        final Component time = time(record1.duration());
        return Component.translatable("message.speedrun.alphabet.item.completed",
                entityName, size, time, record1.asText());
    }

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();
    static String uuidShort(UUID uuid) {
        int msb = (int)(uuid.getMostSignificantBits() >>> 48);
        char[] c = new char[] {
            HEX[msb >> 12],
            HEX[(msb >> 8) & 15],
            HEX[(msb >> 4) & 15],
            HEX[(msb) & 15]
        };
        return String.valueOf(c);
    }

    public static Component time(final long ticks) {
        if (ticks < 0) return Component.translatable("speedrun.alphabet.time_format.unknown", ticks);
        long seconds = ticks / 20;
        long minutes = seconds / 60;
        if (minutes == 0)
            return Component.translatable("speedrun.alphabet.time_format.s", seconds);
        seconds %= 60;
        long hours = minutes / 60;
        if (hours == 0)
            return Component.translatable("speedrun.alphabet.time_format.ms", minutes, seconds);
        minutes %= 60;
        return Component.translatable("speedrun.alphabet.time_format.hms", hours, minutes, seconds);
    }

    public static void sendSound(PlayerList mgr, SoundEvent sound) {
        mgr.getPlayers().forEach(p -> p.playSound(sound, .8F, 1.0F));
    }

    public static void sendWinSound(Player winner, PlayerList mgr) {
        mgr.getPlayers().forEach(p -> {
            if (p != winner) {
                p.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, .8F, 1.0F);
            } else {
                p.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, .8F, 1.0F);
            }
        });
    }
}
