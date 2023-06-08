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

import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record Invitation(UUID host, UUID sessionIdCache, Text info, int type) {
    public static final int DRAFT = 1, COOP = 2, PVP = 3;
    public static final int ACCEPT = 4;

    @Nullable
    public Text toText(PlayerManager manager) {
        final ServerPlayerEntity player = manager.getPlayer(host());
        if (player == null) return null;
        //final String subcommand = isDraft() ? "respond_draft" : "respond_coop";
        return Text.translatable("command.speedrun.alphabet.invite",
                Texts.bracketed(player.getDisplayName()),
                this.info(),
                Text.translatable("command.speedrun.alphabet.invite.accept")
                        .styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                String.format("/itemspeedrun invite respond %d %s %s",
                                        type() + ACCEPT,
                                        player.getGameProfile().getName(),
                                        sessionIdCache())))),
                Text.translatable("command.speedrun.alphabet.invite.deny")
                        .styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                String.format("/itemspeedrun invite respond %d %s %s",
                                        type(),
                                        player.getGameProfile().getName(),
                                        sessionIdCache()))))
        );
    }
}
