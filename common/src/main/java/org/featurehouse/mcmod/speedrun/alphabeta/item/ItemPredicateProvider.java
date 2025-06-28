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
import com.mojang.serialization.JsonOps;
import org.featurehouse.mcmod.speedrun.alphabeta.mixin.EnchantmentsPredicateAccessor;
import org.featurehouse.mcmod.speedrun.alphabeta.util.hooks.MultiverseHooks;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.critereon.DataComponentMatchers;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.predicates.DamagePredicate;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.component.predicates.DataComponentPredicates;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public sealed interface ItemPredicateProvider {
    // NOTE: the ItemStack should contain simple notation as tooltips
    Stream<SingleSpeedrunPredicate> flatMaps();

    static List<ItemPredicateProvider> fromJson(JsonElement element) {
        JsonArray arr = GsonHelper.convertToJsonArray(element, "item_ctx");
        ImmutableList.Builder<ItemPredicateProvider> builder = ImmutableList.builder();
        for (JsonElement e : arr) {
            builder.add(fromSingle(e, true));
        }
        return builder.build();
    }

    private static ItemPredicateProvider fromSingle(JsonElement element, boolean checkIcon) throws IllegalArgumentException {
        if (GsonHelper.isStringValue(element)) {
            return fromSingleString(element);
        } else if (element.isJsonObject()) {
            // items: [], item_predicate: []
            // tag: "", item_predicate: [], all: true
            JsonObject obj = element.getAsJsonObject();

            if (checkIcon && GsonHelper.isObjectNode(obj, "icon")) {
                final JsonObject icon0 = obj.getAsJsonObject("icon");
                Impl.IconState iconState = Impl.IconState.getIconState(GsonHelper.getAsString(obj, "replace", null));
                final ItemStack itemStack = ItemSpeedrun.DataLoader.iconFromJson(icon0);
                return new Impl.WithExplicitIcon(iconState, itemStack, fromSingle(obj, false));
            }

            if (obj.has("advancement")) {
                ResourceLocation id = ResourceLocation.parse(GsonHelper.getAsString(obj, "advancement"));
                return new Impl.OfAdvancement(id);
            }

            if (GsonHelper.isArrayNode(obj, "items")) {
                if (GsonHelper.isStringValue(obj, "tag"))
                    throw new IllegalArgumentException("Item & tag cannot exist at the same time");

                JsonArray items = obj.getAsJsonArray("items");
                List<Holder<Item>> itemList = new ArrayList<>();
                for (JsonElement item : items) {
                    itemList.add(GsonHelper.convertToItem(item, "item"));
                }

                JsonObject predicate = GsonHelper.getAsJsonObject(obj, "item_predicate", null);
                if (predicate == null)
                    return new Impl.SimpleItem(HolderSet.direct(itemList));

                ItemStack stack = itemList.isEmpty() ? Impl.anythingMarker() : new ItemStack(itemList.getFirst());

                ItemPredicate itemPredicate = Impl.parseItemPredicate(predicate);
                Impl.fillExtraRequirements(stack, Either.right(HolderSet.direct(itemList)), itemPredicate);

                return new Impl.CommonPredicate(itemPredicate, stack);
            } else if (GsonHelper.isStringValue(obj, "tag")) {
                String tag = GsonHelper.getAsString(obj, "tag");
                TagKey<Item> tagKey = TagKey.create(MultiverseHooks.itemKey(), ResourceLocation.parse(tag));
                @Nullable JsonObject predicate = GsonHelper.getAsJsonObject(obj, "item_predicate", null);
                if (GsonHelper.getAsBoolean(obj, "all", true)) {
                    if (predicate == null) return new Impl.EverythingInTag(tagKey);
                    return new Impl.ComplexAllInTag(tagKey, Impl.parseItemPredicate(predicate));
                } else {
                    if (predicate == null) return new Impl.AnythingInTag(tagKey);

                    ItemStack marker = Impl.anythingMarker();
                    ItemPredicate itemPredicate = Impl.parseItemPredicate(predicate);

                    Impl.fillExtraRequirements(marker, Either.left(tagKey), itemPredicate);
                    itemPredicate = ItemPredicate.Builder.item()
                            .of(BuiltInRegistries.ITEM, tagKey)
                            .withCount(itemPredicate.count())
                            .withComponents(itemPredicate.components())
                            .build();

                    return new Impl.CommonPredicate(itemPredicate, marker);
                }
            } else {
                JsonObject predicate = GsonHelper.getAsJsonObject(obj, "item_predicate", null);
                if (predicate == null) return Impl.Any.INSTANCE;

                ItemStack stack = Impl.anythingMarker();
                ItemPredicate itemPredicate = Impl.parseItemPredicate(predicate);
                Impl.fillExtraRequirements(stack, null, itemPredicate);
                return new Impl.CommonPredicate(itemPredicate, stack);
            }
        } else throw new IllegalArgumentException("Providing non-string-or-object value");
    }

    private static ItemPredicateProvider fromSingleString(JsonElement element) {
        String s = element.getAsString();
        if (s.startsWith("#")) {
            TagKey<Item> tagKey = TagKey.create(MultiverseHooks.itemKey(), ResourceLocation.parse(s.substring(1)));
            return new Impl.EverythingInTag(tagKey);
        } else {
            Holder<Item> item = GsonHelper.convertToItem(element, "element");
            return new Impl.SimpleItem(item);
        }
    }

    final class Impl {

        private static ItemPredicate parseItemPredicate(JsonObject object) {
            return ItemPredicate.CODEC.parse(JsonOps.INSTANCE, object).getOrThrow(JsonParseException::new);
        }

        @SafeVarargs
        private static ItemPredicate itemPredicate(Holder<Item>... items) {
            return itemPredicate(HolderSet.direct(items));
        }

        private static ItemPredicate itemPredicate(HolderSet<Item> items) {
            return new ItemPredicate(Optional.of(items), MinMaxBounds.Ints.ANY, DataComponentMatchers.ANY);
        }

        private static ItemStack anythingMarker() {
            ItemStack stack = new ItemStack(Items.APPLE);
            stack.set(DataComponents.ITEM_NAME, Component.translatable("item_predicate.speedrun.alphabet.extra_req.items.any"));
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

        private record SimpleItem(HolderSet<Item> items) implements ItemPredicateProvider {
            SimpleItem(Holder<Item> item) {
                this(HolderSet.direct(item));
            }

            @Override
            public Stream<SingleSpeedrunPredicate> flatMaps() {
                if (items.size() == 1)
                    return Stream.of(mapItem(items.get(0)));
                return Stream.of(new SingleSpeedrunPredicate.OfItemPredicate(Impl.itemPredicate(items), Impl.anythingMarker()));
            }
        }

        private static SingleSpeedrunPredicate mapItem(Holder<Item> item) {
            final ItemPredicate predicate = Impl.itemPredicate(item);
            ItemStack stack = new ItemStack(item);
            return new SingleSpeedrunPredicate.OfItemPredicate(predicate, stack);
        }

        private record EverythingInTag(TagKey<Item> tagKey) implements ItemPredicateProvider {
            @Override
            public Stream<SingleSpeedrunPredicate> flatMaps() {
                return MultiverseHooks.itemTagHolders(tagKey()).stream().map(Impl::mapItem);
            }
        }

        // @param itemPredicate must be checked.
        private record ComplexAllInTag(TagKey<Item> tagKey, ItemPredicate predicate) implements ItemPredicateProvider {
            @Override
            public Stream<SingleSpeedrunPredicate> flatMaps() {
                return MultiverseHooks.itemTagHolders(tagKey()).stream()
                        .map(Holder::unwrap)
                        .map(either -> either.map(MultiverseHooks::getItem, Function.identity()))
                        .map(item -> {
                            ItemPredicate itemPredicate = new ItemPredicate(
                                    Optional.of(HolderSet.direct(Holder.direct(item))),
                                    predicate().count(),
                                    predicate().components()
                            );
                            return new SingleSpeedrunPredicate.OfItemPredicate(itemPredicate, new ItemStack(item));
                        });
            }
        }

        private record AnythingInTag(TagKey<Item> tagKey) implements ItemPredicateProvider {
            @Override
            public Stream<SingleSpeedrunPredicate> flatMaps() {
                ItemStack stack = Impl.anythingMarker();
                fillExtraRequirements(stack, Either.left(tagKey()), null);
                return Stream.of(new SingleSpeedrunPredicate.OfItemPredicate(
                        ItemPredicate.Builder.item().of(BuiltInRegistries.ITEM, tagKey()).build(),
                        stack
                ));
            }
        }

        private record OfAdvancement(ResourceLocation advancementId) implements ItemPredicateProvider {
            @Override
            public Stream<SingleSpeedrunPredicate> flatMaps() {
                return Stream.of(new SingleSpeedrunPredicate.OfAdvancement(advancementId, Items.GRASS_BLOCK.getDefaultInstance()));
            }
        }

        private enum IconState implements BiConsumer<ItemStack, ItemStack> {
            USE_ICON("icon", (a, b) -> {}),
            ICON_FIRST("covers_gen", (icon, right) -> icon.applyComponents(DataComponentMap.composite(icon.getComponents(), right.getComponents()))),
            GEN_FIRST("covers_icon", (icon, right) -> icon.applyComponents(DataComponentMap.composite(right.getComponents(), icon.getComponents()))),
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

            public static IconState getIconState(@Nullable String id) {
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
                    final ItemStack right = p.icon();

                    this.iconState().accept(icon, right);
                    return new SingleSpeedrunPredicate() {
                        @Override
                        public boolean testItemStack(ItemStack stack) {
                            return p.testItemStack(stack);
                        }

                        @Override
                        public boolean fitsAdvancementGet(AdvancementHolder advancement) {
                            return p.fitsAdvancementGet(advancement);
                        }

                        @Override
                        public ItemStack icon() {
                            return icon;
                        }

                        @Override
                        public JsonObject serialize() {
                            JsonObject obj = p.serialize();
                            obj.add("icon", ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, icon).getOrThrow());
                            return obj;
                        }

                        @Override
                        public String predicateType() {
                            return p.predicateType();
                        }
                    };
                });
            }
        }

        private Impl() {}

        private static void fillExtraRequirements(ItemStack stack,
                                                  @Nullable Either<TagKey<Item>, HolderSet<Item>> ofAny,
                                                  @Nullable ItemPredicate predicate) {
            List<Component> appendedLores = new ArrayList<>();

            if (predicate != null) {
                // Count
                MinMaxBounds.Ints count = predicate.count();
                if (!count.isAny())
                    appendedLores.add(Component.translatable("item_predicate.speedrun.alphabet.extra_req.count", formatIntRange(count)));

                DataComponentPatch componentChanges = predicate.components().exact().asPatch();
                Map<DataComponentPredicate.Type<?>, DataComponentPredicate> partial = predicate.components().partial();
                // Damage
                componentFromChanges(componentChanges, DataComponents.DAMAGE).ifPresentOrElse(damage -> {
                    appendedLores.add(Component.translatable("item_predicate.speedrun.alphabet.extra_req.damage", formatNumber(damage, damage)));
                }, () -> {
                    if (partial.get(DataComponentPredicates.DAMAGE) instanceof DamagePredicate(
                            MinMaxBounds.Ints durability, MinMaxBounds.Ints damage
                    )) {
                        appendedLores.add(Component.translatable("item_predicate.speedrun.alphabet.extra_req.durability", formatIntRange(durability)));
                        appendedLores.add(Component.translatable("item_predicate.speedrun.alphabet.extra_req.damage", formatIntRange(damage)));
                    }
                });
                // Enchantments
                readEnchantments(componentChanges, DataComponents.STORED_ENCHANTMENTS, partial.get(DataComponentPredicates.STORED_ENCHANTMENTS), appendedLores::add);
                readEnchantments(componentChanges, DataComponents.ENCHANTMENTS, partial.get(DataComponentPredicates.ENCHANTMENTS), appendedLores::add);

                // Custom Data

                // TODO: fill other extra requirements
            }

            if (!appendedLores.isEmpty()) {
                List<Component> lines = Optional.ofNullable(stack.get(DataComponents.LORE))
                        .map(ItemLore::lines)
                        .orElse(Collections.emptyList());
                lines = new ArrayList<>(lines);
                lines.addAll(appendedLores);
                stack.set(DataComponents.LORE, new ItemLore(lines));
            }
        }

        private static void readEnchantments(DataComponentPatch componentChanges,
                                             DataComponentType<ItemEnchantments> componentType,
                                             @Nullable DataComponentPredicate componentPredicate,
                                             Consumer<Component> loreAdder) {
            componentFromChanges(componentChanges, componentType).ifPresent(enchantments -> {
                enchantments.entrySet().forEach(entry -> {
                    Holder<Enchantment> enchantmentType = entry.getKey();
                    int enchantmentLevel = entry.getIntValue();
                    // Name
                    loreAdder.accept(Component.empty()
                            .append(enchantmentType.value().description())
                            .append(" ")
                            .append(formatNumber(enchantmentLevel, enchantmentLevel, true))
                    );
                });
            });
            if (componentPredicate instanceof EnchantmentsPredicateAccessor enchantmentsPredicate) {
                enchantmentsPredicate.alphabetSpeedrun$getEnchantments().forEach(enchantmentPredicate -> {
                    Optional<HolderSet<Enchantment>> enchantments = enchantmentPredicate.enchantments();
                    MinMaxBounds.Ints levels = enchantmentPredicate.level();

                    if (enchantments.isEmpty()) {
                        loreAdder.accept(Component.translatable("item_predicate.speedrun.alphabet.extra_req.enchantments.any", formatEnchantmentLevels(levels)));
                    } else {
                        int size = enchantments.get().size();
                        if (size == 0) return;  // contains nothing

                        loreAdder.accept(Component.empty()
                                .append(enchantments.get().get(0).value().description())
                                .append(size == 1 ? " " : "... ")
                                .append(formatEnchantmentLevels(levels))
                        );
                    }
                });
            }
        }

        private static Component formatIntRange(MinMaxBounds.Ints intRange) {
            return formatNumber(intRange.min().orElse(null), intRange.max().orElse(null));
        }

        private static Component formatEnchantmentLevels(MinMaxBounds.Ints intRange) {
            return formatNumber(intRange.min().orElse(null), intRange.max().orElse(null), true);
        }

        private static Component formatNumber(@Nullable Integer min, @Nullable Integer max) {
            return formatNumber(min, max, false);
        }

        @Contract("null,null,_->fail")
        private static Component formatNumber(@Nullable Integer min, @Nullable Integer max, boolean isEnchanting) {
            Preconditions.checkArgument(min != null || max != null);
            if (min == null) return Component.translatable("item_predicate.speedrun.alphabet.extra_req.count.max", max);
            if (max == null) return Component.translatable("item_predicate.speedrun.alphabet.extra_req.count.min", min);
            if (min.equals(max))
                return isEnchanting ? Component.translatable("enchantment.level." + min)
                        : Component.translatable("item_predicate.speedrun.alphabet.extra_req.count.exact", min);
            return Component.translatable("item_predicate.speedrun.alphabet.extra_req.count.between", min, max);
        }

        private static <T> Optional<? extends T> componentFromChanges(DataComponentPatch changes, DataComponentType<? extends T> componentType) {
            Optional<? extends T> t = changes.get(componentType);
            if (t == null) return Optional.empty();
            return t;
        }
    }
}
