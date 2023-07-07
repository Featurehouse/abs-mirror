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

import org.objectweb.asm.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Consumer;

import static org.objectweb.asm.Opcodes.*;

record ASMLegacyHooks(ClassNode cn) {
    private static final Logger LOGGER = LoggerFactory.getLogger("ASMLegacyHooks");
    private void overwriteMethod(String name, String desc, Consumer<? super InsnList> consumer) {
        cn.methods.stream()
                .filter(m -> Objects.equals(m.name, name) && Objects.equals(m.desc, desc))
                .findAny()
                .ifPresentOrElse(m -> {
                    InsnList list = new InsnList();
                    m.instructions = list;
                    consumer.accept(list);
                }, () -> LOGGER.warn("Unable to find method {}:{} in {}", name, desc, cn.name));
    }

    void apply(String[] s) {
        overwriteMethod("itemTagHolders", s[5], l -> {
            l.add(new FieldInsnNode(GETSTATIC, s[0], s[11], s[2]));
            l.add(new VarInsnNode(ALOAD, 0));
            l.add(new MethodInsnNode(INVOKEVIRTUAL, s[0], s[15], s[5]));
            l.add(new InsnNode(ARETURN));
        });
        overwriteMethod("itemId", s[7], l -> {
            l.add(new FieldInsnNode(GETSTATIC, s[0], s[11], s[2]));
            l.add(new VarInsnNode(ALOAD, 0));
            l.add(new MethodInsnNode(INVOKEVIRTUAL, s[0], s[16], s[6]));
            l.add(new InsnNode(ARETURN));
        });
        overwriteMethod("getItem", s[9], l -> {
            l.add(new FieldInsnNode(GETSTATIC, s[0], s[11], s[2]));
            l.add(new VarInsnNode(ALOAD, 0));
            l.add(new MethodInsnNode(INVOKEVIRTUAL, s[0], s[17], s[8]));
            l.add(new TypeInsnNode(CHECKCAST, s[1]));
            l.add(new InsnNode(ARETURN));
        });
        overwriteMethod("getOptionalItem", s[10], l -> {
            l.add(new FieldInsnNode(GETSTATIC, s[0], s[11], s[2]));
            l.add(new VarInsnNode(ALOAD, 0));
            l.add(new MethodInsnNode(INVOKEVIRTUAL, s[0], s[18], s[10]));
            l.add(new InsnNode(ARETURN));
        });
        overwriteMethod("getOptionalEnchantment", s[10], l -> {
            l.add(new FieldInsnNode(GETSTATIC, s[0], s[13], s[4]));
            l.add(new VarInsnNode(ALOAD, 0));
            l.add(new MethodInsnNode(INVOKEVIRTUAL, s[0], s[18], s[10]));
            l.add(new InsnNode(ARETURN));
        });
        overwriteMethod("itemKey", "()" + s[3], l -> {
            l.add(new FieldInsnNode(GETSTATIC, s[0], s[12], s[3]));
            l.add(new InsnNode(ARETURN));
        });
        overwriteMethod("menuKey", "()" + s[3], l -> {
            l.add(new FieldInsnNode(GETSTATIC, s[0], s[14], s[3]));
            l.add(new InsnNode(ARETURN));
        });
    }
}
