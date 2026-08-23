package com.cosmere.registry;

import java.util.function.Supplier;

import com.cosmere.Cosmere;
import com.cosmere.menu.MetallurgyMenu;
import com.cosmere.menu.SpikeJarMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Container menu types. */
public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Cosmere.MODID);

    public static final Supplier<MenuType<MetallurgyMenu>> METALLURGY = MENUS.register("metallurgy_table",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new MetallurgyMenu(id, inventory)));

    public static final Supplier<MenuType<SpikeJarMenu>> SPIKE_JAR = MENUS.register("spike_jar",
            () -> IMenuTypeExtension.create(SpikeJarMenu::new));

    private ModMenus() {
    }
}
