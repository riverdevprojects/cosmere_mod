package com.cosmere.entity;

import com.cosmere.registry.ModEntities;
import com.cosmere.registry.ModItems;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * A coin, or any small piece of metal, moving far faster than a thrown one has any right to.
 *
 * <p>This is what a Steelpush on a held coin produces. Damage scales with how hard it was
 * Pushed, so a flared Push through a duralumin flashburn turns a copper clip into something
 * that goes through armour.
 */
public class CoinProjectileEntity extends ThrowableItemProjectile implements ItemSupplier {
    /** Damage per block-per-tick of speed at impact. */
    private static final float DAMAGE_PER_SPEED = 4.0F;
    private static final float MAX_DAMAGE = 30.0F;

    public CoinProjectileEntity(EntityType<? extends CoinProjectileEntity> type, Level level) {
        super(type, level);
    }

    public CoinProjectileEntity(Level level, LivingEntity shooter, ItemStack ammo) {
        super(ModEntities.COIN_PROJECTILE.get(), shooter, level);
        this.setItem(ammo.copyWithCount(1));
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.CLIP.get();
    }

    @Override
    protected double getDefaultGravity() {
        // A Pushed coin flies flat; the Allomancer is still holding it up.
        return 0.02D;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) {
            return;
        }
        float speed = (float) this.getDeltaMovement().length();
        float damage = Math.min(MAX_DAMAGE, speed * DAMAGE_PER_SPEED);
        result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), damage);
        dropAsItem();
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            dropAsItem();
            this.discard();
        }
    }

    /** The coin still exists after it lands, so a Coinshot can collect their ammunition. */
    private void dropAsItem() {
        ItemStack stack = this.getItem();
        if (!stack.isEmpty()) {
            this.spawnAtLocation(stack.copyWithCount(1));
        }
    }
}
