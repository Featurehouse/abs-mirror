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

package org.featurehouse.mcmod.speedrun.alphabeta.item.command;

import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.featurehouse.mcmod.speedrun.alphabeta.item.RecordSnapshot;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.DefaultItemSpeedrunDifficulty;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.ItemSpeedrunDifficulty;

import javax.annotation.CheckForNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Draft {
    private final UUID sessionId = UUID.randomUUID();
    private PlayType playType = PlayType.PVP;
    private Identifier goal;
    private final List<UUID> players = new ArrayList<>();
    private ItemSpeedrunDifficulty difficulty = DefaultItemSpeedrunDifficulty.UU;
    private final List<UUID> operators = new ArrayList<>();

    public PlayType getPlayType() {
        return playType;
    }

    public void setPlayType(PlayType playType) {
        Objects.requireNonNull(playType, "playType");
        this.playType = playType;
    }

    @CheckForNull
    public Identifier getGoal() {
        return goal;
    }

    public void setGoal(Identifier goal) {
        Objects.requireNonNull(goal, "goal");
        this.goal = goal;
    }

    public List<UUID> getPlayers() {
        return players;
    }

    public ItemSpeedrunDifficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(ItemSpeedrunDifficulty difficulty) {
        Objects.requireNonNull(difficulty, "difficulty");
        this.difficulty = difficulty;
    }

    public List<UUID> getOperators() {
        return operators;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public boolean sameSession(UUID other) {
        return sessionId.equals(other);
    }

    public Draft() {}

    public static Draft createPVP(Identifier goal, ItemSpeedrunDifficulty difficulty) {
        Draft draft = new Draft();
        //draft.setPlayType(PlayType.PVP);
        draft.setGoal(goal);
        draft.setDifficulty(difficulty);
        return draft;
    }

    public RecordSnapshot snapshot() {
        return new RecordSnapshot(-1L, 0, -1, getGoal(), getDifficulty(), Util.NIL_UUID, playType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Draft draft = (Draft) o;
        return playType == draft.playType && Objects.equals(goal, draft.goal) && Objects.equals(players, draft.players) && Objects.equals(difficulty, draft.difficulty);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playType, goal, players, difficulty);
    }
}
