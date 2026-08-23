package com.cosmere.block;

import java.util.List;

import com.cosmere.hemalurgy.HemalurgyTransfer;
import com.cosmere.item.KolossSkinItem;
import com.cosmere.item.SpikeItem;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A stone slab at waist height, with channels cut for the blood.
 *
 * <p>Hemalurgy needs a body to take from and a body to give to. Lead the victim here on a lead
 * -- it counts as being on the table once it is within {@link #VICTIM_RANGE} of the block and
 * still leashed to you -- then strike with a blank spike to take what it has. The spike comes
 * away charged and the victim does not get up.
 *
 * <p>Strike with an already-charged spike and no victim present and you drive it into
 * yourself, which is how a recipient actually gains the power. Lay a koloss skin on the table
 * instead, with four iron spikes already in your ribs, and you become something else entirely.
 */
public class HemalurgicTableBlock extends Block {
    public static final MapCodec<HemalurgicTableBlock> CODEC = simpleCodec(HemalurgicTableBlock::new);

    /** How far from the table a leashed victim counts as being on it. */
    public static final double VICTIM_RANGE = 3.0D;

    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);

    public HemalurgicTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.getItem() instanceof KolossSkinItem) {
            return HemalurgyTransfer.applyKolossSkin(serverPlayer, stack)
                    ? ItemInteractionResult.CONSUME
                    : ItemInteractionResult.FAIL;
        }

        if (SpikeItem.isSpike(stack)) {
            Mob victim = findLeashedVictim(level, pos, player);
            if (victim != null) {
                return HemalurgyTransfer.harvest(serverPlayer, victim, stack)
                        ? ItemInteractionResult.CONSUME
                        : ItemInteractionResult.FAIL;
            }
            return HemalurgyTransfer.implantIntoSelf(serverPlayer, stack)
                    ? ItemInteractionResult.CONSUME
                    : ItemInteractionResult.FAIL;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (!level.isClientSide) {
            Mob victim = findLeashedVictim(level, pos, player);
            player.displayClientMessage(victim == null
                    ? Component.translatable("cosmere.message.table_empty")
                    : Component.translatable("cosmere.message.table_ready", victim.getDisplayName()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** The mob this player has led to the table, if any. */
    private static Mob findLeashedVictim(Level level, BlockPos pos, Player player) {
        AABB area = new AABB(pos).inflate(VICTIM_RANGE);
        List<Mob> nearby = level.getEntitiesOfClass(Mob.class, area, mob -> mob.isLeashed() && mob.getLeashHolder() == player);
        return nearby.isEmpty() ? null : nearby.get(0);
    }
}
