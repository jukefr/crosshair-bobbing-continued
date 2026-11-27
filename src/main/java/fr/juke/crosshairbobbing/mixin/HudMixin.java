package fr.juke.crosshairbobbing.mixin;

import fr.juke.crosshairbobbing.CrosshairAnimation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wraps the vanilla crosshair render with our transform.
 */
@Mixin(Hud.class)
public class HudMixin {
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    @Inject(method = "extractCrosshair", at = @At("HEAD"))
    private void crosshairBobbing$beforeCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        CrosshairAnimation.Transform transform = CrosshairAnimation.compute(
                partialTick, graphics.guiWidth(), graphics.guiHeight());
        if (transform == null) {
            ACTIVE.set(false);
            return;
        }
        ACTIVE.set(true);

        float centreX = graphics.guiWidth() * 0.5f;
        float centreY = graphics.guiHeight() * 0.5f;
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(centreX + transform.offsetX(), centreY - transform.offsetY());
        pose.rotate(transform.angle());
        pose.scale(transform.scaleX(), transform.scaleY());
        pose.translate(-centreX, -centreY);
    }

    @Inject(method = "extractCrosshair", at = @At("RETURN"))
    private void crosshairBobbing$afterCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ACTIVE.get()) {
            graphics.pose().popMatrix();
            ACTIVE.set(false);
        }
    }
}
