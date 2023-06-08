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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.featurehouse.mcmod.speedrun.alphabeta.item.command.DraftManager;
import org.featurehouse.mcmod.speedrun.alphabeta.item.command.Invitation;
import org.featurehouse.mcmod.speedrun.alphabeta.item.command.InvitationCache;
import org.featurehouse.mcmod.speedrun.alphabeta.item.coop.CoopRecordAccess;
import org.featurehouse.mcmod.speedrun.alphabeta.item.coop.CoopRecordManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MultiplayerRecords {
    private static final Map<UUID/*recordId*/, Set<InvitationCache>> INVITATIONS = Maps.newHashMap();

    static void tickInvitations() {
        for (Set<InvitationCache> set : INVITATIONS.values()) {
            set.removeIf(InvitationCache::tick);
        }
    }

    public static int invite(@NotNull ServerPlayerEntity self, @NotNull Collection<? extends ServerPlayerEntity> players,
                             Consumer<? super Text> errorParser) {
        if (players.isEmpty()) {
            errorParser.accept(Text.translatable("command.speedrun.alphabet.players_empty"));
            return 0;
        }

        if (players.contains(self)) {
            errorParser.accept(Text.translatable("command.speedrun.alphabet.invite.self.invalid"));
            return 0;
        }

        final Optional<Text> text = DraftManager.get().invite(self, players);
        if (text.isPresent()) {
            self.sendMessage(Text.translatable("command.speedrun.alphabet.invite.sent", text.get()));
            return 1;
        }

        final ItemRecordAccess rec = self.alphabetSpeedrun$getItemRecordAccess();
        if (rec == null) {
            errorParser.accept(Text.translatable("command.speedrun.alphabet.invite.record_absent"));
            return 0;
        }

        if (rec.isCoop() && !rec.asCoop().isOp(self)) {
            errorParser.accept(Text.translatable("command.speedrun.alphabet.no_permission"));
            return 0;
        }

        final UUID uuid = rec.recordId();
        final Invitation invitation = new Invitation(self.getUuid(), uuid,
                RecordSnapshot.fromRecord(rec, self.server.getOverworld().getTime()).asText(),
                rec.isCoop() ? Invitation.COOP : Invitation.PVP);
        INVITATIONS.computeIfAbsent(uuid, u0 -> Sets.newHashSet())
                .addAll(players.stream()
                        .map(ServerPlayerEntity::getUuid)
                        .map(InvitationCache::new)
                        .collect(Collectors.toSet()));
        final Text text0 = invitation.toText(self.server.getPlayerManager());
        if (text0 == null) return 0;    // Never happens

        for (ServerPlayerEntity player : players) {
            player.sendMessage(text0.copy());
        }

        return 1;
    }

    public static void respond(int type, ServerPlayerEntity host, ServerPlayerEntity invited, UUID invitationCache, final boolean accept) {
        switch (type) {
            case Invitation.DRAFT -> {
                DraftManager.get().respond(host, invited, invitationCache, accept);
                return;
            }
            case Invitation.PVP -> {
                final ItemRecordAccess acc = host.alphabetSpeedrun$getItemRecordAccess();
                if (acc != null && !acc.isCoop() && acc.recordId().equals(invitationCache)) {
                    acc.sudoJoin(host.getUuid(), Collections.singleton(invited));
                    // TODO: welcome
                } else {
                    invited.sendMessage(Text.translatable("command.speedrun.alphabet.invite.timeout"));
                }
                return;
            }
        }

        CoopRecordAccess coopRecord = CoopRecordManager.fromServer(host.server).get(invitationCache);
        if (coopRecord == null) {
            invited.sendMessage(Text.translatable("command.speedrun.alphabet.invite.absent"));
            return;
        }
        final Set<InvitationCache> invitationCaches = INVITATIONS.get(invitationCache);
        if (invitationCaches == null || invitationCaches.isEmpty()) {
            invited.sendMessage(Text.translatable("command.speedrun.alphabet.invite.timeout"));
            return;
        }
        invitationCaches.stream().filter(c -> invited.getUuid().equals(c.invitedPlayer())).findAny()
                .ifPresentOrElse(c -> {
                    if (accept) {
                        //draft.getPlayers().add(invited.getUuid());
                        RecordSnapshot record1 = RecordSnapshot.fromRecord(coopRecord, host.server.getOverworld().getTime());
                        invited.sendMessage(Text.translatable("command.speedrun.alphabet.start",
                                record1.asText()));
                        ItemSpeedrunEvents.START_RUNNING_EVENT.invoker().onStartRunning(invited, coopRecord, ItemSpeedrunEvents.StartRunning.JOIN_COOP);
                        coopRecord.onStart(invited);
                        // TODO: welcome invited player
                        host.sendMessage(Text.translatable("command.speedrun.alphabet.invite.accepted", invited.getDisplayName()));
                    } else {
                        host.sendMessage(Text.translatable("command.speedrun.alphabet.invite.denied", invited.getDisplayName()));
                    }
                    invitationCaches.remove(c);
                }, () -> invited.sendMessage(Text.translatable("command.speedrun.alphabet.invite.timeout")));
    }
}
