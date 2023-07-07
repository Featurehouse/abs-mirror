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

package org.featurehouse.mcmod.speedrun.alphabeta.util.hooks.legacy.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.obfuscate.DontObfuscate;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

@DontObfuscate
public class LegacyHooksMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("LegacyHooksMixinPlugin/Fabric");
    private static final SemanticVersion V1193S;
    static {
        SemanticVersion v;
        try {
            v = SemanticVersion.parse("1.19.3-");
        } catch (VersionParsingException e) {
            LOGGER.warn("Invalid Fabric implementation: cannot parse '1.19.3-' as SemanticVersion", e);
            v = null;
        }
        V1193S = v;
    }

    private static boolean isLegacy() {
        if (V1193S == null) return false;
        final Version version = FabricLoader.getInstance().getModContainer("minecraft")
                .orElseThrow()
                .getMetadata()
                .getVersion();
        if (!(version instanceof SemanticVersion)) return false;
        return version.compareTo(V1193S) < 0;
    }

    private boolean isLegacy;
    @Override
    public void onLoad(String mixinPackage) {
        isLegacy = isLegacy();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return isLegacy;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if ("org.featurehouse.mcmod.speedrun.alphabeta.util.hooks.legacy.fabric.stub.StubLegacyHooks".equals(mixinClassName)) {
            new ASMLegacyHooks(targetClass).apply(getMapping());
        }
    }

    private static String[] getMapping() {
        return new String[]{"net/minecraft/class_2378", "net/minecraft/class_1792", "Lnet/minecraft/class_2348;", "Lnet/minecraft/class_5321;", "Lnet/minecraft/class_2378;", "(Lnet/minecraft/class_6862;)Lnet/minecraft/class_6885$class_6888;", "(Ljava/lang/Object;)Lnet/minecraft/class_2960;", "(Lnet/minecraft/class_1792;)Lnet/minecraft/class_2960;", "(Lnet/minecraft/class_2960;)Ljava/lang/Object;", "(Lnet/minecraft/class_2960;)Lnet/minecraft/class_1792;", "(Lnet/minecraft/class_2960;)Ljava/util/Optional;", "field_11142", "field_25108", "field_11160", "field_25083", "method_40260", "method_10221", "method_10223", "method_17966"};
    }
}
