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

package org.featurehouse.mcmod.speedrun.alphabeta.mixin;

import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.ItemStack;
import org.featurehouse.mcmod.speedrun.alphabeta.item.FireworkElytraUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FireworkRocketItem.class)
public class FireworkRocketMixin {
    @Redirect(method = {"useOnBlock", "use"}, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;decrement(I)V"
    ))
    private void handleDecrement(ItemStack instance, int amount) {
        FireworkElytraUtils.handleFireworkDecrement(instance, amount);
    }
}
