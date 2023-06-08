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

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.PlayerManager;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@ApiStatus.Internal
// TODO #3: enhanced item record messages
public final class ItemRecordMessages {

    public static Text itemCollected(PlayerEntity player, ItemStack displayedStack,
                                     ItemRecordAccess record, long currentTime,
                                     @Nullable ItemStack actualStack) {
        final Text entityName = player.getDisplayName();
        final Text itemName = displayedStack.toHoverableText();
        final int size0 = record.getCollectedCount();
        final int size1 = record.predicates().size();
        RecordSnapshot record1 = RecordSnapshot.fromRecord(record, currentTime);
        final Text time = time(record1.duration());
        if (actualStack != null) {
            final NbtCompound nbt = actualStack.getNbt();
            if (nbt != null) {
                switch (nbt.getByte("AlphabetSpeedrunDisplaysReal")) {
                    case 1 -> {
                        final Text actualName = actualStack.toHoverableText();
                        return Text.translatable("message.speedrun.alphabet.item.collected.with_actual",
                                entityName, itemName, size0, size1, time, record1.asText(), actualName);
                    }
                    case 2 -> {
                        final Text actualName = actualStack.toHoverableText();
                        return Text.translatable("message.speedrun.alphabet.item.collected.actual_only",
                                entityName, actualName, size0, size1, time, record1.asText());
                    }
                }
            }
        }
        return Text.translatable("message.speedrun.alphabet.item.collected",
                entityName, itemName, size0, size1, time, record1.asText());
    }

    public static Text itemCompleted(PlayerEntity player, ItemRecordAccess record, long currentTime) {
        final Text entityName = player.getDisplayName();
        final int size = record.predicates().size();
        //final Text time = time(record.timeSince(currentTime));
        RecordSnapshot record1 = RecordSnapshot.fromRecord(record, currentTime);
        final Text time = time(record1.duration());
        return Text.translatable("message.speedrun.alphabet.item.completed",
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

    public static Text time(final long ticks) {
        if (ticks < 0) return Text.translatable("speedrun.alphabet.time_format.unknown", ticks);
        long seconds = ticks / 20;
        long minutes = seconds / 60;
        if (minutes == 0)
            return Text.translatable("speedrun.alphabet.time_format.s", seconds);
        seconds %= 60;
        long hours = minutes / 60;
        if (hours == 0)
            return Text.translatable("speedrun.alphabet.time_format.ms", minutes, seconds);
        minutes %= 60;
        return Text.translatable("speedrun.alphabet.time_format.hms", hours, minutes, seconds);
    }

    public static void sendSound(PlayerManager mgr, SoundEvent sound) {
        mgr.getPlayerList().forEach(p -> p.playSound(sound, SoundCategory.AMBIENT, .8F, 1.0F));
    }

    public static void sendWinSound(PlayerEntity winner, PlayerManager mgr) {
        mgr.getPlayerList().forEach(p -> {
            if (p != winner) {
                p.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.AMBIENT, .8F, 1.0F);
            } else {
                p.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.AMBIENT, .8F, 1.0F);
            }
        });
    }
}
