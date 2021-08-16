package me.jellysquid.mods.sodium.mixin.client.world;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.lighting.WorldLightManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld
{
    @Redirect(
        method = "unload(Lnet/minecraft/world/chunk/Chunk;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/lighting/WorldLightManager;enableLightSources(Lnet/minecraft/util/math/ChunkPos;Z)V"
        )
    )
    private void cancelDisableLightUpdates(final WorldLightManager lightingProvider, final ChunkPos pos, final boolean enable) {
    }
}
