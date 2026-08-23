package com.cosmere.event;

import com.cosmere.Cosmere;
import com.cosmere.InvestitureData;
import com.cosmere.allomancy.AllomanticActions;
import com.cosmere.allomancy.AllomancyTicker;
import com.cosmere.feruchemy.FeruchemyTicker;
import com.cosmere.hemalurgy.HemalurgyTransfer;
import com.cosmere.hemalurgy.KolossForm;
import com.cosmere.item.CoinItem;
import com.cosmere.item.DaggerItem;
import com.cosmere.metal.Metal;
import com.cosmere.registry.ModAttachments;
import com.cosmere.registry.ModItems;
import com.cosmere.util.Investiture;
import com.cosmere.world.MistManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;

/**
 * The game-bus hooks: everything that has to happen on somebody else's schedule.
 *
 * <p>The per-tick entry point for all three arts is here, as is the combat handling that makes
 * pewter, atium and daggers behave the way their descriptions claim.
 */
@EventBusSubscriber(modid = Cosmere.MODID)
public final class ModEvents {
    /** Fraction of incoming damage a Thug actually feels at the time. The rest is deferred. */
    private static final float PEWTER_DEFERRED_FRACTION = 0.75F;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        AllomancyTicker.tick(player);
        FeruchemyTicker.tick(player);
        if (player.level() instanceof ServerLevel level) {
            MistManager.tick(level, player);
        }
    }

    /**
     * Pewter defers damage, atium avoids it outright, and a dagger in the back is worth far
     * more than a dagger in the front.
     */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        InvestitureData data = Investiture.of(victim);

        // Atium: the Allomancer sees the blow a second before it lands, so it never lands.
        if (data.isBurning(Metal.ATIUM) && event.getSource().getEntity() instanceof LivingEntity) {
            event.setCanceled(true);
            return;
        }

        applyBackstab(event);

        if (data.isBurning(Metal.PEWTER)) {
            float deferred = event.getAmount() * PEWTER_DEFERRED_FRACTION;
            data.addPewterDebt(deferred);
            event.setAmount(event.getAmount() - deferred);
        }
    }

    /** A dagger driven into someone who is not looking at you does far more than a fair fight would. */
    private static void applyBackstab(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        ItemStack weapon = attacker.getMainHandItem();
        if (!DaggerItem.isDagger(weapon)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        Vec3 victimLook = victim.getLookAngle().normalize();
        Vec3 toAttacker = attacker.position().subtract(victim.position()).normalize();
        if (victimLook.dot(toAttacker) < -0.2D) {
            event.setAmount(event.getAmount() * DaggerItem.BACKSTAB_MULTIPLIER);
        }
    }

    /**
     * A spiked body comes apart into the spikes that were holding it together.
     *
     * <p>Players are exempt: their spikes are part of a spiritweb that survives death along
     * with the powers those spikes grant. Losing forty-seven spikes to one creeper would make
     * Hemalurgy unplayable.
     */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) {
            return;
        }
        InvestitureData data = Investiture.of(entity);
        if (data.hemalurgy().count() == 0) {
            return;
        }
        for (ItemStack drop : HemalurgyTransfer.spikeDrops(data)) {
            event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(
                    entity.level(), entity.getX(), entity.getY(), entity.getZ(), drop));
        }
        data.hemalurgy().clear();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KolossForm.refresh(player);
            Investiture.sync(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        AllomanticActions.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KolossForm.refresh(player);
            Investiture.sync(player);
        }
    }

    /**
     * Powers ride the spiritweb, so they survive death. The attachment is copied for us; this
     * carries the koloss body across too, since that lives on a separate attachment.
     */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player clone = event.getEntity();
        if (original.getData(ModAttachments.KOLOSS_FORM.get())) {
            clone.setData(ModAttachments.KOLOSS_FORM.get(), Boolean.TRUE);
        }
        if (clone instanceof ServerPlayer player) {
            KolossForm.refresh(player);
            Investiture.sync(player);
        }
    }

    /** Scadrial runs on boxings and clips, so villagers take them. */
    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        event.getTrades().values().forEach(trades -> trades.add((entity, random) -> new MerchantOffer(
                new net.minecraft.world.item.trading.ItemCost(ModItems.BOXING.get(), 4),
                new ItemStack(net.minecraft.world.item.Items.EMERALD, 3),
                8, 2, 0.05F)));
    }

    @SubscribeEvent
    public static void onWandererTrades(WandererTradesEvent event) {
        event.getGenericTrades().add((entity, random) -> new MerchantOffer(
                new net.minecraft.world.item.trading.ItemCost(net.minecraft.world.item.Items.EMERALD, 1),
                new ItemStack(ModItems.CLIP.get(), 6),
                12, 1, 0.05F));
    }

    /** Coins are worth what {@link CoinItem#value()} says; nothing else needs to know how. */
    public static int coinValue(ItemStack stack) {
        return stack.getItem() instanceof CoinItem coin ? coin.value() * stack.getCount() : 0;
    }

    /** Keeps a metal item entity from despawning while an Allomancer is still using it as an anchor. */
    @SubscribeEvent
    public static void onEntityJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof net.minecraft.world.entity.item.ItemEntity item
                && com.cosmere.allomancy.MetalScanner.isMovableMetal(item.getItem())) {
            item.setExtendedLifetime();
        }
    }

    private ModEvents() {
    }
}
