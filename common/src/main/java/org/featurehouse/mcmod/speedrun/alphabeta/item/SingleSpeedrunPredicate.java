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
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public interface SingleSpeedrunPredicate {
    // TODO: make CODEC-alize
    default boolean testItemStack(ItemStack stack) {
        return false;
    }

    default boolean fitsAdvancementGet(AdvancementEntry advancement) {
        return false;
    }

    ItemStack icon();

    //@Deprecated
    JsonObject serialize();

    String predicateType();

    Codec<SingleSpeedrunPredicate> CODEC = Codec.STRING.partialDispatch(
            "predicate_type",
            p -> DataResult.success(p.predicateType()),
            s -> switch (s) {
                case "item" -> DataResult.success(RecordCodecBuilder.<OfItemPredicate>mapCodec(
                        instance -> instance.group(
                                ItemPredicate.CODEC.fieldOf("item_predicate").forGetter(OfItemPredicate::predicate),
                                ItemStack.CODEC.fieldOf("icon").forGetter(OfItemPredicate::icon)
                        ).apply(instance, OfItemPredicate::new)
                ));
                case "advancement" -> DataResult.success(RecordCodecBuilder.<OfAdvancement>mapCodec(
                        instance -> instance.group(
                                Identifier.CODEC.fieldOf("advancement_id").forGetter(OfAdvancement::advancementId),
                                ItemStack.CODEC.fieldOf("icon").forGetter(OfAdvancement::icon)
                        ).apply(instance, OfAdvancement::new)
                ));
                default -> DataResult.error(() -> "Expecting predicate_type as item / advancement, got " + s);
            }
    );

    static SingleSpeedrunPredicate deserialize(JsonObject obj) {
        ItemStack icon = ItemStack.CODEC.parse(JsonOps.INSTANCE, JsonHelper.getObject(obj, "icon")).getOrThrow();
        return switch (JsonHelper.getString(obj, "predicate_type")) {
            case "item" -> {
                ItemPredicate itemPredicate = ItemPredicate.CODEC.parse(JsonOps.INSTANCE, obj.get("item_predicate")).getOrThrow(JsonParseException::new);
                yield new OfItemPredicate(itemPredicate, icon);
            }
            case "advancement" -> {
                Identifier advancementId = Identifier.of(JsonHelper.getString(obj, "advancement_id"));
                yield new OfAdvancement(advancementId, icon);
            }
            default -> throw new JsonParseException("Expecting predicate_type as item / advancement, got" + obj.get("predicate_type"));
        };
    }

    record OfItemPredicate(ItemPredicate predicate, ItemStack icon) implements SingleSpeedrunPredicate {
        @Override
        public boolean testItemStack(ItemStack stack) {
            return predicate.test(stack);
        }

        @Override
        public ItemStack icon() {
            return icon.copy();
        }

        @Override
        public JsonObject serialize() {
            JsonObject obj = new JsonObject();
            obj.addProperty("predicate_type", "item");
            obj.add("item_predicate", ItemPredicate.CODEC.encodeStart(JsonOps.INSTANCE, predicate).getOrThrow());
            obj.add("icon", ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, icon).getOrThrow());
            return obj;
        }

        @Override
        public String predicateType() {
            return "item";
        }
    }

    record OfAdvancement(Identifier advancementId, ItemStack icon) implements SingleSpeedrunPredicate {
        @Override
        public boolean fitsAdvancementGet(AdvancementEntry advancement) {
            return advancement.id().equals(advancementId);
        }

        @Override
        public ItemStack icon() {
            return icon.copy();
        }

        @Override
        public JsonObject serialize() {
            JsonObject obj = new JsonObject();
            obj.addProperty("predicate_type", "advancement");
            obj.addProperty("advancement_id", advancementId.toString());
            obj.add("icon", ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, icon).getOrThrow());
            return obj;
        }

        @Override
        public String predicateType() {
            return "advancement";
        }
    }
}
