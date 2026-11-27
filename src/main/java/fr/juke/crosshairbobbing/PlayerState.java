package fr.juke.crosshairbobbing;

import fr.juke.crosshairbobbing.mixin.GameRendererAccessor;
import fr.juke.crosshairbobbing.mixin.LivingEntityAccessor;
import net.minecraft.util.Mth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

/**
 * Version-specific view of the local player's animation state.
 *
 * <p>Everything that changed between Minecraft versions should live here so the
 * maths in {@link CrosshairAnimation} stays stable.</p>
 */
final class PlayerState {
    private PlayerState() {
    }

    static float bob(LocalPlayer player, float partialTicks) {
        return Mth.lerp(partialTicks, player.oBob, player.bob);
    }

    static float backwardsWalkDistance(LocalPlayer player, float partialTicks) {
        return -(player.walkDist + (player.walkDist - player.walkDistO) * partialTicks);
    }

    static float swingProgress(LocalPlayer player, float partialTicks) {
        return player.getAttackAnim(partialTicks);
    }

    static UseKind useKind(LocalPlayer player) {
        if (!player.isUsingItem()) {
            return UseKind.NONE;
        }
        ItemStack stack = player.getUseItem();
        UseAnim animation = stack.getUseAnimation();
        if (animation == UseAnim.EAT || animation == UseAnim.DRINK) {
            return UseKind.EATING;
        }
        if (animation == UseAnim.BOW) {
            return UseKind.BOW;
        }
        if (animation == UseAnim.SPEAR) {
            return UseKind.SPEAR;
        }
        if (stack.getItem() instanceof CrossbowItem && !CrossbowItem.isCharged(stack)) {
            return UseKind.CROSSBOW;
        }
        return UseKind.NONE;
    }

    static float usingItemProgress(LocalPlayer player, float partialTicks) {
        if (!player.isUsingItem()) {
            return 1.0f;
        }
        ItemStack stack = player.getUseItem();
        float used = stack.getUseDuration(player) - (player.getUseItemRemainingTicks() - partialTicks + 1.0f);
        if (stack.getUseAnimation() == UseAnim.BOW) {
            return Math.min(used / 20.0f, 1.0f);
        }
        if (stack.getUseAnimation() == UseAnim.SPEAR) {
            return Math.min(used / 10.0f, 1.0f);
        }
        int duration = Math.max(1, stack.getUseDuration(player));
        return used / duration;
    }

    static float spinProgress(LocalPlayer player, float partialTicks) {
        if (!player.isAutoSpinAttack()) {
            return 0.0f;
        }
        int ticks = ((LivingEntityAccessor) player).crosshairBobbing$autoSpinAttackTicks();
        return Math.max(0.0f, Math.min(1.0f, (20 - ticks + partialTicks) / 20.0f));
    }

    static boolean crouching(LocalPlayer player) {
        return player.getPose() == Pose.CROUCHING || player.isCrouching();
    }

    static float fov(Minecraft minecraft, float partialTicks) {
        return (float) ((GameRendererAccessor) minecraft.gameRenderer)
                .crosshairBobbing$getFov(minecraft.gameRenderer.getMainCamera(), partialTicks, true);
    }

    enum UseKind {
        NONE,
        EATING,
        BOW,
        SPEAR,
        CROSSBOW
    }
}
