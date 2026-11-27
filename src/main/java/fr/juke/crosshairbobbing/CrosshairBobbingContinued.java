package fr.juke.crosshairbobbing;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client entrypoint: loads the config and registers the toggle keybind.
 */
public final class CrosshairBobbingContinued implements ClientModInitializer {
    public static final String MOD_ID = "crosshair-bobbing-continued";
    public static final Logger LOGGER = LoggerFactory.getLogger("Crosshair Bobbing Continued");

    private static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        Config.load(LOGGER);

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.crosshair-bobbing-continued.toggle",
                InputConstants.KEY_B,
                category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                Config config = Config.get();
                config.enabled = !config.enabled;
                Config.save(LOGGER);
                if (client.player != null) {
                    Component state = Component.translatable(config.enabled
                                    ? "crosshair-bobbing-continued.state.on"
                                    : "crosshair-bobbing-continued.state.off")
                            .withStyle(config.enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
                    client.player.displayClientMessage(
                            Component.translatable("crosshair-bobbing-continued.toggle", state), true);
                }
            }
        });

        LOGGER.info("Crosshair Bobbing Continued loaded (enabled={})", Config.get().enabled);
    }

    public static Minecraft minecraft() {
        return Minecraft.getInstance();
    }
}
