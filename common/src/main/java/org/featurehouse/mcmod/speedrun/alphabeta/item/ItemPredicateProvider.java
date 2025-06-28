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
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.component.ComponentPredicate;
import net.minecraft.predicate.component.ComponentPredicateTypes;
import net.minecraft.predicate.component.ComponentsPredicate;
import net.minecraft.predicate.item.DamagePredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
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
                Identifier id = Identifier.of(JsonHelper.getString(obj, "advancement"));
                return new Impl.OfAdvancement(id);
            }

            if (JsonHelper.hasArray(obj, "items")) {
                if (JsonHelper.hasString(obj, "tag"))
                    throw new IllegalArgumentException("Item & tag cannot exist at the same time");

                JsonArray items = obj.getAsJsonArray("items");
                List<RegistryEntry<Item>> itemList = new ArrayList<>();
                for (JsonElement item : items) {
                    itemList.add(JsonHelper.asItem(item, "item"));
                }

                JsonObject predicate = JsonHelper.getObject(obj, "item_predicate", null);
                if (predicate == null)
                    return new Impl.SimpleItem(RegistryEntryList.of(itemList));

                ItemStack stack = itemList.isEmpty() ? Impl.anythingMarker() : new ItemStack(itemList.getFirst());

                ItemPredicate itemPredicate = Impl.parseItemPredicate(predicate);
                Impl.fillExtraRequirements(stack, Either.right(RegistryEntryList.of(itemList)), itemPredicate);

                return new Impl.CommonPredicate(itemPredicate, stack);
            } else if (JsonHelper.hasString(obj, "tag")) {
                String tag = JsonHelper.getString(obj, "tag");
                TagKey<Item> tagKey = TagKey.of(MultiverseHooks.itemKey(), Identifier.of(tag));
                @Nullable JsonObject predicate = JsonHelper.getObject(obj, "item_predicate", null);
                if (JsonHelper.getBoolean(obj, "all", true)) {
                    if (predicate == null) return new Impl.EverythingInTag(tagKey);
                    return new Impl.ComplexAllInTag(tagKey, Impl.parseItemPredicate(predicate));
                } else {
                    if (predicate == null) return new Impl.AnythingInTag(tagKey);

                    ItemStack marker = Impl.anythingMarker();
                    ItemPredicate itemPredicate = Impl.parseItemPredicate(predicate);

                    Impl.fillExtraRequirements(marker, Either.left(tagKey), itemPredicate);
                    itemPredicate = ItemPredicate.Builder.create()
                            .tag(Registries.ITEM, tagKey)
                            .count(itemPredicate.count())
                            .components(itemPredicate.components())
                            .build();

                    return new Impl.CommonPredicate(itemPredicate, marker);
                }
            } else {
                JsonObject predicate = JsonHelper.getObject(obj, "item_predicate", null);
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
            TagKey<Item> tagKey = TagKey.of(MultiverseHooks.itemKey(), Identifier.of(s.substring(1)));
            return new Impl.EverythingInTag(tagKey);
        } else {
            RegistryEntry<Item> item = JsonHelper.asItem(element, "element");
            return new Impl.SimpleItem(item);
        }
    }

    final class Impl {

        private static ItemPredicate parseItemPredicate(JsonObject object) {
            return ItemPredicate.CODEC.parse(JsonOps.INSTANCE, object).getOrThrow(JsonParseException::new);
        }

        @SafeVarargs
        private static ItemPredicate itemPredicate(RegistryEntry<Item>... items) {
            return itemPredicate(RegistryEntryList.of(items));
        }

        private static ItemPredicate itemPredicate(RegistryEntryList<Item> items) {
            return new ItemPredicate(Optional.of(items), NumberRange.IntRange.ANY, ComponentsPredicate.EMPTY);
        }

        private static ItemStack anythingMarker() {
            ItemStack stack = new ItemStack(Items.APPLE);
            stack.set(DataComponentTypes.ITEM_NAME, Text.translatable("item_predicate.speedrun.alphabet.extra_req.items.any"));
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

        private record SimpleItem(RegistryEntryList<Item> items) implements ItemPredicateProvider {
            SimpleItem(RegistryEntry<Item> item) {
                this(RegistryEntryList.of(item));
            }

            @Override
            public Stream<SingleSpeedrunPredicate> flatMaps() {
                if (items.size() == 1)
                    return Stream.of(mapItem(items.get(0)));
                return Stream.of(new SingleSpeedrunPredicate.OfItemPredicate(Impl.itemPredicate(items), Impl.anythingMarker()));
            }
        }

        private static SingleSpeedrunPredicate mapItem(RegistryEntry<Item> item) {
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
                        .map(RegistryEntry::getKeyOrValue)
                        .map(either -> either.map(MultiverseHooks::getItem, Function.identity()))
                        .map(item -> {
                            ItemPredicate itemPredicate = new ItemPredicate(
                                    Optional.of(RegistryEntryList.of(RegistryEntry.of(item))),
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
                        ItemPredicate.Builder.create().tag(Registries.ITEM, tagKey()).build(),
                        stack
                ));
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
            ICON_FIRST("covers_gen", (icon, right) -> icon.applyComponentsFrom(ComponentMap.of(icon.getComponents(), right.getComponents()))),
            GEN_FIRST("covers_icon", (icon, right) -> icon.applyComponentsFrom(ComponentMap.of(right.getComponents(), icon.getComponents()))),
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
                        public boolean fitsAdvancementGet(AdvancementEntry advancement) {
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
                                                  @Nullable Either<TagKey<Item>, RegistryEntryList<Item>> ofAny,
                                                  @Nullable ItemPredicate predicate) {
            List<Text> appendedLores = new ArrayList<>();

            if (predicate != null) {
                // Count
                NumberRange.IntRange count = predicate.count();
                if (!count.isDummy())
                    appendedLores.add(Text.translatable("item_predicate.speedrun.alphabet.extra_req.count", formatIntRange(count)));

                ComponentChanges componentChanges = predicate.components().exact().toChanges();
                Map<ComponentPredicate.Type<?>, ComponentPredicate> partial = predicate.components().partial();
                // Damage
                componentFromChanges(componentChanges, DataComponentTypes.DAMAGE).ifPresentOrElse(damage -> {
                    appendedLores.add(Text.translatable("item_predicate.speedrun.alphabet.extra_req.damage", formatNumber(damage, damage)));
                }, () -> {
                    if (partial.get(ComponentPredicateTypes.DAMAGE) instanceof DamagePredicate(
                            NumberRange.IntRange durability, NumberRange.IntRange damage
                    )) {
                        appendedLores.add(Text.translatable("item_predicate.speedrun.alphabet.extra_req.durability", formatIntRange(durability)));
                        appendedLores.add(Text.translatable("item_predicate.speedrun.alphabet.extra_req.damage", formatIntRange(damage)));
                    }
                });
                // Enchantments
                readEnchantments(componentChanges, DataComponentTypes.STORED_ENCHANTMENTS, partial.get(ComponentPredicateTypes.STORED_ENCHANTMENTS), appendedLores::add);
                readEnchantments(componentChanges, DataComponentTypes.ENCHANTMENTS, partial.get(ComponentPredicateTypes.ENCHANTMENTS), appendedLores::add);

                // Custom Data

                // TODO: fill other extra requirements
            }

            if (!appendedLores.isEmpty()) {
                List<Text> lines = Optional.ofNullable(stack.get(DataComponentTypes.LORE))
                        .map(LoreComponent::lines)
                        .orElse(Collections.emptyList());
                lines = new ArrayList<>(lines);
                lines.addAll(appendedLores);
                stack.set(DataComponentTypes.LORE, new LoreComponent(lines));
            }
        }

        private static void readEnchantments(ComponentChanges componentChanges,
                                             ComponentType<ItemEnchantmentsComponent> componentType,
                                             @Nullable ComponentPredicate componentPredicate,
                                             Consumer<Text> loreAdder) {
            componentFromChanges(componentChanges, componentType).ifPresent(enchantments -> {
                enchantments.getEnchantmentEntries().forEach(entry -> {
                    RegistryEntry<Enchantment> enchantmentType = entry.getKey();
                    int enchantmentLevel = entry.getIntValue();
                    // Name
                    loreAdder.accept(Text.empty()
                            .append(enchantmentType.value().description())
                            .append(" ")
                            .append(formatNumber(enchantmentLevel, enchantmentLevel, true))
                    );
                });
            });
            if (componentPredicate instanceof EnchantmentsPredicateAccessor enchantmentsPredicate) {
                enchantmentsPredicate.alphabetSpeedrun$getEnchantments().forEach(enchantmentPredicate -> {
                    Optional<RegistryEntryList<Enchantment>> enchantments = enchantmentPredicate.enchantments();
                    NumberRange.IntRange levels = enchantmentPredicate.levels();

                    if (enchantments.isEmpty()) {
                        loreAdder.accept(Text.translatable("item_predicate.speedrun.alphabet.extra_req.enchantments.any", formatEnchantmentLevels(levels)));
                    } else {
                        int size = enchantments.get().size();
                        if (size == 0) return;  // contains nothing

                        loreAdder.accept(Text.empty()
                                .append(enchantments.get().get(0).value().description())
                                .append(size == 1 ? " " : "... ")
                                .append(formatEnchantmentLevels(levels))
                        );
                    }
                });
            }
        }

        private static Text formatIntRange(NumberRange.IntRange intRange) {
            return formatNumber(intRange.min().orElse(null), intRange.max().orElse(null));
        }

        private static Text formatEnchantmentLevels(NumberRange.IntRange intRange) {
            return formatNumber(intRange.min().orElse(null), intRange.max().orElse(null), true);
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

        private static <T> Optional<? extends T> componentFromChanges(ComponentChanges changes, ComponentType<? extends T> componentType) {
            Optional<? extends T> t = changes.get(componentType);
            if (t == null) return Optional.empty();
            return t;
        }
    }
}
