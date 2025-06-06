package com.example.examplemod;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main mod class: registers blocks/items, then listens on the Forge EVENT_BUS
 * for server ticks and block-place/break events to implement the mob-trap logic.
 */
@Mod(ExampleMod.MODID)
public class ExampleMod {
    public static final String MODID = "examplemod";
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * For each loaded Level, keep a map: BlockPos → ticksInProximity.
     * When a trap is placed, insert pos→0. Each server tick, if a mob is in range,
     * increment that trap’s counter. At 60 ticks, perform the capture.
     */
    private static final Map<Level, Map<BlockPos, Integer>> TRAP_TIMER = new HashMap<>();

    public ExampleMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);

        // Register this class to the FORGE event bus so we receive tick/place/break events:
        MinecraftForge.EVENT_BUS.register(this);
    }

    // ───────── Server‐Tick Handler ───────────────────────────────────────────────
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onServerTick(ServerTickEvent event) {
        // Only run at END of each server tick
        if (event.phase != ServerTickEvent.Phase.END) return;

        for (Level raw : TRAP_TIMER.keySet()) {
            if (!(raw instanceof ServerLevel level)) continue;
            Map<BlockPos, Integer> timerMap = TRAP_TIMER.get(level);

            // Iterate over a copy of keySet() to avoid concurrent modification
            for (BlockPos pos : List.copyOf(timerMap.keySet())) {
                // 1) Ensure the block at pos is still the MobTrap
                var state = level.getBlockState(pos);
                if (state.getBlock() != ModBlocks.MOB_TRAP.get()) {
                    timerMap.remove(pos);
                    continue;
                }

                // 2) Build a 2‐block radius AABB around pos
                AABB box = new AABB(pos).inflate(2.0);

                // 3) Find all LivingEntity in box, excluding Players
                List<LivingEntity> nearby = level.getEntitiesOfClass(
                        LivingEntity.class,
                        box,
                        e -> !(e instanceof Player)
                );

                if (!nearby.isEmpty()) {
                    int prev = timerMap.getOrDefault(pos, 0);
                    int now = prev + 1;

                    if (now >= 60) {
                        // --- CAPTURE! ---
                        LivingEntity target = nearby.get(0);

                        // a) Get registry name (e.g. "minecraft:zombie")
                        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
                        if (key != null) {
                            // b) Save full NBT
                            CompoundTag mobTag = new CompoundTag();
                            target.save(mobTag);

                            // c) Remove the mob from world
                            target.remove(Entity.RemovalReason.DISCARDED);

                            // d) Remove trap block from world
                            level.removeBlock(pos, false);

                            // e) Create a “filled trap” ItemStack with NBT
                            ItemStack filled = new ItemStack(ModItems.FILLED_MOB_TRAP_ITEM.get());

                            // ─────────────────────────────────────────────────────────
                            // OLD: This was where CustomData.set(...) was originally called
                            //     (which led to version mismatches). Keeping it here for reference:
                            // CustomData.set(DataComponents.CUSTOM_DATA, filled, mobTag);
                            // ─────────────────────────────────────────────────────────

                            // f) Drop it at pos
                            level.addFreshEntity(new ItemEntity(
                                    level,
                                    pos.getX() + 0.5,
                                    pos.getY() + 0.5,
                                    pos.getZ() + 0.5,
                                    filled
                            ));

                            // g) Spawn smoke particles + play sound
                            level.sendParticles(
                                    ParticleTypes.SMOKE,
                                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                    20, 0.3, 0.3, 0.3, 0.02
                            );
                            level.playSound(
                                    null,
                                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                    SoundEvents.TRIDENT_HIT,
                                    SoundSource.BLOCKS,
                                    1.0f, 1.0f
                            );
                        }

                        // Done with this position
                        timerMap.remove(pos);
                    } else {
                        timerMap.put(pos, now);
                    }
                } else {
                    // No mobs in range → reset/stop tracking
                    timerMap.remove(pos);
                }
            }
        }
    }

    // ───────── Block‐Place / Block‐Break Events ──────────────────────────────────
    @SubscribeEvent
    public void onBlockPlace(EntityPlaceEvent event) {
        var raw = event.getLevel();
        if (!(raw instanceof Level level) || level.isClientSide) return;

        if (event.getPlacedBlock().getBlock() == ModBlocks.MOB_TRAP.get()) {
            TRAP_TIMER.computeIfAbsent(level, l -> new HashMap<>()).put(event.getPos(), 0);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BreakEvent event) {
        var raw = event.getLevel();
        if (!(raw instanceof Level level) || level.isClientSide) return;

        if (event.getState().getBlock() == ModBlocks.MOB_TRAP.get()) {
            Map<BlockPos, Integer> map = TRAP_TIMER.get(level);
            if (map != null) {
                map.remove(event.getPos());
            }
        }
    }
}

