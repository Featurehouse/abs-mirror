package org.featurehouse.mcmod.speedrun.alphabeta.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.core.component.predicates.EnchantmentsPredicate;

@Mixin(EnchantmentsPredicate.class)
public interface EnchantmentsPredicateAccessor {
    @Invoker("enchantments")
    List<EnchantmentPredicate> alphabetSpeedrun$getEnchantments();
}
