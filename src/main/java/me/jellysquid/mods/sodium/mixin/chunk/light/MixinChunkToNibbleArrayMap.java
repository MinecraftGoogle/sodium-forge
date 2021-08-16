package me.jellysquid.mods.sodium.mixin.chunk.light;

import me.jellysquid.mods.phosphor.common.chunk.light.SharedNibbleArrayMap;
import me.jellysquid.mods.phosphor.common.util.collections.DoubleBufferedLong2ObjectHashMap;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.lighting.LightDataMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@SuppressWarnings("OverwriteModifiers")
@Mixin(LightDataMap.class)
public abstract class MixinChunkToNibbleArrayMap implements SharedNibbleArrayMap {
    @Shadow
    private boolean cacheEnabled;

    @Shadow
    @Final
    private long[] lastSectionKeys;

    @Shadow
    @Final
    private NibbleArray[] lastSections;

    @Shadow
    public abstract void disableCache();

    private DoubleBufferedLong2ObjectHashMap<NibbleArray> queue;
    private boolean isShared;

    /**
     * @reason Allow shared access, avoid copying
     * @author JellySquid
     */
    @Overwrite
    public void copyDataLayer(long pos) {
        this.checkExclusiveOwner();

        this.queue.putSync(pos, this.queue.getSync(pos).copy());

        this.disableCache();
    }

    /**
     * @reason Allow shared access, avoid copying
     * @author JellySquid
     */
    @Overwrite
    public NibbleArray getLayer(long pos) {
        if (this.cacheEnabled) {
            // Hoist array field access out of the loop to allow the JVM to drop bounds checks
            long[] cachePositions = this.lastSectionKeys;

            for(int i = 0; i < cachePositions.length; ++i) {
                if (pos == cachePositions[i]) {
                    return this.lastSections[i];
                }
            }
        }

        // Move to a separate method to help the JVM inline methods
        return this.getUncached(pos);
    }

    private NibbleArray getUncached(long pos) {
        NibbleArray array;

        if (this.isShared) {
            array = this.queue.getAsync(pos);
        } else {
            array = this.queue.getSync(pos);
        }

        if (array == null) {
            return null;
        }

        if (this.cacheEnabled) {
            long[] cachePositions = this.lastSectionKeys;
            NibbleArray[] cacheArrays = this.lastSections;

            for(int i = cacheArrays.length - 1; i > 0; --i) {
                cachePositions[i] = cachePositions[i - 1];
                cacheArrays[i] = cacheArrays[i - 1];
            }

            cachePositions[0] = pos;
            cacheArrays[0] = array;
        }

        return array;
    }

    /**
     * @reason Allow shared access, avoid copying
     * @author JellySquid
     */
    @Overwrite
    public void setLayer(long pos, NibbleArray data) {
        this.checkExclusiveOwner();

        this.queue.putSync(pos, data);
    }

    /**
     * @reason Allow shared access, avoid copying
     * @author JellySquid
     */
    @Overwrite
    public NibbleArray removeLayer(long chunkPos) {
        this.checkExclusiveOwner();

        return this.queue.removeSync(chunkPos);
    }

    /**
     * @reason Allow shared access, avoid copying
     * @author JellySquid
     */
    @Overwrite
    public boolean hasLayer(long chunkPos) {
        if (this.isShared) {
            return this.queue.getAsync(chunkPos) != null;
        } else {
            return this.queue.containsSync(chunkPos);
        }
    }

    /**
     * Check if the light array table is exclusively owned (not shared). If not, an exception is thrown to catch the
     * invalid state. Synchronous writes can only occur while the table is exclusively owned by the writer/actor thread.
     */
    private void checkExclusiveOwner() {
        if (this.isShared) {
            throw new IllegalStateException("Tried to synchronously write to light data array table after it was made shareable");
        }
    }

    @Override
    public DoubleBufferedLong2ObjectHashMap<NibbleArray> getUpdateQueue() {
        return this.queue;
    }

    @Override
    public void makeSharedCopy(SharedNibbleArrayMap map) {
        this.queue = map.getUpdateQueue();
        this.isShared = this.queue != null;

        if (this.isShared) {
            this.queue.flushChangesSync();
        }
    }

    @Override
    public void init() {
        if (this.queue != null) {
            throw new IllegalStateException("Map already initialized");
        }

        this.queue = new DoubleBufferedLong2ObjectHashMap<>();
    }
}
