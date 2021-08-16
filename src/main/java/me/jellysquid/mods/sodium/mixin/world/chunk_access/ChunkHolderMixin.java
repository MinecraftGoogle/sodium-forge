package me.jellysquid.mods.sodium.mixin.world.chunk_access;

import com.mojang.datafixers.util.Either;
import me.jellysquid.mods.lithium.common.world.chunk.ChunkHolderExtended;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ChunkHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReferenceArray;

@Mixin(ChunkHolder.class)
public class ChunkHolderMixin implements ChunkHolderExtended {
    @Shadow
    @Final
    private AtomicReferenceArray<CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>>> futures;

    private long lastRequestTime;

    @Override
    public CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> getFutureByStatus(int index) {
        return this.futures.get(index);
    }

    @Override
    public void setFutureForStatus(int index, CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> future) {
        this.futures.set(index, future);
    }

    @Override
    public boolean updateLastAccessTime(long time) {
        long prev = this.lastRequestTime;
        this.lastRequestTime = time;

        return prev != time;
    }
}
