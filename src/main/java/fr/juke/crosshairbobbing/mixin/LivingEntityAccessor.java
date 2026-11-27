package fr.juke.crosshairbobbing.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("autoSpinAttackTicks")
    int crosshairBobbing$autoSpinAttackTicks();
}
