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
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
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
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE_PTH, x, y, 0, 0, 176, 166);
        // Arrows
        if (handler.hasPrevPage()) context.drawTexture(TEXTURE_PTH, x + 7, y + 149, 176, 0, 18, 10);
        if (handler.hasNextPage()) context.drawTexture(TEXTURE_PTH, x + 151, y + 149, 176, 10, 18, 10);
        // Coloring
        for (int k = 0; k < 63; k++) {
            final Boolean slotCompleted = handler.isSlotCompleted(k);
            if (slotCompleted == null) break;
            if (!slotCompleted) {
                context.drawTexture(TEXTURE_PTH, x + 8 + 18 * (k % 9), y + 19 + 18 * (k / 9), 0, 166, 16, 16);
            } else {
                //LOGGER.debug("SlotCompleted: {}", k);
                context.drawTexture(TEXTURE_PTH, x + 8 + 18 * (k % 9), y + 19 + 18 * (k / 9), 16, 166, 16, 16);
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Don't draw inventory title
        context.drawText(textRenderer, this.title, this.titleX, this.titleY, 4210752, false);
    }
}
