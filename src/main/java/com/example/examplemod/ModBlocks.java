package com.example.examplemod;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Holds all Block registrations. No BlockEntity is used.
 */
public class ModBlocks {
    public static final DeferredRegister<Block> REGISTRY =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ExampleMod.MODID);

    /**
     * The “empty” Mob Trap block. When placed, ExampleMod will begin tracking it.
     * If a mob stays within 2 blocks for 60 ticks, the block disappears and
     * drops a filled‐trap item containing that mob’s NBT.
     */
    public static final RegistryObject<Block> MOB_TRAP =
            REGISTRY.register("mob_trap",
                    () -> new MobTrapBlock(Properties
                            .of()             // no Material parameter under official
                            .strength(0.2f)
                            .instabreak()
                            .noOcclusion()
                    )
            );

    public static void register(IEventBus modEventBus) {
        REGISTRY.register(modEventBus);
    }
}
