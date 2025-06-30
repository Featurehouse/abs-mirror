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

package org.featurehouse.mcmod.speedrun.alphabeta.item.menu;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Environment(EnvType.CLIENT)
public class ClientItemListScreen extends AbstractContainerScreen<ItemListViewMenu> {
    private static final ResourceLocation TEXTURE_PTH = ResourceLocation.fromNamespaceAndPath("alphabet_speedrun", "textures/gui/view.png");
    private static final ResourceLocation SPRITE_BACK = ResourceLocation.fromNamespaceAndPath("alphabet_speedrun", "view/back.png");
    private static final ResourceLocation SPRITE_FORTH = ResourceLocation.fromNamespaceAndPath("alphabet_speedrun", "view/forth.png");
    private static final int COLOR_COMPLETED = 0x45c545, COLOR_NOT_COMPLETED = 0xc54545;

    public ClientItemListScreen(ItemListViewMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_PTH, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 176, 166);
        // Arrows
        if (menu.hasPrevPage()) context.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE_BACK, 18, 10, 0, 0, leftPos + 7, topPos + 149, 18, 10);
        if (menu.hasNextPage()) context.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE_FORTH, 18, 10, 0, 0, leftPos + 151, topPos + 149, 18, 10);
        // Coloring
        for (int k = 0; k < 63; k++) {
            final Boolean slotCompleted = menu.isSlotCompleted(k);
            if (slotCompleted == null) break;
            final int posX = leftPos + 8 + 18 * (k % 9);
            final int posY = topPos + 19 + 18 * (k / 9);

            if (!slotCompleted) {
                context.fill(posX, posY, posX + 16, posY + 16, COLOR_NOT_COMPLETED);
            } else {
                context.fill(posX, posY, posX + 16, posY + 16, COLOR_COMPLETED);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (menu.hasPrevPage() && (leftPos + 7) <= mouseX && mouseX <= (leftPos + 25) &&
                (topPos + 149) <= mouseY && mouseY <= (topPos + 159)) {
            //handler.prevPage();
            this.requestTurnPage(menu.getPage() - 1);
            return true;
        } else if (menu.hasNextPage() && (leftPos + 151) <= mouseX && mouseX <= (leftPos + 169) &&
                (topPos + 149) <= mouseY && mouseY <= (topPos + 159)) {
            this.requestTurnPage(menu.getPage() + 1);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void requestTurnPage(int target) {
        Objects.requireNonNull(Objects.requireNonNull(this.minecraft).gameMode).handleInventoryButtonClick(menu.containerId, target);
    }

    @Override
    public void render(@NotNull GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        this.renderTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
        // Don't draw inventory title
        context.drawString(font, this.title, this.titleLabelX, this.titleLabelY, 0xff404040, false);
    }
}
