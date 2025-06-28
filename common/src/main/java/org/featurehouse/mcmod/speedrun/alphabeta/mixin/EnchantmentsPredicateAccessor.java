package org.featurehouse.mcmod.speedrun.alphabeta.mixin;

import net.minecraft.predicate.item.EnchantmentPredicate;
import net.minecraft.predicate.item.EnchantmentsPredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(EnchantmentsPredicate.class)
public interface EnchantmentsPredicateAccessor {
    @Invoker("getEnchantments")
    List<EnchantmentPredicate> alphabetSpeedrun$getEnchantments();
}
