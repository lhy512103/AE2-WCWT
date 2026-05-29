package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.RegistryAccess;

/**
 * 1.21 引入的缓冲区类型在 1.20.1 中不存在，这里保留最小兼容外形，
 * 让现有编解码逻辑继续基于 FriendlyByteBuf 工作。
 */
public class RegistryFriendlyByteBuf extends FriendlyByteBuf {
    private final RegistryAccess registryAccess;

    public RegistryFriendlyByteBuf(ByteBuf source) {
        this(source, null);
    }

    public RegistryFriendlyByteBuf(ByteBuf source, RegistryAccess registryAccess) {
        super(source);
        this.registryAccess = registryAccess;
    }

    public RegistryAccess registryAccess() {
        return this.registryAccess;
    }
}
