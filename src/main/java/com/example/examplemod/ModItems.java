package com.example.examplemod;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Holds all Item registrations (BlockItems + standalone items).
 *
 * We have removed creative‐tab references here so compilation cannot fail
 * if the tab constant has moved. You can add .tab(...) back later if needed.
 */
public class ModItems {
    public static final DeferredRegister<Item> REGISTRY =
            DeferredRegister.create(ForgeRegistries.ITEMS, ExampleMod.MODID);

    /** The BlockItem so players can pick/place the empty trap. */
    public static final RegistryObject<Item> MOB_TRAP_ITEM =
            REGISTRY.register("mob_trap",
                    () -> new BlockItem(
                            ModBlocks.MOB_TRAP.get(),
                            new Item.Properties() // no .tab(...) here
                    )
            );

    /**
     * The “filled” trap item. When a trap captures a mob, we drop one of these
     * with the mob’s full NBT stored inside.
     */
    public static final RegistryObject<Item> FILLED_MOB_TRAP_ITEM =
            REGISTRY.register("filled_mob_trap",
                    () -> new Item(new Item.Properties()) // no .tab(...) here
            );

    public static void register(IEventBus modEventBus) {
        REGISTRY.register(modEventBus);
    }
}
