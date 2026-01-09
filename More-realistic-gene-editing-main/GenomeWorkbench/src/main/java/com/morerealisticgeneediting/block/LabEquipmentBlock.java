package com.morerealisticgeneediting.block;

import com.morerealisticgeneediting.equipment.EquipmentTier;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for all laboratory equipment blocks.
 * 
 * Features:
 * - Directional placement (faces player)
 * - Active state for running animations
 * - Standard interaction handling
 * - Customizable collision shape
 */
public class LabEquipmentBlock extends BlockWithEntity {
    
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");
    
    // Standard benchtop equipment shape (slightly smaller than full block)
    protected static final VoxelShape SHAPE = VoxelShapes.cuboid(0.0625, 0, 0.0625, 0.9375, 0.875, 0.9375);
    
    public LabEquipmentBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(ACTIVE, false));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }
    
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }
    
    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }
    
    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }
    
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, 
                              Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            NamedScreenHandlerFactory screenHandlerFactory = state.createScreenHandlerFactory(world, pos);
            if (screenHandlerFactory != null) {
                player.openHandledScreen(screenHandlerFactory);
            }
        }
        return ActionResult.SUCCESS;
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        // Override in subclasses to provide specific block entities
        return null;
    }
    
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof LabEquipmentBlockEntity labEntity) {
                labEntity.dropContents(world, pos);
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }
    
    /**
     * Set the active state of the equipment
     */
    public static void setActive(World world, BlockPos pos, BlockState state, boolean active) {
        world.setBlockState(pos, state.with(ACTIVE, active), Block.NOTIFY_ALL);
    }
    
    /**
     * Check if equipment is currently active/running
     */
    public static boolean isActive(BlockState state) {
        return state.get(ACTIVE);
    }
}
