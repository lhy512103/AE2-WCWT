package com.lhy.wcwt.compat;

import appeng.api.networking.IGrid;
import appeng.helpers.patternprovider.PatternContainer;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class NeoEcoApiCompatTest {
    @Test
    void absentEcoDoesNotLinkItsApiOrBlockOtherUploadSystems() throws Exception {
        String target = NeoEcoApiCompat.class.getName();
        ClassLoader isolated = new ClassLoader(getClass().getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("cn.dancingsnow.neoecoae.")) {
                    throw new ClassNotFoundException("ECO is deliberately absent: " + name);
                }
                if (!name.equals(target)) {
                    return super.loadClass(name, resolve);
                }
                var loaded = findLoadedClass(name);
                if (loaded == null) {
                    try (var input = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                        if (input == null) throw new ClassNotFoundException(name);
                        byte[] bytes = input.readAllBytes();
                        loaded = defineClass(name, bytes, 0, bytes.length);
                    } catch (IOException exception) {
                        throw new ClassNotFoundException(name, exception);
                    }
                }
                if (resolve) resolveClass(loaded);
                return loaded;
            }
        };
        Class<?> compat = Class.forName(target, true, isolated);
        assertFalse((boolean) compat.getMethod("isAvailable").invoke(null));
        assertFalse((boolean) compat.getMethod("isEcoPatternProvider", PatternContainer.class)
                .invoke(null, new Object[]{null}));
        assertNull(compat.getMethod("patternStorageSystem", IGrid.class).invoke(null, new Object[]{null}));
        assertFalse((boolean) compat.getMethod("uploadPatternToEcoStorage", IGrid.class, ItemStack.class)
                .invoke(null, null, null));
    }
}
