package me.jellysquid.mods.sodium.mixin.shapes.blockstate_cache;

import me.jellysquid.mods.lithium.common.util.collections.Object2BooleanCacheTable;
import net.minecraft.block.Block;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Block.class)
public class BlockMixin {
    private static final Object2BooleanCacheTable<VoxelShape> FULL_CUBE_CACHE = new Object2BooleanCacheTable<>(
            512,
            shape -> !VoxelShapes.joinIsNotEmpty(VoxelShapes.block(), shape, IBooleanFunction.NOT_SAME)
    );

    /**
     * @reason Use a faster cache implementation
     * @author gegy1000
     */
    @Overwrite
    public static boolean isShapeFullBlock(VoxelShape shape) {
        return FULL_CUBE_CACHE.get(shape);
    }
}
