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

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.featurehouse.mcmod.speedrun.alphabeta.item.menu.ClientItemListScreen;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class ClientItemSpeedrunEvents {
    public static final KeyBinding VIEW_CURRENT_KEY = new KeyBinding("key.speedrun.alphabet.view",
            GLFW.GLFW_KEY_B, KeyBinding.MISC_CATEGORY);

    public static void init() {
        MenuRegistry.registerScreenFactory(ItemSpeedrunEvents.MENU_TYPE_R.get(), ClientItemListScreen::new);
        KeyMappingRegistry.register(VIEW_CURRENT_KEY);
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (VIEW_CURRENT_KEY.wasPressed()) {
                NetworkManager.sendToServer(new Identifier("alphabet_speedrun", "item_list"),
                        new PacketByteBuf(Unpooled.EMPTY_BUFFER));
            }
        });
    }
}
