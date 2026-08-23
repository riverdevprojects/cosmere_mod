package com.cosmere.hemalurgy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.cosmere.InvestitureData;
import com.cosmere.item.SpikeData;
import com.cosmere.item.SpikeItem;
import com.cosmere.metal.Metal;
import com.cosmere.metal.MetalCategory;
import com.cosmere.registry.ModAttachments;
import com.cosmere.util.Investiture;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The act itself: taking an attribute out of one spiritweb and driving it into another.
 *
 * <p>Hemalurgy always leaks. {@link #TRANSFER_EFFICIENCY} is how much of what was taken
 * actually arrives, and the rest is simply gone -- which is why nobody builds an Inquisitor
 * out of one spike.
 */
public final class HemalurgyTransfer {
    /** Fraction of a stolen attribute that survives the spike. */
    public static final float TRANSFER_EFFICIENCY = 0.7F;

    /**
     * Drives a blank spike through a victim led to the table, killing it and charging the
     * spike with whatever the spike's metal is able to take.
     *
     * @return true when the spike came away charged
     */
    public static boolean harvest(ServerPlayer user, LivingEntity victim, ItemStack spikeStack) {
        if (!(spikeStack.getItem() instanceof SpikeItem spike)) {
            return false;
        }
        if (SpikeItem.dataOf(spikeStack).isCharged()) {
            user.displayClientMessage(Component.translatable("cosmere.message.spike_already_charged")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }

        StolenAttribute stolen = takeFrom(victim, spike.metal());
        if (stolen == null) {
            user.displayClientMessage(Component.translatable("cosmere.message.nothing_to_steal",
                    victim.getDisplayName()).withStyle(ChatFormatting.RED), true);
            return false;
        }

        ItemStack charged = spikeStack.split(1);
        SpikeItem.setData(charged, new SpikeData(Optional.of(stolen), 0));
        if (!user.getInventory().add(charged)) {
            user.drop(charged, false);
        }

        ServerLevel level = user.serverLevel();
        Vec3 at = victim.position();
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR,
                at.x, at.y + 1.0D, at.z, 30, 0.3D, 0.5D, 0.3D, 0.1D);
        level.playSound(null, victim.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.BLOCKS, 1.0F, 0.6F);

        // The victim's power is gone whether or not the spike caught all of it.
        Investiture.of(victim).revokeAllPowers();
        victim.hurt(user.damageSources().playerAttack(user), Float.MAX_VALUE);
        return true;
    }

    /** What a spike of {@code spikeMetal} can take out of {@code victim}. Null when there is nothing to take. */
    @Nullable
    private static StolenAttribute takeFrom(LivingEntity victim, Metal spikeMetal) {
        StolenAttribute.Kind kind = StolenAttribute.kindStolenBy(spikeMetal);
        InvestitureData victimData = Investiture.of(victim);

        return switch (kind) {
            case ALLOMANTIC_POWER -> {
                Metal taken = pickPower(new ArrayList<>(victimData.allomanticPowers()),
                        StolenAttribute.quadrantStolenBy(spikeMetal), true);
                yield taken == null ? null : new StolenAttribute(kind, Optional.of(taken), TRANSFER_EFFICIENCY);
            }
            case FERUCHEMIC_POWER -> {
                Metal taken = pickPower(new ArrayList<>(victimData.feruchemicPowers()),
                        StolenAttribute.quadrantStolenBy(spikeMetal), false);
                yield taken == null ? null : new StolenAttribute(kind, Optional.of(taken), TRANSFER_EFFICIENCY);
            }
            case EVERYTHING -> new StolenAttribute(kind, Optional.empty(), TRANSFER_EFFICIENCY);
            case VOID -> new StolenAttribute(kind, Optional.empty(), 1.0F);
            // The rest steal ordinary bodily and mental traits, which anything alive has.
            default -> new StolenAttribute(kind, Optional.empty(),
                    TRANSFER_EFFICIENCY * (float) Math.max(1.0D, victim.getMaxHealth() / 20.0D));
        };
    }

    @Nullable
    private static Metal pickPower(List<Metal> available, @Nullable MetalCategory quadrant, boolean allomantic) {
        for (Metal metal : available) {
            MetalCategory category = allomantic ? metal.allomanticCategory() : metal.feruchemicCategory();
            if (quadrant == null || category == quadrant) {
                return metal;
            }
        }
        return null;
    }

    /**
     * Drives a charged spike into the player standing at the table. The spike finds the first
     * place on the body that will take it; the Jar of Spikes screen exists for when you care
     * which.
     */
    public static boolean implantIntoSelf(ServerPlayer player, ItemStack spikeStack) {
        if (!(spikeStack.getItem() instanceof SpikeItem spike)) {
            return false;
        }
        SpikeData data = SpikeItem.dataOf(spikeStack);
        if (!data.isCharged()) {
            player.displayClientMessage(Component.translatable("cosmere.message.spike_blank_no_victim")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        SpikeSlot slot = firstOpenSlot(Investiture.of(player), spike.metal());
        if (slot == null) {
            player.displayClientMessage(Component.translatable("cosmere.message.no_room_for_spike")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        return implant(player, spikeStack, slot);
    }

    /** Drives a charged spike into a specific place on the body. */
    public static boolean implant(ServerPlayer player, ItemStack spikeStack, SpikeSlot slot) {
        if (!(spikeStack.getItem() instanceof SpikeItem spike)) {
            return false;
        }
        SpikeData data = SpikeItem.dataOf(spikeStack);
        InvestitureData investiture = Investiture.of(player);

        if (investiture.isSpirituallyShielded()) {
            player.displayClientMessage(Component.translatable("cosmere.message.spiritually_shielded")
                    .withStyle(ChatFormatting.AQUA), true);
            return false;
        }
        if (!investiture.hemalurgy().canAccept(spike.metal(), slot)) {
            return false;
        }

        investiture.hemalurgy().add(new PlacedSpike(spike.metal(), slot, data.charge()));
        spikeStack.shrink(1);

        // Driving a spike through yourself hurts, and the more of them you carry the worse it goes.
        float pain = 4.0F + investiture.hemalurgy().count() * 0.5F;
        player.hurt(player.damageSources().magic(), pain);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT,
                SoundSource.PLAYERS, 1.0F, 0.5F);

        if (investiture.hemalurgy().isOverSpiked()) {
            // A spiritweb this shredded cannot hold a Connection to anything, bed included.
            player.setRespawnPosition(player.level().dimension(), null, 0.0F, false, false);
            player.hurt(player.damageSources().magic(), Float.MAX_VALUE);
            player.displayClientMessage(Component.translatable("cosmere.message.spiritweb_torn")
                    .withStyle(ChatFormatting.DARK_RED), false);
        } else {
            player.displayClientMessage(Component.translatable("cosmere.message.spike_placed",
                    Component.translatable(slot.translationKey())).withStyle(ChatFormatting.DARK_RED), true);
        }

        Investiture.sync(player);
        return true;
    }

    @Nullable
    public static SpikeSlot firstOpenSlot(InvestitureData data, Metal metal) {
        for (SpikeSlot slot : SpikeSlot.values()) {
            if (data.hemalurgy().canAccept(metal, slot)) {
                return slot;
            }
        }
        return null;
    }

    /**
     * Lays a koloss skin over a body already carrying four iron spikes through the ribs.
     * There is no way back from this.
     */
    public static boolean applyKolossSkin(ServerPlayer player, ItemStack skin) {
        InvestitureData data = Investiture.of(player);
        int ironSpikes = data.hemalurgy().countOf(Metal.IRON);
        if (ironSpikes < 4) {
            player.displayClientMessage(Component.translatable("cosmere.message.needs_iron_spikes", 4 - ironSpikes)
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (player.getData(ModAttachments.KOLOSS_FORM.get())) {
            return false;
        }
        player.setData(ModAttachments.KOLOSS_FORM.get(), Boolean.TRUE);
        skin.shrink(1);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR,
                SoundSource.PLAYERS, 1.5F, 0.6F);
        player.displayClientMessage(Component.translatable("cosmere.message.became_koloss")
                .withStyle(ChatFormatting.BLUE), false);
        KolossForm.apply(player);
        return true;
    }

    /** Loot dropped when a spiked body finally comes apart. */
    public static List<ItemStack> spikeDrops(InvestitureData data) {
        List<ItemStack> drops = new ArrayList<>();
        for (PlacedSpike spike : data.hemalurgy().spikes()) {
            ItemStack stack = new ItemStack(com.cosmere.registry.ModItems.SPIKES.get(spike.metal()).get());
            SpikeItem.setData(stack, new SpikeData(spike.charge(), 0));
            drops.add(stack);
        }
        return drops;
    }

    private HemalurgyTransfer() {
    }
}
