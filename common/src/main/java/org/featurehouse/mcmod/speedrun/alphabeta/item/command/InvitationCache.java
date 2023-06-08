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

import org.featurehouse.mcmod.speedrun.alphabeta.config.AlphabetSpeedrunConfigData;

import java.util.Objects;
import java.util.UUID;

public final class InvitationCache {
    private final UUID invitedPlayer;
    private int timeout = 20 * AlphabetSpeedrunConfigData.getInstance().getDefaultInvitationCooldown();

    public InvitationCache(UUID invitedPlayer) {
        this.invitedPlayer = invitedPlayer;
    }

    public UUID invitedPlayer() {
        return invitedPlayer;
    }

    public boolean tick() {
        return --timeout <= 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        return Objects.equals(this.invitedPlayer, ((InvitationCache) obj).invitedPlayer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(invitedPlayer);
    }

    @Override
    public String toString() {
        return "InvitationCache[" +
                "invitedPlayer=" + invitedPlayer + ", " +
                "timeout=" + timeout + ']';
    }

}
