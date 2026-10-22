package dev.sp4rkled.loop;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Input;

import java.util.ArrayList;
import java.util.List;

/** Client-side tick recorder/replayer. No teleporting or Baritone integration. */
public final class LoopClient implements ClientModInitializer {
    private static final List<Frame> FRAMES = new ArrayList<>();
    private static Mode mode = Mode.IDLE;
    private static int playbackIndex;

    private enum Mode { IDLE, RECORDING, PLAYING }

    private record Frame(float yaw, float pitch, boolean forward, boolean backward,
                         boolean left, boolean right, boolean jump, boolean shift,
                         boolean sprint, boolean attack, boolean use) { }

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("loopstart").executes(ctx -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return 0;
                stopPlayback(mc);
                FRAMES.clear();
                mode = Mode.RECORDING;
                tell(mc, "Loop: recording started. Run /loopend to finish.");
                return 1;
            }));

            dispatcher.register(ClientCommands.literal("loopend").executes(ctx -> {
                if (mode != Mode.RECORDING) {
                    tell(Minecraft.getInstance(), "Loop: nothing is being recorded.");
                    return 0;
                }
                mode = Mode.IDLE;
                tell(Minecraft.getInstance(), "Loop: recorded " + FRAMES.size() + " ticks. Use /looprun to replay.");
                return 1;
            }));

            dispatcher.register(ClientCommands.literal("looprun").executes(ctx -> {
                Minecraft mc = Minecraft.getInstance();
                if (FRAMES.isEmpty()) {
                    tell(mc, "Loop: no recording yet. Use /loopstart, then /loopend.");
                    return 0;
                }
                if (mc.player == null) return 0;
                playbackIndex = 0;
                mode = Mode.PLAYING;
                tell(mc, "Loop: replaying " + FRAMES.size() + " ticks repeatedly. Use /loopover to stop.");
                return 1;
            }));

            dispatcher.register(ClientCommands.literal("loopover").executes(ctx -> {
                Minecraft mc = Minecraft.getInstance();
                releaseKeys(mc);
                mode = Mode.IDLE;
                tell(mc, "Loop: stopped.");
                return 1;
            }));
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            LocalPlayer player = mc.player;
            if (player == null) {
                if (mode == Mode.PLAYING) mode = Mode.IDLE;
                return;
            }
            if (mode == Mode.RECORDING) {
                Input input = player.input.keyPresses;
                FRAMES.add(new Frame(player.getYRot(), player.getXRot(),
                        input.forward(), input.backward(), input.left(), input.right(),
                        input.jump(), input.shift(), input.sprint(),
                        mc.options.keyAttack.isDown(), mc.options.keyUse.isDown()));
            } else if (mode == Mode.PLAYING && !FRAMES.isEmpty()) {
                Frame f = FRAMES.get(playbackIndex);
                player.setYRot(f.yaw());
                player.setXRot(f.pitch());
                mc.options.keyUp.setDown(f.forward());
                mc.options.keyDown.setDown(f.backward());
                mc.options.keyLeft.setDown(f.left());
                mc.options.keyRight.setDown(f.right());
                mc.options.keyJump.setDown(f.jump());
                mc.options.keyShift.setDown(f.shift());
                mc.options.keySprint.setDown(f.sprint());
                mc.options.keyAttack.setDown(f.attack());
                mc.options.keyUse.setDown(f.use());
                playbackIndex = (playbackIndex + 1) % FRAMES.size();
            }
        });
    }

    private static void stopPlayback(Minecraft mc) {
        if (mode == Mode.PLAYING) releaseKeys(mc);
        mode = Mode.IDLE;
    }

    private static void releaseKeys(Minecraft mc) {
        mc.options.keyUp.setDown(false);
        mc.options.keyDown.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);
        mc.options.keyJump.setDown(false);
        mc.options.keyShift.setDown(false);
        mc.options.keySprint.setDown(false);
        mc.options.keyAttack.setDown(false);
        mc.options.keyUse.setDown(false);
    }

    private static void tell(Minecraft mc, String message) {
        mc.gui.getChat().addClientSystemMessage(Component.literal(message));
    }
}
