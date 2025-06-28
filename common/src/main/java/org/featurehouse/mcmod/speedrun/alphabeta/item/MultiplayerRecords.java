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
import com.google.common.collect.Sets;
import org.featurehouse.mcmod.speedrun.alphabeta.item.command.DraftManager;
import org.featurehouse.mcmod.speedrun.alphabeta.item.command.Invitation;
import org.featurehouse.mcmod.speedrun.alphabeta.item.command.InvitationCache;
import org.featurehouse.mcmod.speedrun.alphabeta.item.coop.CoopRecordAccess;
import org.featurehouse.mcmod.speedrun.alphabeta.item.coop.CoopRecordManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class MultiplayerRecords {
    private static final Map<UUID/*recordId*/, Set<InvitationCache>> INVITATIONS = Maps.newHashMap();

    static void tickInvitations() {
        for (Set<InvitationCache> set : INVITATIONS.values()) {
            set.removeIf(InvitationCache::tick);
        }
    }

    public static int invite(@NotNull ServerPlayer self, @NotNull Collection<? extends ServerPlayer> players,
                             Consumer<? super Component> errorParser) {
        if (players.isEmpty()) {
            errorParser.accept(Component.translatable("command.speedrun.alphabet.players_empty"));
            return 0;
        }

        if (players.contains(self)) {
            errorParser.accept(Component.translatable("command.speedrun.alphabet.invite.self.invalid"));
            return 0;
        }

        final Optional<Component> text = DraftManager.get().invite(self, players);
        if (text.isPresent()) {
            self.sendSystemMessage(Component.translatable("command.speedrun.alphabet.invite.sent", text.get()));
            return 1;
        }

        final ItemRecordAccess rec = self.alphabetSpeedrun$getItemRecordAccess();
        if (rec == null) {
            errorParser.accept(Component.translatable("command.speedrun.alphabet.invite.record_absent"));
            return 0;
        }

        if (rec.isCoop() && !rec.asCoop().isOp(self)) {
            errorParser.accept(Component.translatable("command.speedrun.alphabet.no_permission"));
            return 0;
        }

        final UUID uuid = rec.recordId();
        final Invitation invitation = new Invitation(self.getUUID(), uuid,
                RecordSnapshot.fromRecord(rec, Objects.requireNonNull(self.getServer()).overworld().getGameTime()).asText(),
                rec.isCoop() ? Invitation.COOP : Invitation.PVP);
        INVITATIONS.computeIfAbsent(uuid, u0 -> Sets.newHashSet())
                .addAll(players.stream()
                        .map(ServerPlayer::getUUID)
                        .map(InvitationCache::new)
                        .collect(Collectors.toSet()));
        final Component text0 = invitation.toText(Objects.requireNonNull(self.getServer()).getPlayerList());
        if (text0 == null) return 0;    // Never happens

        for (ServerPlayer player : players) {
            player.sendSystemMessage(text0.copy());
        }

        return 1;
    }

    public static void respond(int type, ServerPlayer host, ServerPlayer invited, UUID invitationCache, final boolean accept) {
        switch (type) {
            case Invitation.DRAFT -> {
                DraftManager.get().respond(host, invited, invitationCache, accept);
                return;
            }
            case Invitation.PVP -> {
                final ItemRecordAccess acc = host.alphabetSpeedrun$getItemRecordAccess();
                if (acc != null && !acc.isCoop() && acc.recordId().equals(invitationCache)) {
                    acc.sudoJoin(host.getUUID(), Collections.singleton(invited));
                    // TODO: welcome
                } else {
                    invited.sendSystemMessage(Component.translatable("command.speedrun.alphabet.invite.timeout"));
                }
                return;
            }
        }

        CoopRecordAccess coopRecord = CoopRecordManager.fromServer(Objects.requireNonNull(host.getServer())).get(invitationCache);
        if (coopRecord == null) {
            invited.sendSystemMessage(Component.translatable("command.speedrun.alphabet.invite.absent"));
            return;
        }
        final Set<InvitationCache> invitationCaches = INVITATIONS.get(invitationCache);
        if (invitationCaches == null || invitationCaches.isEmpty()) {
            invited.sendSystemMessage(Component.translatable("command.speedrun.alphabet.invite.timeout"));
            return;
        }
        invitationCaches.stream().filter(c -> invited.getUUID().equals(c.invitedPlayer())).findAny()
                .ifPresentOrElse(c -> {
                    if (accept) {
                        //draft.getPlayers().add(invited.getUuid());
                        RecordSnapshot record1 = RecordSnapshot.fromRecord(coopRecord, Objects.requireNonNull(host.getServer()).overworld().getGameTime());
                        invited.sendSystemMessage(Component.translatable("command.speedrun.alphabet.start",
                                record1.asText()));
                        ItemSpeedrunEvents.START_RUNNING_EVENT.invoker().onStartRunning(invited, coopRecord, ItemSpeedrunEvents.StartRunning.JOIN_COOP);
                        coopRecord.onStart(invited);
                        // TODO: welcome invited player
                        host.sendSystemMessage(Component.translatable("command.speedrun.alphabet.invite.accepted", invited.getDisplayName()));
                    } else {
                        host.sendSystemMessage(Component.translatable("command.speedrun.alphabet.invite.denied", invited.getDisplayName()));
                    }
                    invitationCaches.remove(c);
                }, () -> invited.sendSystemMessage(Component.translatable("command.speedrun.alphabet.invite.timeout")));
    }
}
