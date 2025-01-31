package me.jellysquid.mods.sodium.mixin.core.model;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.world.biome.ItemColorsExtended;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.IRegistryDelegate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemColors.class)
public class MixinItemColors implements ItemColorsExtended {
    // Parity with Vanilla's itemColor map
    private Reference2ReferenceMap<IRegistryDelegate<Item>, ItemColor> itemsToColor;

    private static final ItemColor DEFAULT_PROVIDER = (stack, tintIdx) -> -1;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        this.itemsToColor = new Reference2ReferenceOpenHashMap<>();
        this.itemsToColor.defaultReturnValue(DEFAULT_PROVIDER);
    }

    @Inject(method = "register", at = @At("HEAD"), remap = false)
    private void preRegisterColor(ItemColor mapper, ItemLike[] convertibles, CallbackInfo ci) {
        for (ItemLike convertible : convertibles) {
            synchronized (this) {
                this.itemsToColor.put(convertible.asItem().delegate, mapper);
            }
        }
    }

    @Override
    public ItemColor getColorProvider(ItemStack stack) {
        synchronized (this) {
            return this.itemsToColor.get(stack.getItem().delegate);
        }
    }
}
