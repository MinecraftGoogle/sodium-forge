package me.jellysquid.mods.sodium.mixin.entity.replace_entitytype_predicates;

import me.jellysquid.mods.lithium.common.world.WorldHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.HangingEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.function.Predicate;

@Mixin(HangingEntity.class)
public abstract class AbstractDecorationEntityMixin extends Entity {
    @Shadow
    @Final
    protected static Predicate<Entity> HANGING_ENTITY; //entity instanceof AbstractDecorationEntity

    public AbstractDecorationEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Redirect(method = "survives", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private List<Entity> getAbstractDecorationEntities(World world, Entity excluded, AxisAlignedBB box, Predicate<? super Entity> predicate) {
        if (predicate == HANGING_ENTITY) {
            return WorldHelper.getEntitiesOfClass(world, excluded, HangingEntity.class, box);
        }
        return world.getEntities(excluded, box, predicate);
    }
}
