package com.cosmere.feruchemy;

import java.util.ArrayList;
import java.util.List;

import com.cosmere.InvestitureData;
import com.cosmere.item.MetalmindData;
import com.cosmere.item.MetalmindItem;
import com.cosmere.metal.Metal;
import com.cosmere.util.Investiture;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * One server tick of every metalmind a player is carrying.
 *
 * <p>Only metalminds actually on the player count -- worn on the hand, in the hotbar, anywhere
 * in the inventory. Tapping is faster than storing by {@link #COMPRESSION}, which is the whole
 * bargain of Feruchemy: pay slowly, spend quickly.
 */
public final class FeruchemyTicker {
    /** Charge added per tick while storing. */
    public static final float STORE_RATE = 0.05F;
    /** Tapping spends this many times faster than storing fills, and hits that much harder. */
    public static final float COMPRESSION = 4.0F;

    public static void tick(ServerPlayer player) {
        InvestitureData data = Investiture.of(player);
        data.clearLoanedPowers();
        data.setInvestitureSuppressed(false);

        if (data.feruchemyModes().isEmpty()) {
            return;
        }

        List<ItemStack> metalminds = collectMetalminds(player);
        boolean identitySuppressed = data.isIdentitySuppressed();
        boolean changed = false;

        for (ItemStack stack : metalminds) {
            MetalmindItem item = (MetalmindItem) stack.getItem();
            Metal metal = item.metal();
            FeruchemyMode mode = data.feruchemyMode(metal);
            if (mode == FeruchemyMode.OFF || !data.canStore(metal)) {
                continue;
            }

            MetalmindData contents = item.dataOf(stack);
            if (mode == FeruchemyMode.STORING) {
                if (contents.isFull()) {
                    continue;
                }
                item.setData(stack, contents.store(STORE_RATE, player.getUUID(), identitySuppressed));
                FeruchemyEffects.applyStoring(player, data, metal, 0);
                changed = true;
            } else {
                if (contents.isEmpty() || !contents.canBeTappedBy(player.getUUID())) {
                    continue;
                }
                item.setData(stack, contents.tap(STORE_RATE * COMPRESSION));
                FeruchemyEffects.applyTapping(player, data, metal, 1);
                changed = true;
            }
        }

        if (changed && player.tickCount % 20 == 0) {
            Investiture.sync(player);
        }
    }

    /**
     * Every metalmind on the player, one per metal. Carrying two ironminds does not double the
     * effect -- the body can only pay into one at a time -- but it does mean the fuller one is
     * used first when tapping.
     */
    private static List<ItemStack> collectMetalminds(ServerPlayer player) {
        List<ItemStack> found = new ArrayList<>();
        java.util.EnumSet<Metal> seen = java.util.EnumSet.noneOf(Metal.class);
        for (ItemStack stack : player.getInventory().items) {
            addIfBest(stack, found, seen, player);
        }
        addIfBest(player.getOffhandItem(), found, seen, player);
        return found;
    }

    private static void addIfBest(ItemStack stack, List<ItemStack> found, java.util.EnumSet<Metal> seen, ServerPlayer player) {
        if (!(stack.getItem() instanceof MetalmindItem item)) {
            return;
        }
        if (!seen.add(item.metal())) {
            // Already have one of this metal; prefer whichever holds more.
            for (int i = 0; i < found.size(); i++) {
                ItemStack existing = found.get(i);
                MetalmindItem existingItem = (MetalmindItem) existing.getItem();
                if (existingItem.metal() == item.metal()
                        && item.dataOf(stack).charge() > existingItem.dataOf(existing).charge()) {
                    found.set(i, stack);
                    return;
                }
            }
            return;
        }
        found.add(stack);
    }

    private FeruchemyTicker() {
    }
}
