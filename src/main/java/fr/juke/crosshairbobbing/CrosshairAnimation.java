package fr.juke.crosshairbobbing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Computes the screen-space crosshair transform that makes the crosshair follow
 * the camera's view-bobbing, plus the optional action animations.
 *
 * <p>The maths is deliberately kept free of Minecraft rendering types so the
 * same code can be used on every Minecraft version. The GUI mixin on each branch
 * only has to push a matrix and apply the returned {@link Transform}.</p>
 */
public final class CrosshairAnimation {
    private static final float MIN_ATTACK_SCALE = 1.4f;
    private static final float MAX_ATTACK_SCALE = 2.0f;
    private static final float MIN_ATTACK_DEGREES = 15f;
    private static final float MAX_ATTACK_DEGREES = 25f;
    private static final long ATTACK_ANIM_LEN = 200L;
    private static final long ITEM_SWITCH_LEN = 250L;
    private static final double DEPTH_FALLBACK = 1000.0;

    private static boolean crit;
    private static int critDir = 1;
    private static long attackAnimStart;
    private static float attackAnimDegrees;
    private static float attackAnimScale;
    private static float attackDamage;

    private static long itemSwitchStart;
    private static int lastSlot = -1;
    private static ItemStack lastItem = ItemStack.EMPTY;
    private static boolean itemStateInitialised;

    private static float sneakProgress;
    private static long lastFrameNanos = System.nanoTime();

    private CrosshairAnimation() {
    }

    public static void onAttack(float damage) {
        boolean isCrit = damage > 1.0f;

        attackDamage = Math.min(1.0f, damage);
        attackAnimStart = System.currentTimeMillis();
        float random = (float) Math.random();
        attackAnimDegrees = random * (MAX_ATTACK_DEGREES - MIN_ATTACK_DEGREES) + MIN_ATTACK_DEGREES;
        attackAnimScale = random * (MAX_ATTACK_SCALE - MIN_ATTACK_SCALE) + MIN_ATTACK_SCALE;
        crit = isCrit;
        critDir *= -1;
    }

    /**
     * @return the transform to apply around the crosshair, or {@code null} to leave it alone.
     */
    public static Transform compute(float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Config config = Config.get();
        if (player == null || minecraft.level == null) {
            return null;
        }
        if (!config.enabled || width <= 0 || height <= 0) {
            return null;
        }

        long now = System.currentTimeMillis();
        long frameNanos = System.nanoTime();
        float deltaSeconds = Math.min(0.1f, (frameNanos - lastFrameNanos) / 1_000_000_000.0f);
        lastFrameNanos = frameNanos;

        // ---- view bobbing (same transform as GameRenderer#bobView) ----
        double translateX = 0.0;
        double translateY = 0.0;
        double rotationZ = 0.0;
        double rotationX = 0.0;
        boolean bobbing = config.followBobbing && minecraft.options.bobView().get();
        if (bobbing) {
            float walk = PlayerState.backwardsWalkDistance(player, partialTick);
            float bobAmount = PlayerState.bob(player, partialTick);
            translateX = Math.sin(walk * Math.PI) * bobAmount * 0.5;
            translateY = -Math.abs(Math.cos(walk * Math.PI) * bobAmount);
            rotationZ = Math.sin(walk * Math.PI) * bobAmount * 3.0;
            rotationX = Math.abs(Math.cos(walk * Math.PI - 0.2) * bobAmount) * 5.0;
        }

        // ---- item switch: a short camera-space slide ----
        if (config.itemSwitchAnimation) {
            ItemStack held = player.getInventory().getSelectedItem();
            int slot = player.getInventory().getSelectedSlot();
            if (!itemStateInitialised) {
                itemStateInitialised = true;
                lastSlot = slot;
                lastItem = held.copy();
                itemSwitchStart = now - ITEM_SWITCH_LEN;
            } else if (slot != lastSlot || held.getItem() != lastItem.getItem()) {
                lastSlot = slot;
                lastItem = held.copy();
                itemSwitchStart = now;
            }
            double t = clamp((now - itemSwitchStart) / (double) ITEM_SWITCH_LEN, 0.0, 1.0);
            double eased = t * t * (3.0 - 2.0 * t);
            translateX += (1.0 - eased) * 0.08;
            translateY += (1.0 - eased) * -0.3;
        }

        Matrix4f transform = new Matrix4f()
                .translation((float) translateX, (float) translateY, 0.0f)
                .rotateZ((float) Math.toRadians(rotationZ))
                .rotateX((float) Math.toRadians(rotationX));

        float depth = centreDepth(minecraft, player, partialTick);
        Vector4f centre = new Vector4f(0.0f, 0.0f, -depth, 1.0f).mul(transform);
        Vector4f up = new Vector4f(0.0f, 1.0f, -depth, 1.0f).mul(transform);

        float fov = PlayerState.fov(minecraft);
        if (!(fov > 1.0f) || !(fov < 179.0f)) {
            fov = 70.0f;
        }
        double halfWidth = width * 0.5;
        double halfHeight = height * 0.5;
        double tanHalfFov = Math.tan(Math.toRadians(fov) * 0.5);
        double aspect = (double) width / (double) height;

        double centreScreenX = centre.x / -centre.z * halfWidth / (tanHalfFov * aspect);
        double centreScreenY = centre.y / -centre.z * halfHeight / tanHalfFov;
        double upScreenX = up.x / -up.z * halfWidth / (tanHalfFov * aspect);
        double upScreenY = up.y / -up.z * halfHeight / tanHalfFov;

        double angle = Math.atan2(upScreenX - centreScreenX, upScreenY - centreScreenY);
        double offsetX = centreScreenX;
        double offsetY = centreScreenY;
        double scale = 1.0;
        double verticalScale = 1.0;

        // ---- sneaking tilt (smoothed, because camera eye height is not public everywhere) ----
        float sneakTarget = PlayerState.crouching(player) ? 1.0f : 0.0f;
        sneakProgress += (sneakTarget - sneakProgress) * Math.min(1.0f, deltaSeconds * 12.0f);
        if (config.sneakAnimation) {
            angle += Math.toRadians(-sneakProgress * 70.0);
        }

        if (config.tridentSpinAnimation) {
            angle += Math.toRadians(
                    interpolateSin(PlayerState.spinProgress(player, partialTick), 0.0, Math.PI * 0.5) * -720.0);
        }

        // ---- hand swing pulse ----
        float swing = config.swingAnimation ? PlayerState.swingProgress(player, partialTick) : 1.0f;

        // ---- attack animation ----
        if (config.attackAnimation && now < attackAnimStart + ATTACK_ANIM_LEN && attackDamage > 0.01f) {
            double t = clamp((now - attackAnimStart) / (double) ATTACK_ANIM_LEN, 0.0, 1.0);
            double wave = Math.sin(Math.PI * 0.5 + Math.PI * 0.5 * t);
            angle += Math.toRadians(wave * attackDamage * attackAnimDegrees);
            scale *= wave * attackDamage * (attackAnimScale - 1.0) + 1.0;
            swing = 1.0f;
        }

        scale *= 1.0 + interpolateSin(swing, Math.PI * 0.5, Math.PI) * 0.2;

        // ---- item use animations ----
        PlayerState.UseKind useKind = PlayerState.useKind(player);
        float useProgress = PlayerState.usingItemProgress(player, partialTick);
        if (useKind == PlayerState.UseKind.EATING) {
            if (config.eatingAnimation) {
                if (useProgress < 0.2f) {
                    angle += Math.toRadians(
                            interpolateSin(Math.min(1.0, useProgress * 10.0), 0.0, Math.PI * 0.5) * 135.0);
                } else if (useProgress < 0.9f) {
                    angle += Math.toRadians(
                            interpolateSin((useProgress - 0.2f) / 0.7f, 0.0, Math.PI * 6.0) * -30.0 + 45.0);
                } else {
                    angle += Math.toRadians(
                            interpolateSin(Math.min(1.0, (useProgress - 0.9f) * 10.0), 0.0, Math.PI * 0.5) * -135.0 + 45.0);
                }
            }
        } else if (useKind == PlayerState.UseKind.CROSSBOW && config.crossbowAnimation) {
            if (useProgress < 0.9f) {
                angle += Math.toRadians(useProgress / 0.9 * 360.0);
                scale *= fract(5.0 * (1.0 - useProgress / 0.9)) * 0.8 + 1.0;
            } else {
                angle += Math.toRadians(
                        interpolateSin(Math.min(1.0, (useProgress - 0.9f) * 10.0), 0.0, Math.PI * 0.5) * -180.0 + 180.0);
            }
        } else if ((useKind == PlayerState.UseKind.BOW || useKind == PlayerState.UseKind.SPEAR)
                && config.bowAimAnimation) {
            scale *= interpolateSin(useProgress, 0.0, Math.PI * 0.5) * 0.5 + 1.0;
            verticalScale *= interpolateSin(useProgress, 0.0, Math.PI * 0.5) * -0.6 + 1.0;
        }

        if (!bobbing && Math.abs(offsetX) < 1.0e-4 && Math.abs(offsetY) < 1.0e-4
                && Math.abs(angle) < 1.0e-4 && Math.abs(scale - 1.0) < 1.0e-4
                && Math.abs(verticalScale - 1.0) < 1.0e-4) {
            return null;
        }

        return new Transform((float) offsetX, (float) offsetY, (float) angle,
                (float) scale, (float) (scale * verticalScale));
    }

    private static float centreDepth(Minecraft minecraft, LocalPlayer player, float partialTick) {
        HitResult hit = minecraft.hitResult;
        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            double distance = hit.getLocation().distanceTo(player.getEyePosition(partialTick));
            if (distance > 0.05) {
                return (float) Math.min(distance, 1000.0);
            }
        }
        return (float) DEPTH_FALLBACK;
    }

    private static double interpolateSin(double x, double begin, double end) {
        x = clamp(x, 0.0, 1.0);
        return Math.sin((end - begin) * x + begin);
    }

    private static double fract(double value) {
        return value - Math.floor(value);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Transform(float offsetX, float offsetY, float angle, float scaleX, float scaleY) {
    }
}
