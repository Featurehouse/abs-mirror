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

package org.featurehouse.mcmod.speedrun.alphabeta;

import com.mojang.logging.LogUtils;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.platform.Platform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.featurehouse.mcmod.speedrun.alphabeta.config.AlphabetSpeedrunConfigData;
import org.featurehouse.mcmod.speedrun.alphabeta.config.ItemRunDifficultyRuleFactory;
import org.featurehouse.mcmod.speedrun.alphabeta.item.ClientItemSpeedrunEvents;
import org.featurehouse.mcmod.speedrun.alphabeta.item.ItemSpeedrunEvents;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.DefaultItemSpeedrunDifficulty;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.ItemSpeedrunDifficulty;
import org.featurehouse.mcmod.speedrun.alphabeta.util.pack.BuiltinABSPackProvider;
import org.featurehouse.mcmod.speedrun.alphabeta.util.qj5.JsonReader;
import org.featurehouse.mcmod.speedrun.alphabeta.util.qj5.JsonWriter;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

public class AlphabetSpeedrunMod {
    public static void init() {
        ItemSpeedrunEvents.init();
        ItemSpeedrunEvents.MENU_REG.register();
        BuiltinABSPackProvider.init();
    }

    // In Forge, this should be invoked from common init event.
    // In Fabric, this should be invoked after a specified entrypoint.
    public static void initConfig() {
        // Register difficulties
        fireDifficultyRegistry().forEach(difficulty -> DefaultItemSpeedrunDifficulty.registerDifficulty(difficulty.getId(), difficulty));
        // Finalize "difficult difficulties" config
        ItemRunDifficultyRuleFactory.Impl.setInitialized();
        // Now read the config!
        AlphabetSpeedrunConfigData configData = AlphabetSpeedrunConfigData.getInstance();
        boolean update = !Files.exists(CONFIG_PATH);
        if (!update) {
            try (JsonReader reader = JsonReader.json5(CONFIG_PATH)) {
                update |= configData.readFromJson5(reader);
            } catch (IOException e) {
                LOGGER.error("Failed to read config from" + CONFIG_PATH, e);
            }
        }
        if (update) {
            try (JsonWriter writer = JsonWriter.json5(CONFIG_PATH)) {
                configData.writeToJson5(writer);
            } catch (IOException e) {
                LOGGER.error("Failed to write config to " + CONFIG_PATH, e);
            }
        }
    }

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path CONFIG_PATH = Platform.getConfigFolder().resolve("alphabet-speedrun-3.json5");

    @ExpectPlatform
    private static Collection<? extends ItemSpeedrunDifficulty> fireDifficultyRegistry() { throw new IncompatibleClassChangeError("Missing Implementation"); }

    @Environment(EnvType.CLIENT)
    public static void initClient() {
        ClientItemSpeedrunEvents.init();
    }
}
