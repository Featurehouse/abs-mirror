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

package org.featurehouse.mcmod.speedrun.alphabeta.config;

import com.google.common.base.Preconditions;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.DefaultItemSpeedrunDifficulty;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.ItemSpeedrunDifficulty;
import org.featurehouse.mcmod.speedrun.alphabeta.util.qj5.JsonReader;
import org.featurehouse.mcmod.speedrun.alphabeta.util.qj5.JsonToken;
import org.featurehouse.mcmod.speedrun.alphabeta.util.qj5.JsonWriter;

import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class AlphabetSpeedrunConfigData {
    final static int CURRENT_SCHEMA = 10;
    private boolean itemsOnlyAvailableWhenRunning = false;
    private boolean stopOnQuit = false;
    private boolean timerPausesWhenVacant = true;
    private int defaultInvitationCooldown = 30;
    private boolean enableLegacyCommands = false;

    // default: !empty
    private ItemRunDifficultyRuleFactory difficultiesWithOp = new ItemRunDifficultyRuleFactory.Impl(
            Collections.singletonList(DefaultItemSpeedrunDifficulty.NN.getId()), true);

    private final Permissions permissions = new Permissions();

    // PERMISSIONS //
    public static final class Permissions {
        private int normalStart = 0,
                difficultStart = 0,
                draft = 0,
                stop = 0,
                resume = 0,
                archive = 0,
                stopOthers = 3,
                resumeOthers = 3,
                archiveOthers = 3,
                view = 0,
                viewOthers = 2,
                viewPvpMates = 0,
                join = 0,
                invite = 0,
                delete = 0,
                deleteOthers = 3,
                list = 0,
                listOthers = 2;

        public int getNormalStart() {
            return normalStart;
        }

        public void setNormalStart(int normalStart) {
            checkPermissionRange(normalStart);
            this.normalStart = normalStart;
        }

        public int getDifficultStart() {
            return difficultStart;
        }

        public void setDifficultStart(int difficultStart) {
            checkPermissionRange(difficultStart);
            this.difficultStart = difficultStart;
        }

        public int getStop() {
            return stop;
        }

        public void setStop(int stop) {
            checkPermissionRange(stop);
            this.stop = stop;
        }

        public int getResume() {
            return resume;
        }

        public void setResume(int resume) {
            checkPermissionRange(resume);
            this.resume = resume;
        }

        public int getArchive() {
            return archive;
        }

        public void setArchive(int archive) {
            checkPermissionRange(archive);
            this.archive = archive;
        }

        public int getStopOthers() {
            return stopOthers;
        }

        public void setStopOthers(int stopOthers) {
            checkPermissionRange(stopOthers);
            this.stopOthers = stopOthers;
        }

        public int getResumeOthers() {
            return resumeOthers;
        }

        public void setResumeOthers(int resumeOthers) {
            checkPermissionRange(resumeOthers);
            this.resumeOthers = resumeOthers;
        }

        public int getArchiveOthers() {
            return archiveOthers;
        }

        public void setArchiveOthers(int archiveOthers) {
            checkPermissionRange(archiveOthers);
            this.archiveOthers = archiveOthers;
        }

        public int getView() {
            return view;
        }

        public void setView(int view) {
            checkPermissionRange(view);
            this.view = view;
        }

        public int getViewOthers() {
            return viewOthers;
        }

        public void setViewOthers(int viewOthers) {
            checkPermissionRange(viewOthers);
            this.viewOthers = viewOthers;
        }

        public int getViewPvpMates() {
            return viewPvpMates;
        }

        public void setViewPvpMates(int viewPvpMates) {
            checkPermissionRange(viewPvpMates);
            this.viewPvpMates = viewPvpMates;
        }

        public int getInvite() {
            return invite;
        }

        public void setInvite(int invite) {
            this.invite = invite;
        }

        public int getDelete() {
            return delete;
        }

        public void setDelete(int delete) {
            checkPermissionRange(delete);
            this.delete = delete;
        }

        public int getDeleteOthers() {
            return deleteOthers;
        }

        public void setDeleteOthers(int deleteOthers) {
            checkPermissionRange(deleteOthers);
            this.deleteOthers = deleteOthers;
        }

        public int getList() {
            return list;
        }

        public void setList(int list) {
            checkPermissionRange(list);
            this.list = list;
        }

        public int getListOthers() {
            return listOthers;
        }

        public void setListOthers(int listOthers) {
            checkPermissionRange(listOthers);
            this.listOthers = listOthers;
        }

        public int getDraft() {
            return draft;
        }

        public void setDraft(int draft) {
            this.draft = draft;
        }

        public int getJoin() {
            return join;
        }

        public void setJoin(int join) {
            this.join = join;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Permissions that = (Permissions) o;

            return new EqualsBuilder()
                    .append(normalStart, that.normalStart)
                    .append(difficultStart, that.difficultStart)
                    .append(draft, that.draft)
                    .append(stop, that.stop)
                    .append(resume, that.resume)
                    .append(archive, that.archive)
                    .append(stopOthers, that.stopOthers)
                    .append(resumeOthers, that.resumeOthers)
                    .append(archiveOthers, that.archiveOthers)
                    .append(view, that.view)
                    .append(viewOthers, that.viewOthers)
                    .append(viewPvpMates, that.viewPvpMates)
                    .append(join, that.join)
                    .append(invite, that.invite)
                    .append(delete, that.delete)
                    .append(deleteOthers, that.deleteOthers)
                    .append(list, that.list)
                    .append(listOthers, that.listOthers)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(normalStart)
                    .append(difficultStart)
                    .append(draft)
                    .append(stop)
                    .append(resume)
                    .append(archive)
                    .append(stopOthers)
                    .append(resumeOthers)
                    .append(archiveOthers)
                    .append(view)
                    .append(viewOthers)
                    .append(viewPvpMates)
                    .append(join)
                    .append(invite)
                    .append(delete)
                    .append(deleteOthers)
                    .append(list)
                    .append(listOthers)
                    .toHashCode();
        }

        public void writeToJson5(JsonWriter writer) throws IOException {
            // TODO: write comments
            writer.beginObject();
            writer.name("normal-start").value(getNormalStart());
            writer.name("difficult-start").value(getDifficultStart());
            writer.name("draft").value(getDraft());
            writer.name("stop").value(getStop());
            writer.name("resume").value(getResume());
            writer.name("archive").value(getArchive());
            writer.name("stop-others").value(getStopOthers());
            writer.name("resume-others").value(getResumeOthers());
            writer.name("archive-others").value(getArchiveOthers());
            writer.name("view").value(getView());
            writer.name("view-others").value(getViewOthers());
            writer.name("view-pvp-mates").value(getViewPvpMates());
            writer.name("join").value(getJoin());
            writer.name("invite").value(getInvite());
            writer.name("delete").value(getDelete());
            writer.name("delete-others").value(getDeleteOthers());
            writer.name("list").value(getList());
            writer.name("list-others").value(getListOthers());
            writer.endObject();
        }

        public void readFromJson5(JsonReader reader) throws IOException {
            reader.beginObject();
            while (reader.peek() == JsonToken.NAME) {
                switch (reader.nextName()) {
                    case "normal-start" -> setNormalStart(reader.nextInt());
                    case "difficult-start" -> setDifficultStart(reader.nextInt());
                    case "draft" -> setDraft(reader.nextInt());
                    case "stop" -> setStop(reader.nextInt());
                    case "resume" -> setResume(reader.nextInt());
                    case "archive" -> setArchive(reader.nextInt());
                    case "stop-others" -> setStopOthers(reader.nextInt());
                    case "resume-others" -> setResumeOthers(reader.nextInt());
                    case "archive-others" -> setArchiveOthers(reader.nextInt());
                    case "view" -> setView(reader.nextInt());
                    case "view-others" -> setViewOthers(reader.nextInt());
                    case "view-pvp-mates" -> setViewPvpMates(reader.nextInt());
                    case "join" -> setJoin(reader.nextInt());
                    case "invite" -> setInvite(reader.nextInt());
                    case "delete" -> setDelete(reader.nextInt());
                    case "delete-others" -> setDeleteOthers(reader.nextInt());
                    case "list" -> setList(reader.nextInt());
                    case "list-others" -> setListOthers(reader.nextInt());
                    default -> reader.skipValue();
                }
            }
            reader.endObject();
        }

        private static void checkPermissionRange(int permission) throws IllegalArgumentException {
            Preconditions.checkArgument(permission >= 0 && permission < 5, "Illegal permission - expect [0, 4], got %s", permission);
        }
    }

    public boolean isItemsOnlyAvailableWhenRunning() {
        return itemsOnlyAvailableWhenRunning;
    }

    public void setItemsOnlyAvailableWhenRunning(boolean itemsOnlyAvailableWhenRunning) {
        this.itemsOnlyAvailableWhenRunning = itemsOnlyAvailableWhenRunning;
    }

    public boolean isStopOnQuit() {
        return stopOnQuit;
    }

    public void setStopOnQuit(boolean stopOnQuit) {
        this.stopOnQuit = stopOnQuit;
    }

    public boolean isTimerPausesWhenVacant() {
        return timerPausesWhenVacant;
    }

    public void setTimerPausesWhenVacant(boolean timerPausesWhenVacant) {
        this.timerPausesWhenVacant = timerPausesWhenVacant;
    }

    public ItemRunDifficultyRuleFactory getDifficultiesWithOp() {
        return difficultiesWithOp;
    }

    public void setDifficultiesWithOp(ItemRunDifficultyRuleFactory difficultiesWithOp) {
        this.difficultiesWithOp = difficultiesWithOp;
        calcDifficulties();    // recalculate difficult difficulties
    }

    public Permissions getPermissions() {
        return permissions;
    }

    public int getDefaultInvitationCooldown() {
        return defaultInvitationCooldown;
    }

    public void setDefaultInvitationCooldown(int defaultInvitationCooldown) {
        Preconditions.checkArgument(defaultInvitationCooldown > 0 && defaultInvitationCooldown < 1800);
        this.defaultInvitationCooldown = defaultInvitationCooldown;
    }

    public boolean isEnableLegacyCommands() {
        return enableLegacyCommands;
    }

    public void setEnableLegacyCommands(boolean enableLegacyCommands) {
        this.enableLegacyCommands = enableLegacyCommands;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        AlphabetSpeedrunConfigData that = (AlphabetSpeedrunConfigData) o;

        return new EqualsBuilder()
                .append(itemsOnlyAvailableWhenRunning, that.itemsOnlyAvailableWhenRunning)
                .append(stopOnQuit, that.stopOnQuit)
                .append(timerPausesWhenVacant, that.timerPausesWhenVacant)
                .append(defaultInvitationCooldown, that.defaultInvitationCooldown)
                .append(difficultiesWithOp, that.difficultiesWithOp)
                .append(permissions, that.permissions)
                .append(difficultDifficulties, that.difficultDifficulties)
                .append(enableLegacyCommands, that.enableLegacyCommands)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(1, 31)
                .append(itemsOnlyAvailableWhenRunning)
                .append(stopOnQuit)
                .append(timerPausesWhenVacant)
                .append(defaultInvitationCooldown)
                .append(difficultiesWithOp)
                .append(permissions)
                .append(difficultDifficulties)
                .append(enableLegacyCommands)
                .toHashCode();
    }

    public void writeToJson5(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("schema_version").value(CURRENT_SCHEMA);
        writer.comment("""
                Permission levels that are required in varieties of alphabet-speedrun mod.
                All these numeral values should be between 0 and 4 (both inclusive).""");
        writer.name("permissions"); getPermissions().writeToJson5(writer);

        writer.comment("""
                Note: this only works on the nine default difficulties.
                
                If this is set to true, the items given in one speedrun record is only
                available in the speedrun process. When the specific speedrun is finished or
                terminated, the items, including firework rockets and the unbreakable
                elytra, will be discarded.
                
                In addition, items will be re-given while resuming if this option is true.""");
        writer.name("items-only-available-when-running").value(isItemsOnlyAvailableWhenRunning());

        writer.comment("""
                If this is set to true, when a player quits the game/server, the running
                speedrun process will be stopped.
                It is recommended to turn "timer-pauses-when-vacant" into true alongside.
                Note that the record will NOT automatically start when the player rejoins.""");
        writer.name("stop-on-quit").value(isStopOnQuit());

        writer.comment("""
                If this is set to true, the "timer" will not run when a player terminates
                the speedrun process. Technically, a timestamp will be attached to the
                speedrun record when a player terminates the process, or otherwise it won't
                be attached, so that the mod engine won't know that it was paused.""");
        writer.name("timer-pauses-when-vacant").value(isTimerPausesWhenVacant());

        writer.comment("""
                Default time limit (seconds) for inviting other players to one's run, if not
                specified in the draft.""");
        writer.name("default-invitation-cooldown").value(getDefaultInvitationCooldown());

        writer.comment("""
                If this is on, legacy commands (/speedabc and /hannumspeed) will be enabled.
                Default to false.""");
        writer.name("enable-legacy-commands").value(isEnableLegacyCommands());

        final var ruleFactory = getDifficultiesWithOp();
        writer.comment("""
                A collection of item speedrun difficulties.
                
                Difficulties in the collection are treated as "difficult" difficulties,
                and requires players have the permission level of "difficult-start" to
                start an item speedrun.
                
                Difficulties out of the collection are treated as "normal" difficulties,
                and requires players have to permission level in "normal-start" to start
                an item speedrun.
                
                If "difficulties-with-op.inverted" is set to true, then the rules above
                are inverted - included difficulties will be "normal", while others are
                "difficult".""");
        if (ItemRunDifficultyRuleFactory.Impl.referringToAll(ruleFactory)) {
            writer.name("difficulties-with-op").value("ALL");
        } else {
            writer.name("difficulties-with-op");
            ItemRunDifficultyRuleFactory.Impl.serializeList(ruleFactory, writer);
            if (ruleFactory.isInverted())
                writer.name("difficulties-with-op.inverted").value(true);
        }

        writer.endObject();
    }

    // Return: should overwrite
    public boolean readFromJson5(JsonReader reader) throws IOException {
        boolean difficultyFactoryInverted = false;
        Function<Boolean, ItemRunDifficultyRuleFactory> factoryFactory = null;
        int schemaVersion = -1;

        reader.beginObject();
        while (reader.peek() == JsonToken.NAME) {
            switch (reader.nextName()) {
                case "schema_version" -> schemaVersion = reader.nextInt();
                case "permissions" -> getPermissions().readFromJson5(reader);
                case "items-only-available-when-running" -> setItemsOnlyAvailableWhenRunning(reader.nextBoolean());
                case "stop-on-quit" -> setStopOnQuit(reader.nextBoolean());
                case "timer-pauses-when-vacant" -> setTimerPausesWhenVacant(reader.nextBoolean());
                case "default-invitation-cooldown" -> setDefaultInvitationCooldown(reader.nextInt());
                case "enable-legacy-commands" -> setEnableLegacyCommands(reader.nextBoolean());
                case "difficulties-with-op.inverted" -> difficultyFactoryInverted = reader.nextBoolean();
                case "difficulties-with-op" -> factoryFactory = switch (reader.peek()) {
                    case STRING -> {
                        String s = reader.nextString();
                        if ("ALL".equals(s))    // inverted -> NONE (empty, false); !inverted -> ALL (empty, true)
                            yield (inverted -> new ItemRunDifficultyRuleFactory.Impl(Collections.emptyList(), !inverted));
                        if ("NONE".equals(s))
                            yield (inverted -> new ItemRunDifficultyRuleFactory.Impl(Collections.emptyList(), inverted));
                        BiFunction<Identifier, Boolean, ItemRunDifficultyRuleFactory> fun = (identifier, inverted) ->
                                new ItemRunDifficultyRuleFactory.Impl(Collections.singletonList(identifier), inverted);
                        if (s.startsWith("!")) yield (inverted) -> fun.apply(new Identifier(s.substring(1)), inverted);
                        yield (inverted) -> fun.apply(new Identifier(s), inverted);
                    }

                    case BEGIN_ARRAY -> {
                        List<Identifier> idList = new ArrayList<>();
                        reader.beginArray();
                        while (reader.peek() != JsonToken.END_ARRAY) {
                            Identifier id = new Identifier(reader.nextString());
                            idList.add(id);
                        }
                        reader.endArray();
                        yield inverted -> new ItemRunDifficultyRuleFactory.Impl(Collections.unmodifiableList(idList), inverted);
                    }

                    case NULL -> {
                        reader.nextNull();
                        yield (inverted -> new ItemRunDifficultyRuleFactory.Impl(Collections.emptyList(), inverted));
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + reader.peek());
                };
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        if (factoryFactory != null) {
            setDifficultiesWithOp(factoryFactory.apply(difficultyFactoryInverted));
        }
        return schemaVersion < CURRENT_SCHEMA;
    }

    private static volatile AlphabetSpeedrunConfigData INSTANCE;

    public static AlphabetSpeedrunConfigData getInstance() {
        if (INSTANCE == null) {
            synchronized (AlphabetSpeedrunConfigData.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AlphabetSpeedrunConfigData();
                }
            }
        }
        return INSTANCE;
    }

    private Collection<ItemSpeedrunDifficulty> difficultDifficulties;
    private void calcDifficulties() {
        this.difficultDifficulties = this.getDifficultiesWithOp().findDifficultDifficulties(DefaultItemSpeedrunDifficulty.getIdToObjMap());
    }

    public Collection<ItemSpeedrunDifficulty> getDifficultDifficulties() {
        if (ItemRunDifficultyRuleFactory.Impl.isInitialized()) {
            return difficultDifficulties;
        } else throw new IllegalStateException("Difficulty registry not initialized");
    }
}
