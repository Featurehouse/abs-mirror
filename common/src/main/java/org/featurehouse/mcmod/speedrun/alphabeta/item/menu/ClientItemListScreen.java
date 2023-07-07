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

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Objects;

@Environment(EnvType.CLIENT)
public class ClientItemListScreen extends HandledScreen<ItemListViewMenu> {
    private static final Identifier TEXTURE_PTH = new Identifier("alphabet_speedrun", "textures/gui/view.png");

    public ClientItemListScreen(ItemListViewMenu handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE_PTH);
        this.drawTexture(matrices, x, y, 0, 0, 176, 166);
        // Arrows
        if (handler.hasPrevPage()) drawTexture(matrices, x + 7, y + 149, 176, 0, 18, 10);
        if (handler.hasNextPage()) drawTexture(matrices, x + 151, y + 149, 176, 10, 18, 10);
        // Coloring
        for (int k = 0; k < 63; k++) {
            final Boolean slotCompleted = handler.isSlotCompleted(k);
            if (slotCompleted == null) break;
            if (!slotCompleted) {
                drawTexture(matrices, x + 8 + 18 * (k % 9), y + 19 + 18 * (k / 9), 0, 166, 16, 16);
            } else {
                //LOGGER.debug("SlotCompleted: {}", k);
                drawTexture(matrices, x + 8 + 18 * (k % 9), y + 19 + 18 * (k / 9), 16, 166, 16, 16);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handler.hasPrevPage() && (x + 7) <= mouseX && mouseX <= (x + 25) &&
                (y + 149) <= mouseY && mouseY <= (y + 159)) {
            //handler.prevPage();
            this.requestTurnPage(handler.getPage() - 1);
            return true;
        } else if (handler.hasNextPage() && (x + 151) <= mouseX && mouseX <= (x + 169) &&
                (y + 149) <= mouseY && mouseY <= (y + 159)) {
            this.requestTurnPage(handler.getPage() + 1);
            //org.featurehouse.mcmod.speedrun.alphabeta.util.AlphaBetaDebug.log((logger) -> logger.info(java.util.Arrays.toString(java.util.stream.IntStream.range(0, handler.sync.listSize).mapToObj(handler.sync::getBit).toArray())));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void requestTurnPage(int target) {
        Objects.requireNonNull(Objects.requireNonNull(this.client).interactionManager).clickButton(handler.syncId, target);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
        // Don't draw inventory title
        this.textRenderer.draw(matrices, this.title, (float)this.titleX, (float)this.titleY, 4210752);
    }
}
