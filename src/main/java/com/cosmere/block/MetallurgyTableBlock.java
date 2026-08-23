package com.cosmere.block;

import com.cosmere.menu.MetallurgyMenu;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Where alloys are made.
 *
 * <p>Steel is not iron, and an Allomancer who burns the wrong mixture gets sick rather than
 * powerful, so every alloy in the mod is mixed here to an exact ratio rather than smelted
 * loosely in a furnace. Recipes live in {@link com.cosmere.crafting.AlloyRecipes}.
 *
 * <p>Like a crafting table it holds nothing between uses; the contents drop when the screen
 * closes.
 */
public class MetallurgyTableBlock extends Block {
    public static final MapCodec<MetallurgyTableBlock> CODEC = simpleCodec(MetallurgyTableBlock::new);

    private static final Component TITLE = Component.translatable("container.cosmere.metallurgy_table");

    public MetallurgyTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        player.openMenu(getMenuProvider(level, pos));
        return InteractionResult.CONSUME;
    }

    private static MenuProvider getMenuProvider(Level level, BlockPos pos) {
        return new SimpleMenuProvider(
                (id, inventory, player) -> new MetallurgyMenu(id, inventory, ContainerLevelAccess.create(level, pos)),
                TITLE);
    }
}
