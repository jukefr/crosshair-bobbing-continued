package fr.juke.crosshairbobbing.mixin;

import fr.juke.crosshairbobbing.CrosshairAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Triggers the crosshair hit animation on a real entity hit or a broken block.
 */
@Mixin(MultiPlayerGameMode.class)
public class GameModeMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void crosshairBobbing$onAttack(Player player, Entity target, CallbackInfo ci) {
        if (player == Minecraft.getInstance().player) {
            CrosshairAnimation.onAttack(1.0f);
        }
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void crosshairBobbing$onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            CrosshairAnimation.onAttack(0.6f);
        }
    }
}
