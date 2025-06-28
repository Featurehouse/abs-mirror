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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.featurehouse.mcmod.speedrun.alphabeta.item.command.PlayType;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.DefaultItemSpeedrunDifficulty;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.ItemSpeedrunDifficulty;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public record RecordSnapshot(long duration, int collected, int required, ResourceLocation goalId, ItemSpeedrunDifficulty difficulty, UUID recordId, PlayType playType) {
    public static RecordSnapshot fromRecord(ItemRecordAccess record, long currentTime) {
        long duration = record.timeSince(currentTime);
        return new RecordSnapshot(duration, record.getCollectedCount(), record.predicates().size(),
                record.goalId(), record.difficulty(), record.recordId(), record.isCoop() ? PlayType.COOP : PlayType.PVP);
    }

    static RecordSnapshot fromPvpRecordJson(@NotNull JsonObject obj, long currentTime) throws JsonSyntaxException {
        ResourceLocation goalId = ResourceLocation.parse(GsonHelper.getAsString(obj, "goal_id"));
        ResourceLocation difficulty = ResourceLocation.parse(GsonHelper.getAsString(obj, "difficulty"));
        UUID recordId = UUID.fromString(GsonHelper.getAsString(obj, "record_id"));
        int required;
        if (GsonHelper.isArrayNode(obj, "displayed_stacks")) {
            // Old schema before v3.0.x
            required = GsonHelper.getAsJsonArray(obj, "displayed_stacks").size();
        } else {
            required = GsonHelper.getAsJsonArray(obj, "predicates").size();
        }
        int collected = 0; {
            var arr = GsonHelper.getAsJsonArray(obj, "collected");
            for (JsonElement e : arr) {
                if (e.getAsLong() >= 0) {
                    collected++;
                }
            }
        }
        long duration;
        long startTime = GsonHelper.getAsLong(obj, "start_time");
        long l = GsonHelper.getAsLong(obj, "finish_time", -1);
        if (l < 0)
            l = GsonHelper.getAsLong(obj, "last_quit_time", -1);
        if (l >= 0)
            duration = l - startTime;
        else
            duration = currentTime - startTime - GsonHelper.getAsLong(obj, "vacant_time", 0);
        return new RecordSnapshot(duration, collected, required, goalId, DefaultItemSpeedrunDifficulty.getDifficulty(difficulty), recordId, PlayType.PVP);
    }

    boolean isFinished() {
        return collected() == required();
    }

    public Component asText() {
        return ComponentUtils.wrapInSquareBrackets(Component.empty() // To avoid things after '#' are bolded
                        .append(Optional.ofNullable(ItemSpeedrun.get(this.goalId()))
                                .map(spr -> spr.display().copy())
                                .orElseGet(() -> Component.translatable("message.speedrun_alphabet.item.goal.unknown"))
                        ).append(Component.literal("#" + ItemRecordMessages.uuidShort(this.recordId())).withStyle(ChatFormatting.GRAY)))
                .withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(recordInnerText())));
    }

    private Component recordInnerText() {
        MutableComponent t = Component.empty();
        t.append(Component.translatable("message.speedrun_alphabet.item.record.goal_id", this.goalId()))
                .append("\n");
        t.append(Component.translatable("message.speedrun_alphabet.item.record.snapshot.play_type", playType().getText()))
                .append("\n");
        t.append(Component.translatable("message.speedrun_alphabet.item.record.difficulty", difficulty().asText()))
                .append("\n");
        if (this.required() >= 0) { // otherwise stub
            if (this.isFinished()) {
                t.append(Component.translatable("message.speedrun_alphabet.item.record.progress",
                        Component.translatable("message.speedrun_alphabet.item.record.progress.data",
                                this.collected(), this.required()).withStyle(ChatFormatting.GREEN))).append("\n");
                t.append(Component.translatable("message.speedrun_alphabet.item.record.finish_time", ItemRecordMessages.time(this.duration()))).append("\n");
            } else {
                t.append(Component.translatable("message.speedrun_alphabet.item.record.progress",
                        Component.translatable("message.speedrun_alphabet.item.record.progress.data",
                                this.collected(), this.required()).withStyle(ChatFormatting.RED))).append("\n");
            }
            t.append(Component.translatable("message.speedrun_alphabet.item.record.id", this.recordId())).append("\n");
        }
        t.append(Component.translatable("message.speedrun_alphabet.item.non-synced").withStyle(ChatFormatting.GRAY));
        return t;
    }
}
