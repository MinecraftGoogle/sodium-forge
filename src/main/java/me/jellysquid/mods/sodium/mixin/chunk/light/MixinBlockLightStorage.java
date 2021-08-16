package me.jellysquid.mods.sodium.mixin.chunk.light;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.jellysquid.mods.phosphor.common.chunk.light.BlockLightStorageAccess;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.lighting.BlockLightStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockLightStorage.class)
public abstract class MixinBlockLightStorage extends MixinLightStorage<BlockLightStorage.StorageMap> implements BlockLightStorageAccess {
    @Unique
    private final LongSet lightEnabled = new LongOpenHashSet();

    @Override
    protected void enableLightSources(final long chunkPos, final boolean enable) {
        if (enable) {
            this.lightEnabled.add(chunkPos);
        } else {
            this.lightEnabled.remove(chunkPos);
        }
    }

    @Override
    public boolean isLightEnabled(final long sectionPos) {
        return this.lightEnabled.contains(SectionPos.getZeroNode(sectionPos));
    }

    @Override
    protected int getLightmapComplexityChange(final long blockPos, final int oldVal, final int newVal, final NibbleArray lightmap) {
        return newVal - oldVal;
    }

    @Override
    protected int getInitialLightmapComplexity(final long sectionPos, final NibbleArray lightmap) {
        int complexity = 0;

        for (int y = 0; y < 16; ++y) {
            for (int z = 0; z < 16; ++z) {
                for (int x = 0; x < 16; ++x) {
                    complexity += lightmap.get(x, y, z);
                }
            }
        }

        return complexity;
    }
}
