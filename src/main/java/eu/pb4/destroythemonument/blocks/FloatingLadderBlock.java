package eu.pb4.destroythemonument.blocks;

import eu.pb4.polymer.api.block.PolymerBlock;
import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;

public class FloatingLadderBlock extends LadderBlock implements PolymerBlock {
    public FloatingLadderBlock(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return true;
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.LADDER;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return Blocks.LADDER.getDefaultState().with(HorizontalFacingBlock.FACING, state.get(HorizontalFacingBlock.FACING));
    }
}
