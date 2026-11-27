package fr.juke.crosshairbobbing;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client entrypoint: loads the config and registers the toggle keybind.
 */
public final class CrosshairBobbingReborn implements ClientModInitializer {
    public static final String MOD_ID = "crosshair-bobbing-reborn";
    public static final Logger LOGGER = LoggerFactory.getLogger("Crosshair Bobbing Reborn");

    private static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        Config.load(LOGGER);

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.crosshair-bobbing-reborn.toggle",
                InputConstants.KEY_B,
                "key.categories.crosshair-bobbing-reborn"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                Config config = Config.get();
                config.enabled = !config.enabled;
                Config.save(LOGGER);
                if (client.player != null) {
                    Component state = Component.translatable(config.enabled
                                    ? "crosshair-bobbing-reborn.state.on"
                                    : "crosshair-bobbing-reborn.state.off")
                            .withStyle(config.enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
                    client.player.displayClientMessage(
                            Component.translatable("crosshair-bobbing-reborn.toggle", state), true);
                }
            }
        });

        LOGGER.info("Crosshair Bobbing Reborn loaded (enabled={})", Config.get().enabled);
    }

    public static Minecraft minecraft() {
        return Minecraft.getInstance();
    }
}
