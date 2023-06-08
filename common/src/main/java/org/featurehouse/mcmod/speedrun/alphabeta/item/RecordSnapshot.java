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
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.featurehouse.mcmod.speedrun.alphabeta.item.command.PlayType;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.DefaultItemSpeedrunDifficulty;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.ItemSpeedrunDifficulty;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public record RecordSnapshot(long duration, int collected, int required, Identifier goalId, ItemSpeedrunDifficulty difficulty, UUID recordId, PlayType playType) {
    public static RecordSnapshot fromRecord(ItemRecordAccess record, long currentTime) {
        long duration = record.timeSince(currentTime);
        return new RecordSnapshot(duration, record.getCollectedCount(), record.predicates().size(),
                record.goalId(), record.difficulty(), record.recordId(), record.isCoop() ? PlayType.COOP : PlayType.PVP);
    }

    static RecordSnapshot fromPvpRecordJson(@NotNull JsonObject obj, long currentTime) throws JsonSyntaxException {
        Identifier goalId = new Identifier(JsonHelper.getString(obj, "goal_id"));
        Identifier difficulty = new Identifier(JsonHelper.getString(obj, "difficulty"));
        UUID recordId = UUID.fromString(JsonHelper.getString(obj, "record_id"));
        int required;
        if (JsonHelper.hasArray(obj, "displayed_stacks")) {
            // Old schema before v3.0.x
            required = JsonHelper.getArray(obj, "displayed_stacks").size();
        } else {
            required = JsonHelper.getArray(obj, "predicates").size();
        }
        int collected = 0; {
            var arr = JsonHelper.getArray(obj, "collected");
            for (JsonElement e : arr) {
                if (e.getAsLong() >= 0) {
                    collected++;
                }
            }
        }
        long duration;
        long startTime = JsonHelper.getLong(obj, "start_time");
        long l = JsonHelper.getLong(obj, "finish_time", -1);
        if (l < 0)
            l = JsonHelper.getLong(obj, "last_quit_time", -1);
        if (l >= 0)
            duration = l - startTime;
        else
            duration = currentTime - startTime - JsonHelper.getLong(obj, "vacant_time", 0);
        return new RecordSnapshot(duration, collected, required, goalId, DefaultItemSpeedrunDifficulty.getDifficulty(difficulty), recordId, PlayType.PVP);
    }

    boolean isFinished() {
        return collected() == required();
    }

    public Text asText() {
        return Texts.bracketed(Text.empty() // To avoid things after '#' are bolded
                        .append(Optional.ofNullable(ItemSpeedrun.get(this.goalId()))
                                .map(spr -> spr.display().copy())
                                .orElseGet(() -> Text.translatable("message.speedrun_alphabet.item.goal.unknown"))
                        ).append(Text.literal("#" + ItemRecordMessages.uuidShort(this.recordId())).formatted(Formatting.GRAY)))
                .styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, recordInnerText())));
    }

    private Text recordInnerText() {
        MutableText t = Text.empty();
        t.append(Text.translatable("message.speedrun_alphabet.item.record.goal_id", this.goalId()))
                .append("\n");
        t.append(Text.translatable("message.speedrun_alphabet.item.record.snapshot.play_type", playType().getText()))
                .append("\n");
        t.append(Text.translatable("message.speedrun_alphabet.item.record.difficulty", difficulty().asText()))
                .append("\n");
        if (this.required() >= 0) { // otherwise stub
            if (this.isFinished()) {
                t.append(Text.translatable("message.speedrun_alphabet.item.record.progress",
                        Text.translatable("message.speedrun_alphabet.item.record.progress.data",
                                this.collected(), this.required()).formatted(Formatting.GREEN))).append("\n");
                t.append(Text.translatable("message.speedrun_alphabet.item.record.finish_time", ItemRecordMessages.time(this.duration()))).append("\n");
            } else {
                t.append(Text.translatable("message.speedrun_alphabet.item.record.progress",
                        Text.translatable("message.speedrun_alphabet.item.record.progress.data",
                                this.collected(), this.required()).formatted(Formatting.RED))).append("\n");
            }
            t.append(Text.translatable("message.speedrun_alphabet.item.record.id", this.recordId())).append("\n");
        }
        t.append(Text.translatable("message.speedrun_alphabet.item.non-synced").formatted(Formatting.GRAY));
        return t;
    }
}
