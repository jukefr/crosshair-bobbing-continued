package fr.juke.crosshairbobbing.mixin;

import fr.juke.crosshairbobbing.CrosshairAnimation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wraps the vanilla crosshair render with our transform.
 */
@Mixin(Gui.class)
public class GuiMixin {
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    @Inject(method = "renderCrosshair", at = @At("HEAD"))
    private void crosshairBobbing$beforeCrosshair(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
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
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(centreX + transform.offsetX(), centreY - transform.offsetY(), 0.0);
        pose.mulPose(Axis.ZP.rotation(transform.angle()));
        pose.scale(transform.scaleX(), transform.scaleY(), 1.0f);
        pose.translate(-centreX, -centreY, 0.0);
    }

    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    private void crosshairBobbing$afterCrosshair(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ACTIVE.get()) {
            graphics.pose().popPose();
            ACTIVE.set(false);
        }
    }
}
