package fr.juke.crosshairbobbing;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NeoForge client entrypoint: loads the config, registers the toggle keybind and
 * consumes the key each client tick.
 */
@Mod(value = CrosshairBobbingContinued.MOD_ID, dist = Dist.CLIENT)
public final class CrosshairBobbingContinued {
    /** NeoForge mod ids cannot contain hyphens. The config file keeps the hyphenated name. */
    public static final String MOD_ID = "crosshair_bobbing_continued";
    public static final Logger LOGGER = LoggerFactory.getLogger("Crosshair Bobbing Continued");

    private static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
    private static KeyMapping toggleKey;

    public CrosshairBobbingContinued(IEventBus modEventBus) {
        Config.load(LOGGER);

        modEventBus.addListener(this::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);

        LOGGER.info("Crosshair Bobbing Continued loaded (enabled={})", Config.get().enabled);
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        toggleKey = new KeyMapping(
                "key.crosshair-bobbing-continued.toggle",
                InputConstants.KEY_B,
                CATEGORY);
        event.register(toggleKey);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        if (toggleKey == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        while (toggleKey.consumeClick()) {
            Config config = Config.get();
            config.enabled = !config.enabled;
            Config.save(LOGGER);
            if (minecraft.player != null) {
                Component state = Component.translatable(config.enabled
                                ? "crosshair-bobbing-continued.state.on"
                                : "crosshair-bobbing-continued.state.off")
                        .withStyle(config.enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
                minecraft.player.sendOverlayMessage(
                        Component.translatable("crosshair-bobbing-continued.toggle", state));
            }
        }
    }
}
