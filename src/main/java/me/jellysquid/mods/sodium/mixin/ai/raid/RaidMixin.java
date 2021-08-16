package me.jellysquid.mods.sodium.mixin.ai.raid;

import net.minecraft.util.math.MathHelper;
import net.minecraft.world.raid.Raid;
import net.minecraft.world.server.ServerBossInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Raid.class)
public abstract class RaidMixin {
    @Shadow
    @Final
    private ServerBossInfo raidEvent;

    @Shadow
    public abstract float getHealthOfLivingRaiders();

    @Shadow
    private float totalHealth;

    private boolean isBarDirty;

    /**
     * Check if an update was queued for the bar, and if so, perform an update
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (this.isBarDirty) {
            this.raidEvent.setPercent(MathHelper.clamp(this.getHealthOfLivingRaiders() / this.totalHealth, 0.0F, 1.0F));

            this.isBarDirty = false;
        }
    }

    /**
     * @reason Delay re-calculating and sending progress bar updates until the next tick to avoid excessive updates
     * @author JellySquid
     */
    @Overwrite
    public void updateBossbar() {
        this.isBarDirty = true;
    }

}
