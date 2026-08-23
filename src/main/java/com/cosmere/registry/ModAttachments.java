package com.cosmere.registry;

import java.util.function.Supplier;

import com.cosmere.Cosmere;
import com.cosmere.InvestitureData;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Per-entity persistent data. Everything about a player's Investiture rides on one
 * attachment; see {@link InvestitureData} for why.
 */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Cosmere.MODID);

    /**
     * Survives death: powers are part of the spiritweb, not the body. Metal reserves survive
     * too, which is generous, but losing a stomach full of atium to a creeper is not fun.
     */
    public static final Supplier<AttachmentType<InvestitureData>> INVESTITURE =
            ATTACHMENT_TYPES.register("investiture", () -> AttachmentType
                    .builder(InvestitureData::new)
                    .serialize(InvestitureData.CODEC)
                    .copyOnDeath()
                    .build());

    /**
     * Set while a player is wearing a koloss body. Kept apart from {@link InvestitureData}
     * because it is a state of the body, not of the spiritweb, and does not survive death.
     */
    public static final Supplier<AttachmentType<Boolean>> KOLOSS_FORM =
            ATTACHMENT_TYPES.register("koloss_form", () -> AttachmentType
                    .builder(() -> Boolean.FALSE)
                    .serialize(com.mojang.serialization.Codec.BOOL)
                    .build());

    private ModAttachments() {
    }
}
