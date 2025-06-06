package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A simple “MobTrapBlock” with a BooleanProperty HAS_MOB.
 * The actual capture logic lives in ExampleMod’s server‐tick handler.
 */
public class MobTrapBlock extends Block {
    public static final BooleanProperty HAS_MOB = BooleanProperty.create("has_mob");

    public MobTrapBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HAS_MOB, Boolean.FALSE));
    }

    // We removed the @Override annotation here in case the signature did not match exactly.
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_MOB);
    }

    // We removed @Override annotation here as well, to avoid “method does not override” errors.
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        // Toggle HAS_MOB on right-click for visual feedback (not used in capture logic).
        if (!level.isClientSide) {
            boolean current = state.getValue(HAS_MOB);
            level.setBlock(pos, state.setValue(HAS_MOB, !current), 3);
        }
        return InteractionResult.SUCCESS;
    }
}
