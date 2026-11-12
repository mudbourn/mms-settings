package info.mudbourn.mmssettings;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

final class Speedometer {
    private static final int TICKS_PER_SECOND = 20;
    private static final int SAMPLES = 10;
    private static final int HOTBAR_HALF_WIDTH = 91;
    private static final int OFFHAND_SLOT_WIDTH = 29;
    private static final int GAP = 4;

    private static final double[] distances = new double[SAMPLES];
    private static boolean enabled;
    private static Vec3 last;
    private static int cursor;

    private Speedometer() {
    }

    static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> dispatcher.register(
            ClientCommandManager.literal("speedometer").executes(command -> {
                enabled = !enabled;
                command.getSource().sendFeedback(Component.translatable(
                    enabled ? "mms_settings.speedometer.on" : "mms_settings.speedometer.off"
                ));

                return 1;
            })
        ));

        ClientTickEvents.END_CLIENT_TICK.register(Speedometer::tick);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MmsSettings.MOD_ID, "speedometer"), Speedometer::render);
    }

    private static void tick(Minecraft client) {
        Player player = client.player;

        if (!enabled || player == null) {
            last = null;

            return;
        }

        Vec3 position = player.position();
        distances[cursor] = last == null ? 0 : position.distanceTo(last);
        cursor = (cursor + 1) % SAMPLES;
        last = position;
    }

    private static double blocksPerSecond() {
        double total = 0;

        for (double distance : distances) {
            total += distance;
        }

        return total / SAMPLES * TICKS_PER_SECOND;
    }

    private static void render(GuiGraphics graphics, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();

        if (!enabled || client.player == null || client.options.hideGui || client.player.isSpectator()) {
            return;
        }

        String text = String.format("%.2f b/s", blocksPerSecond());
        int width = client.font.width(text);
        int center = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() - 15;
        int offset = HOTBAR_HALF_WIDTH + OFFHAND_SLOT_WIDTH + GAP;
        boolean offhandLeft = client.player.getMainArm() == HumanoidArm.RIGHT;
        int x = offhandLeft ? center - offset - width : center + offset;

        graphics.drawString(client.font, text, x, y, 0xFFFFFFFF);
    }
}
