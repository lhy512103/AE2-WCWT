package com.lhy.wcwt.util;

import net.minecraft.resources.ResourceLocation;

public final class ResourceLocationCompat {
    private ResourceLocationCompat() {
    }

    public static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
