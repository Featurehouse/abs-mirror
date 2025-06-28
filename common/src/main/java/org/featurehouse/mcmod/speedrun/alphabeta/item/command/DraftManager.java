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
import org.featurehouse.mcmod.speedrun.alphabeta.config.AlphabetSpeedrunConfigData;
import org.featurehouse.mcmod.speedrun.alphabeta.item.ItemRecordAccess;
import org.featurehouse.mcmod.speedrun.alphabeta.item.RecordSnapshot;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

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

    public @Nullable Draft get(ServerPlayer player) {
        return drafts.get(player.getUUID());
    }

    private static final boolean isDraftSupported = Util.make(() -> {
        try {
            Class.forName(org.objectweb.asm.Type.getObjectType("org/featurehouse/mcmod/speedrun/alphabeta/item/command/Draft").getClassName());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    });

    public Optional<Component> createDraft(ServerPlayer serverPlayer) {
        if (!isDraftSupported) {
            return Optional.of(Component.translatable("demo.speedrun.alphabet.draft"));
        }
        ItemRecordAccess acc;
        if ((acc = serverPlayer.alphabetSpeedrun$getItemRecordAccess()) != null)
            return Optional.of(Component.translatable("command.speedrun.alphabet.draft.running",
                    RecordSnapshot.fromRecord(acc, Objects.requireNonNull(serverPlayer.getServer()).overworld().getGameTime()).asText()));
        UUID uuid;
        if (drafts.containsKey(uuid = serverPlayer.getUUID()))
            return Optional.of(Component.translatable("command.speedrun.alphabet.draft.dup"));
        drafts.put(uuid, new Draft());
        return Optional.empty();
    }

    public Optional<Component> invite(ServerPlayer host, Collection<? extends ServerPlayer> players) {
        final UUID uuid = host.getUUID();
        Draft draft;
        if ((draft = drafts.get(uuid)) == null) return Optional.empty();

        invitations.computeIfAbsent(draft, u0 -> Sets.newHashSet())
                .addAll(players.stream()
                .map(ServerPlayer::getUUID)
                .map(InvitationCache::new)
                .collect(Collectors.toSet()));
        final Component info = draft.snapshot().asText();
        Invitation invitation = new Invitation(uuid, draft.getSessionId(), info, Invitation.DRAFT);
        final Component text = invitation.toText(Objects.requireNonNull(host.getServer()).getPlayerList());
        if (text == null) return Optional.empty();

        for (ServerPlayer player : players) {
            player.sendSystemMessage(text.copy());
        }
        return Optional.of(info);
    }

    public void respond(ServerPlayer host, ServerPlayer invited, UUID invitationCache, final boolean accept) {
        final Draft draft = drafts.get(host.getUUID());
        if (draft == null || !draft.sameSession(invitationCache)) {
            org.featurehouse.mcmod.speedrun.alphabeta.util.AlphaBetaDebug.log(1,l->l.info("HS={} SS={}",draft!=null?draft.getSessionId(): Util.NIL_UUID,invitationCache));
            invited.sendSystemMessage(Component.translatable("command.speedrun.alphabet.invite.absent"));
            return;
        }
        final Set<InvitationCache> invitationCaches = invitations.get(draft);
        if (invitationCaches == null || invitationCaches.isEmpty()) {
            invited.sendSystemMessage(Component.translatable("command.speedrun.alphabet.invite.timeout"));
            return;
        }
        invitationCaches.stream().filter(c -> invited.getUUID().equals(c.invitedPlayer())).findAny()
                .ifPresentOrElse(c -> {
                    if (accept) {
                        draft.getPlayers().add(invited.getUUID());
                        host.sendSystemMessage(Component.translatable("command.speedrun.alphabet.invite.accepted", invited.getDisplayName()));
                        // TODO: welcome
                    } else {
                        host.sendSystemMessage(Component.translatable("command.speedrun.alphabet.invite.denied", invited.getDisplayName()));
                    }
                    invitationCaches.remove(c);
                }, () -> invited.sendSystemMessage(Component.translatable("command.speedrun.alphabet.invite.timeout")));
    }

    public int submit(CommandSourceStack source, ServerPlayer serverPlayer) {
        UUID uuid = serverPlayer.getUUID();
        Draft draft;
        if ((draft = drafts.get(uuid)) == null) {
            source.sendFailure(Component.translatable("command.speedrun.alphabet.draft.not_found"));
            return 0;
        }

        final AlphabetSpeedrunConfigData.Permissions permissions = AlphabetSpeedrunConfigData.getInstance().getPermissions();
        if (!source.hasPermission(AlphabetSpeedrunConfigData.getInstance().getDifficultDifficulties().contains(draft.getDifficulty())
                ? permissions.getDifficultStart() : permissions.getNormalStart())) {
            source.sendFailure(Component.translatable("command.speedrun.alphabet.no_permission"));
            return 0;
        }

        int i;
        if ((i = invitations.getOrDefault(draft, Collections.emptySet()).size()) != 0) {
            source.sendFailure(Component.translatable("command.speedrun.alphabet.invite.respond.wait", i));
            return 0;
        }

        if ((i = ItemSpeedrunCommandHandle.startFromDraft(source::sendFailure, serverPlayer, draft)) == 0)
            return 0;
        invitations.remove(draft);
        return i;
    }
}
