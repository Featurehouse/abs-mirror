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

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.advancement.Advancement;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public interface SingleSpeedrunPredicate {
    default boolean testItemStack(ItemStack stack) {
        return false;
    }

    default boolean fitsAdvancementGet(Advancement advancement) {
        return false;
    }

    ItemStack getIcon();

    JsonObject serialize();

    static SingleSpeedrunPredicate deserialize(JsonObject obj) {
        ItemStack icon = ItemSpeedrunRecord.jsonToStack(JsonHelper.getObject(obj, "icon"));
        return switch (JsonHelper.getString(obj, "predicate_type")) {
            case "item" -> {
                ItemPredicate itemPredicate = ItemPredicate.fromJson(obj.get("item_predicate"));
                yield new OfItemPredicate(itemPredicate, icon);
            }
            case "advancement" -> {
                Identifier advancementId = new Identifier(JsonHelper.getString(obj, "advancement_id"));
                yield new OfAdvancement(advancementId, icon);
            }
            default -> throw new JsonParseException("Expecting predicate_type as item / advancement, got" + obj.get("predicate_type"));
        };
    }

    final class OfItemPredicate implements SingleSpeedrunPredicate {
        private final ItemPredicate predicate;
        private final ItemStack icon;

        public OfItemPredicate(ItemPredicate predicate, ItemStack icon) {
            this.predicate = predicate;
            this.icon = icon;
        }

        @Override
        public boolean testItemStack(ItemStack stack) {
            return predicate.test(stack);
        }

        @Override
        public ItemStack getIcon() {
            return icon.copy();
        }

        @Override
        public JsonObject serialize() {
            JsonObject obj = new JsonObject();
            obj.addProperty("predicate_type", "item");
            obj.add("item_predicate", predicate.toJson());
            obj.add("icon", ItemSpeedrunRecord.stackToJson(icon));
            return obj;
        }
    }

    final class OfAdvancement implements SingleSpeedrunPredicate {
        private final Identifier advancementId;
        private final ItemStack icon;

        public OfAdvancement(Identifier advancementId, ItemStack icon) {
            this.advancementId = advancementId;
            this.icon = icon;
        }

        @Override
        public boolean fitsAdvancementGet(Advancement advancement) {
            return advancement.getId().equals(advancementId);
        }

        @Override
        public ItemStack getIcon() {
            return icon.copy();
        }

        @Override
        public JsonObject serialize() {
            JsonObject obj = new JsonObject();
            obj.addProperty("predicate_type", "advancement");
            obj.addProperty("advancement_id", advancementId.toString());
            obj.add("icon", ItemSpeedrunRecord.stackToJson(icon));
            return obj;
        }
    }
}
