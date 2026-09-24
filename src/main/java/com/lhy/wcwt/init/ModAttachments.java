package com.lhy.wcwt.init;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.helpers.WcwtToolkitPlayerState;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, WcwtMod.MOD_ID);

    /** Runtime toolkit state of one player object; the toolkit contents themselves persist in player data. */
    public static final Supplier<AttachmentType<WcwtToolkitPlayerState>> TOOLKIT_STATE = ATTACHMENT_TYPES.register(
            "toolkit_state", () -> AttachmentType.builder(WcwtToolkitPlayerState::new).build());

    private ModAttachments() {
    }
}
