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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.gson.*;
import com.mojang.datafixers.util.Either;
import net.minecraft.advancement.Advancement;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.featurehouse.mcmod.speedrun.alphabeta.util.hooks.MultiverseHooks;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public sealed interface ItemPredicateProvider {
    // NOTE: the ItemStack should contain simple notation as tooltips
    Stream<SingleSpeedrunPredicate> flatMaps();

    static List<ItemPredicateProvider> fromJson(JsonElement element) {
        JsonArray arr = JsonHelper.asArray(element, "item_ctx");
        ImmutableList.Builder<ItemPredicateProvider> builder = ImmutableList.builder();
        for (JsonElement e : arr) {
            builder.add(fromSingle(e, true));
        }
        return builder.build();
    }

    private static ItemPredicateProvider fromSingle(JsonElement element, boolean checkIcon) throws IllegalArgumentException {
        if (JsonHelper.isString(element)) {
            return fromSingleString(element);
        } else if (element.isJsonObject()) {
            // items: [], item_predicate: []
            // tag: "", item_predicate: [], all: true
            JsonObject obj = element.getAsJsonObject();

            if (checkIcon && JsonHelper.hasJsonObject(obj, "icon")) {
                final JsonObject icon0 = obj.getAsJsonObject("icon");
                Impl.IconState iconState = Impl.IconState.getIconState(JsonHelper.getString(obj, "replace", null));
                final ItemStack itemStack = ItemSpeedrun.DataLoader.iconFromJson(icon0);
                return new Impl.WithExplicitIcon(iconState, itemStack, fromSingle(obj, false));
            }

            if (obj.has("advancement")) {
                Identifier id = new Identifier(JsonHelper.getString(obj, "advancement"));
                return new Impl.OfAdvancement(id);
            }

            if (JsonHelper.hasArray(obj, "items")) {
                if (JsonHelper.hasString(obj, "tag")) throw new IllegalArgumentException("Item & tag cannot exist at the same time");
                JsonArray items = obj.getAsJsonArray("items");
                List<Item> itemList = new ArrayList<>();
                for (JsonElement item : items) {
                    itemList.add(JsonHelper.asItem(item, "item"));
                }

                JsonObject predicate = JsonHelper.getObject(obj, "item_predicate", null);
                if (predicate == null) return new Impl.SimpleItem(itemList);
                predicate.add("items", items);
                predicate.remove("tag");
                ItemStack stack;
                if (!itemList.isEmpty()) stack = new ItemStack(itemList.get(0));
                else stack = Impl.anythingMarker();
                Impl.fillExtraRequirements(stack, predicate);
                return new Impl.CommonPredicate(ItemPredicate.fromJson(predicate), stack);
            } else if (JsonHelper.hasString(obj, "tag")) {
                String tag = JsonHelper.getString(obj, "tag");
                TagKey<Item> tagKey = TagKey.of(MultiverseHooks.itemKey(), new Identifier(tag));
                @Nullable JsonObject predicate = JsonHelper.getObject(obj, "item_predicate", null);
                if (JsonHelper.getBoolean(obj, "all", true)) {
                    if (predicate == null) return new Impl.EverythingInTag(tagKey);
                    //Impl.fillExtraRequirements(marker, predicate);
                    ItemPredicate.fromJson(predicate);    // throw json syntax/parse errors in advance
                    return new Impl.ComplexAllInTag(tagKey, predicate);
                } else {
                    if (predicate == null) return new Impl.AnythingInTag(tagKey);
                    ItemStack marker = Impl.anythingMarker();
                    predicate.remove("items");
                    predicate.addProperty("tag", tag);
                    Impl.fillExtraRequirements(marker, predicate);
                    return new Impl.CommonPredicate(ItemPredicate.fromJson(predicate), marker);
                }
            } else {
                JsonObject predicate = JsonHelper.getObject(obj, "item_predicate", null);
                if (predicate == null) return Impl.Any.INSTANCE;
                ItemStack stack = Impl.anythingMarker();
                Impl.fillExtraRequirements(stack, predicate);
                return new Impl.CommonPredicate(ItemPredicate.fromJson(predicate), stack);
            }
        } else throw new IllegalArgumentException("Providing non-string-or-object value");
    }

    private static ItemPredicateProvider fromSingleString(JsonElement element) {
        String s = element.getAsString();
        if (s.startsWith("#")) {
            TagKey<Item> tagKey = TagKey.of(MultiverseHooks.itemKey(), new Identifier(s.substring(1)));
            return new Impl.EverythingInTag(tagKey);
        } else {
            Item item = JsonHelper.asItem(element, "element");
            return new Impl.SimpleItem(item);
        }
    }

    final class Impl {
        private static ItemPredicate itemPredicate(Item... items) {
            return ItemPredicate.Builder.create()
                    .items(items).build();
        }

        private static ItemStack anythingMarker() {
            ItemStack stack = new ItemStack(Items.APPLE);
            stack.setCustomName(Text.translatable("item_predicate.speedrun.alphabet.extra_req.items.any"));
            return stack;
        }

        private static final class Any implements ItemPredicateProvider {
            static final Any INSTANCE = new Any();

            @Override
            public Stream<SingleSpeedrunPredicate> flatMaps() {
                return Stream.empty();
            }
        }

        private record CommonPredicate(ItemPredicate predicate, ItemStack stack) implements ItemPredicateProvider {
            @Override
            public Stream<SingleSpeedrunPredicate> flatMaps() {
                //return Stream.of(Pair.of(predicate(), stack()));
                return Stream.of(new SingleSpeedrunPredicate.OfItemPredicate(predicate(), stack()));
            }
        }

        private record SimpleItem(List<Item> items) implements ItemPredicateProvider {
            SimpleItem(Item item) {
                this(Collections.singletonList(item));
            }

            @Override
            public Stream<SingleSpeedrunPredicate> flatMaps() {
                if (items.size() == 1)
                    return Stream.of(mapItem(items.get(0)));
                return Stream.of(new SingleSpeedrunPredicate.OfItemPredicate(Impl.itemPredicate(items.toArray(new Item[0])), Impl.anythingMarker()));
            }
        }

        private static SingleSpeedrunPredicate mapItem(Item item) {
            final ItemPredicate predicate = Impl.itemPredicate(item);
            ItemStack stack = new ItemStack(item);
            //return Pair.of(predicate, stack);
            return new SingleSpeedrunPredicate.OfItemPredicate(predicate, stack);
        }

        private record EverythingInTag(TagKey<Item> tagKey) implements ItemPredicateProvider {
            @Override
            public Stream<SingleSpeedrunPredicate> flatMaps() {
                return MultiverseHooks.itemTagHolders(tagKey()).stream()
                        .map(RegistryEntry::value)
                        .map(Impl::mapItem);
            }
        }

        // @param itemPredicate must be checked.
        private record ComplexAllInTag(TagKey<Item> tagKey, JsonObject itemPredicate) implements ItemPredicateProvider {
            @Override
            public Stream<SingleSpeedrunPredicate> flatMaps() {
                return MultiverseHooks.itemTagHolders(tagKey()).stream()
                        .map(RegistryEntry::getKeyOrValue)
                        .map(either -> either.map(RegistryKey::getValue, MultiverseHooks::itemId))
                        .map(itemId -> {
                            JsonObject itemPredicate = itemPredicate().deepCopy();
                            itemPredicate.remove("tag");
                            JsonArray arr = new JsonArray();
                            arr.add(itemId.toString());
                            itemPredicate.add("items", arr);
                            ItemStack stack = new ItemStack(MultiverseHooks.getItem(itemId));
                            Impl.fillExtraRequirements(stack, itemPredicate);
                            //return Pair.of(ItemPredicate.fromJson(itemPredicate), stack);
                            return new SingleSpeedrunPredicate.OfItemPredicate(ItemPredicate.fromJson(itemPredicate), stack);
                        });
            }
        }

        private record AnythingInTag(TagKey<Item> tagKey) implements ItemPredicateProvider {
            @Override
            public Stream<SingleSpeedrunPredicate> flatMaps() {
                JsonObject obj = new JsonObject();
                obj.addProperty("tag", tagKey().id().toString());
                ItemStack stack = Impl.anythingMarker();
                fillExtraRequirements(stack, obj);
                return Stream.of(new SingleSpeedrunPredicate.OfItemPredicate(ItemPredicate.Builder.create().tag(tagKey()).build(), stack));
            }
        }

        private record OfAdvancement(Identifier advancementId) implements ItemPredicateProvider {
            @Override
            public Stream<SingleSpeedrunPredicate> flatMaps() {
                return Stream.of(new SingleSpeedrunPredicate.OfAdvancement(advancementId, Items.GRASS_BLOCK.getDefaultStack()));
            }
        }

        private enum IconState implements BiConsumer<ItemStack, ItemStack> {
            USE_ICON("icon", (a, b) -> {}),
            ICON_FIRST("covers_gen", (icon, right) -> {
                final NbtCompound rightNbt = right.getNbt();

                if (!icon.hasNbt()) {
                    icon.setNbt(rightNbt);
                } else if (rightNbt != null) {
                    // merge NBT
                    rightNbt.copyFrom(icon.getNbt());
                    icon.setNbt(rightNbt);
                }
            }),
            GEN_FIRST("covers_icon", (icon, right) -> {
                final NbtCompound rightNbt = right.getNbt();
                if (rightNbt != null) {
                    if (!icon.hasNbt())
                        icon.setNbt(rightNbt);
                    else rightNbt.copyFrom(icon.getNbt());
                }
            })
            ;
            private final String id;
            private final BiConsumer<ItemStack, ItemStack> consumer;

            private static final Map<String, IconState> BY_ID = Arrays.stream(values()).collect(Collectors.toMap(Object::toString, Function.identity()));

            IconState(String id, BiConsumer<ItemStack, ItemStack> c) {
                this.id = id;
                consumer = c;
            }

            public void accept(ItemStack icon, ItemStack generated) {
                consumer.accept(icon, generated);
            }

            @Override
            public String toString() {
                return id;
            }

            public static IconState getIconState(String id) {
                if (id == null)
                    return ICON_FIRST;
                final IconState ret = BY_ID.get(id);
                if (ret == null) {
                    throw new JsonParseException("Illegal icon state: " + id);
                }
                return ret;
            }
        }

        private record WithExplicitIcon(IconState iconState, ItemStack icon, ItemPredicateProvider wrapped) implements ItemPredicateProvider {
            @Override
            public Stream<SingleSpeedrunPredicate> flatMaps() {
                return wrapped().flatMaps().map(p -> {
                    final ItemStack icon = this.icon().copy();
                    final ItemStack right = p.getIcon();

                    this.iconState().accept(icon, right);
                    return new SingleSpeedrunPredicate() {
                        @Override
                        public boolean testItemStack(ItemStack stack) {
                            return p.testItemStack(stack);
                        }

                        @Override
                        public boolean fitsAdvancementGet(Advancement advancement) {
                            return p.fitsAdvancementGet(advancement);
                        }

                        @Override
                        public ItemStack getIcon() {
                            return icon;
                        }

                        @Override
                        public JsonObject serialize() {
                            JsonObject obj = p.serialize();
                            obj.add("icon", ItemSpeedrunRecord.stackToJson(icon));
                            return obj;
                        }
                    };
                });
            }
        }

        private Impl() {}

        private static void fillExtraRequirements(ItemStack stack, JsonObject itemPredicate) {
            // Count
            NumberRange.IntRange count = NumberRange.IntRange.fromJson(itemPredicate.get("count"));
            NumberRange.IntRange durability = NumberRange.IntRange.fromJson(itemPredicate.get("durability"));
            NbtList loreList = new NbtList();
            if (!count.isDummy())
                loreList.add(serializeText(Text.translatable("item_predicate.speedrun.alphabet.extra_req.durability",
                        formatNumber(durability.getMin(), durability.getMax()))));
            if (!durability.isDummy())
                loreList.add(serializeText(Text.translatable("item_predicate.speedrun.alphabet.extra_req.durability",
                        formatNumber(durability.getMin(), durability.getMax()))));
            if (itemPredicate.has("nbt"))
                loreList.add(serializeText(Text.translatable("item_predicate.speedrun.alphabet.extra_req.nbt")));
            if (itemPredicate.has("potion"))
                stack.getOrCreateNbt().putString("Potion", JsonHelper.getString(itemPredicate, "potion"));
            var storedEnchantmentCtx = readEnchantments(itemPredicate.get("stored_enchantments"));
            if (storedEnchantmentCtx != null) {
                storedEnchantmentCtx.ifRight(m ->
                        EnchantmentHelper.set(m, stack))
                        .ifLeft(l -> {
                            loreList.add(serializeText(Text.translatable("item_predicate.speedrun.alphabet.extra_req.enchantments.stored")));
                            l.forEach(t -> loreList.add(serializeText(t)));
                        });
            } else {
                var enchantmentCtx = readEnchantments(itemPredicate.get("enchantments"));
                if (enchantmentCtx != null) {
                    enchantmentCtx.ifRight(m ->
                            EnchantmentHelper.set(m, stack))
                            .ifLeft(l -> {
                                loreList.add(serializeText(Text.translatable("item_predicate.speedrun.alphabet.extra_req.enchantments")));
                                l.forEach(t -> loreList.add(serializeText(t)));
                            });
                }
            }
            if (itemPredicate.has("tag")) {
                final String tag = JsonHelper.getString(itemPredicate, "tag");
                loreList.add(serializeText(Text.translatable("item_predicate.speedrun.alphabet.extra_req.tag", tag)));
            } else {
                JsonArray items = JsonHelper.getArray(itemPredicate, "items", null);
                if (items != null && items.size() > 1) {
                    MutableText text = Text.empty();
                    boolean b = false;
                    for (JsonElement e : items) {
                        Identifier id = new Identifier(JsonHelper.asString(e, "item"));
                        final Item item = MultiverseHooks.getOptionalItem(id).orElseThrow(() -> new JsonSyntaxException("Unknown item id '" + id + '\''));
                        if (b) text.append(", ");
                        b = true;
                        text.append(Text.translatable(item.getTranslationKey()).formatted(Formatting.AQUA));
                    }
                    text = Text.translatable("item_predicate.speedrun.alphabet.extra_req.items", text);
                    loreList.add(serializeText(text));
                }
            }
            if (!loreList.isEmpty()) {
                stack.getOrCreateSubNbt("display").put("Lore", loreList);
            }
        }

        private static @Nullable Either<List<Text>, Map<Enchantment, Integer>> readEnchantments(@Nullable JsonElement el) {
            if (el == null || el.isJsonNull()) return null; // no conditions
            JsonArray arr = JsonHelper.asArray(el, "enchantments");
            Map<Enchantment, Integer> map = new HashMap<>();
            List<Text> texts = new ArrayList<>();
            for (JsonElement e : arr) {
                if (e.isJsonNull()) continue;
                final JsonObject o = JsonHelper.asObject(e, "enchantment");
                Enchantment enchantment = null;
                if (o.has("enchantment")) {
                    Identifier identifier = new Identifier(JsonHelper.getString(o, "enchantment"));
                    enchantment = MultiverseHooks.getOptionalEnchantment(identifier).orElseThrow(() -> new JsonSyntaxException("Unknown enchantment '" + identifier + '\''));
                }
                NumberRange.IntRange intRange = NumberRange.IntRange.fromJson(o.get("levels"));
                final boolean dummy = intRange.isDummy();
                if (enchantment == null && dummy) continue;
                if (dummy) {    // enchantment != null
                    texts.add(Text.translatable(enchantment.getTranslationKey()));
                    map = null;
                } else if (enchantment == null) {
                    texts.add(Text.translatable("item_predicate.speedrun.alphabet.extra_req.enchantments.any",
                            formatNumber(intRange.getMin(), intRange.getMax(), true)));
                    map = null;
                } else {
                    texts.add(Text.translatable(enchantment.getTranslationKey())
                            .append(" ").append(formatNumber(intRange.getMin(), intRange.getMax(), true)));
                    if (map != null && Objects.equals(intRange.getMin(), intRange.getMax()))
                        map.put(enchantment, intRange.getMin());
                }
            }
            if (map != null && !map.isEmpty()) return Either.right(map);
            return texts.isEmpty() ? null : Either.left(texts);
        }

        private static NbtString serializeText(Text text) {
            return NbtString.of(Text.Serializer.toJson(text));
        }

        private static Text formatNumber(@Nullable Integer min, @Nullable Integer max) {
            return formatNumber(min, max, false);
        }

        @Contract("null,null,_->fail")
        private static Text formatNumber(@Nullable Integer min, @Nullable Integer max, boolean isEnchanting) {
            Preconditions.checkArgument(min != null || max != null);
            if (min == null) return Text.translatable("item_predicate.speedrun.alphabet.extra_req.count.max", max);
            if (max == null) return Text.translatable("item_predicate.speedrun.alphabet.extra_req.count.min", min);
            if (min.equals(max))
                return isEnchanting ? Text.translatable("enchantment.level." + min)
                        : Text.translatable("item_predicate.speedrun.alphabet.extra_req.count.exact", min);
            return Text.translatable("item_predicate.speedrun.alphabet.extra_req.count.between", min, max);
        }
    }
}
