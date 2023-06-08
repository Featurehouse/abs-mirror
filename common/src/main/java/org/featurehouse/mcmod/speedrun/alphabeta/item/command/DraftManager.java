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

import com.google.common.collect.Sets;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.featurehouse.mcmod.speedrun.alphabeta.config.AlphabetSpeedrunConfigData;
import org.featurehouse.mcmod.speedrun.alphabeta.item.ItemRecordAccess;
import org.featurehouse.mcmod.speedrun.alphabeta.item.RecordSnapshot;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class DraftManager {
    private static final ThreadLocal<DraftManager> INSTANCES = ThreadLocal.withInitial(DraftManager::new);
    public static DraftManager get() { return INSTANCES.get(); }

    private final Map<UUID, Draft> drafts = new HashMap<>();
    //private final Map<UUID, Invitation> invitations = new LinkedHashMap<>();
    private final Map<Draft, Set<InvitationCache>> invitations = new LinkedHashMap<>();

    protected DraftManager() { }

    public void tick() {
        for (Set<InvitationCache> v : invitations.values()) {
            v.removeIf(InvitationCache::tick);
        }
    }

    public @Nullable Draft get(ServerPlayerEntity player) {
        return drafts.get(player.getUuid());
    }

    private static final boolean isDraftSupported = Util.make(() -> {
        try {
            Class.forName(org.objectweb.asm.Type.getObjectType("org/featurehouse/mcmod/speedrun/alphabeta/item/command/Draft").getClassName());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    });

    public Optional<Text> createDraft(ServerPlayerEntity serverPlayer) {
        if (!isDraftSupported) {
            return Optional.of(Text.translatable("demo.speedrun.alphabet.draft"));
        }
        ItemRecordAccess acc;
        if ((acc = serverPlayer.alphabetSpeedrun$getItemRecordAccess()) != null)
            return Optional.of(Text.translatable("command.speedrun.alphabet.draft.running",
                    RecordSnapshot.fromRecord(acc, serverPlayer.server.getOverworld().getTime()).asText()));
        UUID uuid;
        if (drafts.containsKey(uuid = serverPlayer.getUuid()))
            return Optional.of(Text.translatable("command.speedrun.alphabet.draft.dup"));
        drafts.put(uuid, new Draft());
        return Optional.empty();
    }

    public Optional<Text> invite(ServerPlayerEntity host, Collection<? extends ServerPlayerEntity> players) {
        final UUID uuid = host.getUuid();
        Draft draft;
        if ((draft = drafts.get(uuid)) == null) return Optional.empty();

        invitations.computeIfAbsent(draft, u0 -> Sets.newHashSet())
                .addAll(players.stream()
                .map(ServerPlayerEntity::getUuid)
                .map(InvitationCache::new)
                .collect(Collectors.toSet()));
        final Text info = draft.snapshot().asText();
        Invitation invitation = new Invitation(uuid, draft.getSessionId(), info, Invitation.DRAFT);
        final Text text = invitation.toText(host.server.getPlayerManager());
        if (text == null) return Optional.empty();

        for (ServerPlayerEntity player : players) {
            player.sendMessage(text.copy());
        }
        return Optional.of(info);
    }

    public void respond(ServerPlayerEntity host, ServerPlayerEntity invited, UUID invitationCache, final boolean accept) {
        final Draft draft = drafts.get(host.getUuid());
        if (draft == null || !draft.sameSession(invitationCache)) {
            org.featurehouse.mcmod.speedrun.alphabeta.util.AlphaBetaDebug.log(1,l->l.info("HS={} SS={}",draft!=null?draft.getSessionId(): Util.NIL_UUID,invitationCache));
            invited.sendMessage(Text.translatable("command.speedrun.alphabet.invite.absent"));
            return;
        }
        final Set<InvitationCache> invitationCaches = invitations.get(draft);
        if (invitationCaches == null || invitationCaches.isEmpty()) {
            invited.sendMessage(Text.translatable("command.speedrun.alphabet.invite.timeout"));
            return;
        }
        invitationCaches.stream().filter(c -> invited.getUuid().equals(c.invitedPlayer())).findAny()
                .ifPresentOrElse(c -> {
                    if (accept) {
                        draft.getPlayers().add(invited.getUuid());
                        host.sendMessage(Text.translatable("command.speedrun.alphabet.invite.accepted", invited.getDisplayName()));
                        // TODO: welcome
                    } else {
                        host.sendMessage(Text.translatable("command.speedrun.alphabet.invite.denied", invited.getDisplayName()));
                    }
                    invitationCaches.remove(c);
                }, () -> invited.sendMessage(Text.translatable("command.speedrun.alphabet.invite.timeout")));
    }

    public int submit(ServerCommandSource source, ServerPlayerEntity serverPlayer) {
        UUID uuid = serverPlayer.getUuid();
        Draft draft;
        if ((draft = drafts.get(uuid)) == null) {
            source.sendError(Text.translatable("command.speedrun.alphabet.draft.not_found"));
            return 0;
        }

        final AlphabetSpeedrunConfigData.Permissions permissions = AlphabetSpeedrunConfigData.getInstance().getPermissions();
        if (!source.hasPermissionLevel(AlphabetSpeedrunConfigData.getInstance().getDifficultDifficulties().contains(draft.getDifficulty())
                ? permissions.getDifficultStart() : permissions.getNormalStart())) {
            source.sendError(Text.translatable("command.speedrun.alphabet.no_permission"));
            return 0;
        }

        int i;
        if ((i = invitations.getOrDefault(draft, Collections.emptySet()).size()) != 0) {
            source.sendError(Text.translatable("command.speedrun.alphabet.invite.respond.wait", i));
            return 0;
        }

        if ((i = ItemSpeedrunCommandHandle.startFromDraft(source::sendError, serverPlayer, draft)) == 0)
            return 0;
        invitations.remove(draft);
        return i;
    }
}
