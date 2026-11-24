package atd.driver;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Evidence input driver. Reads one-line commands from atd-driver/inbox.txt and injects them as
 * ordinary key-mapping input, so the vanilla client runs handleKeybinds -> startUseItem/startAttack
 * exactly as for a physical mouse click. It never calls server-facing methods directly.
 */
public final class InputDriver implements ClientModInitializer {
    private Path dir;
    private final Deque<String> queue = new ArrayDeque<>();
    private int waitTicks;
    private int holdUseTicks = -1;
    private int holdAttackTicks = -1;
    private long tick;
    private final java.util.Map<KeyMapping, Integer> holds = new java.util.LinkedHashMap<>();

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft mc) {
        tick++;
        if (dir == null) {
            dir = mc.gameDirectory.toPath().resolve("atd-driver");
            try {
                Files.createDirectories(dir);
                log("{\"event\":\"driver_ready\"}");
            } catch (Exception e) {
                return;
            }
        }
        if (holdUseTicks >= 0 && holdUseTicks-- == 0) {
            mc.options.keyUse.setDown(false);
            log("{\"event\":\"use_released\"," + state(mc) + "}");
        }
        if (holdAttackTicks >= 0 && holdAttackTicks-- == 0) {
            mc.options.keyAttack.setDown(false);
        }
        for (var it = holds.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            if (e.getValue() <= 0) {
                e.getKey().setDown(false);
                it.remove();
                log("{\"event\":\"released\",\"key\":" + q(e.getKey().getName()) + "," + state(mc) + "}");
            } else e.setValue(e.getValue() - 1);
        }
        pollInbox();
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        while (!queue.isEmpty() && waitTicks == 0) {
            String line = queue.poll();
            try {
                run(mc, line.trim());
            } catch (Throwable t) {
                log("{\"cmd\":" + q(line) + ",\"error\":" + q(t.toString()) + "}");
            }
        }
    }

    private void pollInbox() {
        Path inbox = dir.resolve("inbox.txt");
        try {
            if (!Files.exists(inbox) || Files.size(inbox) == 0) return;
            Path taken = dir.resolve("inbox.taking");
            Files.move(inbox, taken, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            List<String> lines = Files.readAllLines(taken, StandardCharsets.UTF_8);
            Files.delete(taken);
            for (String l : lines) if (!l.isBlank()) queue.add(l);
        } catch (Exception ignored) {
        }
    }

    private void run(Minecraft mc, String line) throws Exception {
        String[] a = line.split("\\s+");
        String before = state(mc);
        switch (a[0]) {
            case "click_use" -> {
                KeyMapping.click(key(mc.options.keyUse));
                log("{\"cmd\":" + q(line) + ",\"before\":{" + before + "}}");
            }
            case "hold_use" -> {
                int n = Integer.parseInt(a[1]);
                mc.options.keyUse.setDown(true);
                KeyMapping.click(key(mc.options.keyUse));
                holdUseTicks = n;
                log("{\"cmd\":" + q(line) + ",\"before\":{" + before + "}}");
            }
            case "click_attack" -> {
                KeyMapping.click(key(mc.options.keyAttack));
                log("{\"cmd\":" + q(line) + ",\"before\":{" + before + "}}");
            }
            case "hold_attack" -> {
                mc.options.keyAttack.setDown(true);
                KeyMapping.click(key(mc.options.keyAttack));
                holdAttackTicks = Integer.parseInt(a[1]);
                log("{\"cmd\":" + q(line) + ",\"before\":{" + before + "}}");
            }
            case "press" -> {
                KeyMapping m = KeyMapping.get(a[1]);
                if (m == null) throw new IllegalArgumentException("unknown key " + a[1]);
                int n = a.length > 2 ? Integer.parseInt(a[2]) : 0;
                if (n > 0) { m.setDown(true); holds.put(m, n); }
                if (m == mc.options.keyAttack) {
                    // Vanilla continues mining only while the mouse is grabbed by a focused window; emulate that focus.
                    Field grabbed = mc.mouseHandler.getClass().getDeclaredField("mouseGrabbed");
                    grabbed.setAccessible(true);
                    grabbed.setBoolean(mc.mouseHandler, true);
                }
                KeyMapping.click(key(m));
                log("{\"cmd\":" + q(line) + ",\"before\":{" + before + "}}");
            }
            case "slotclick" -> {
                var menu = mc.player.containerMenu;
                if (mc.gui.screen() == null) {
                    // A real player cannot click a slot without an open screen (W5-OBS-1:
                    // a denial closes the inventory, a scripted click must not reach the closed menu).
                    log("{\"cmd\":" + q(line) + ",\"skipped\":\"no_screen\",\"carried\":" + q(menu.getCarried().toString()) + "}");
                    break;
                }
                mc.gameMode.handleContainerInput(menu.containerId, Integer.parseInt(a[1]), Integer.parseInt(a[2]),
                    net.minecraft.world.inventory.ContainerInput.valueOf(a[3]), mc.player);
                log("{\"cmd\":" + q(line) + ",\"container\":" + menu.containerId + ",\"carried\":" + q(menu.getCarried().toString()) + "}");
            }
            case "respawn" -> {
                if (mc.player != null) mc.player.respawn();
                mc.gui.setScreen(null);
                log("{\"cmd\":\"respawn\"}");
            }
            case "landmarks" -> {
                Object snap = Class.forName("com.diskree.achievetodo.client.bridge.BridgeClient").getMethod("landmarks").invoke(null);
                log("{\"cmd\":\"landmarks\",\"snapshot\":" + q(String.valueOf(snap)) + "}");
            }
            case "release_all" -> {
                for (var m : holds.keySet()) m.setDown(false);
                holds.clear();
                log("{\"cmd\":\"release_all\"}");
            }
            case "slot" -> {
                KeyMapping.click(key(mc.options.keyHotbarSlots[Integer.parseInt(a[1])]));
                log("{\"cmd\":" + q(line) + "}");
            }
            case "swap_hands" -> {
                KeyMapping.click(key(mc.options.keySwapOffhand));
                log("{\"cmd\":" + q(line) + "}");
            }
            case "close" -> {
                if (mc.player != null) mc.player.closeContainer();
                log("{\"cmd\":" + q(line) + ",\"before\":{" + before + "}}");
            }
            case "aim" -> {
                var p = mc.player;
                double dx = Double.parseDouble(a[1]) - p.getX();
                double dy = Double.parseDouble(a[2]) - p.getEyeY();
                double dz = Double.parseDouble(a[3]) - p.getZ();
                float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
                float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
                // Same effect as mouse look: client-authoritative rotation, synced by the normal movement packet.
                p.turn((yaw - p.getYRot()) / 0.15, (pitch - p.getXRot()) / 0.15);
                log("{\"cmd\":" + q(line) + ",\"yaw\":" + yaw + ",\"pitch\":" + pitch + ",\"before\":{" + before + "}}");
            }
            case "wait" -> waitTicks = Integer.parseInt(a[1]);
            case "state" -> log("{\"cmd\":\"state\",\"tag\":" + q(a.length > 1 ? a[1] : "") + "," + before + "}");
            // 0.3.0: the last N chat messages with every show_text hover they carry (chat hover oracle).
            case "chat" -> log("{\"cmd\":\"chat\",\"messages\":" + chat(mc, a.length > 1 ? Integer.parseInt(a[1]) : 5) + "}");
            case "shot" -> {
                String name = a[1].replaceAll("[^A-Za-z0-9_.-]", "_") + ".png";
                Screenshot.grab(mc.gameDirectory, name, mc.gameRenderer.mainRenderTarget(), 1, c -> { });
                log("{\"cmd\":\"shot\",\"file\":" + q(new File(new File(mc.gameDirectory, "screenshots"), name).getPath()) + "," + before + "}");
            }
            case "quit" -> {
                log("{\"cmd\":\"quit\"," + before + "}");
                mc.disconnectWithProgressScreen();
            }
            case "exit" -> {
                log("{\"cmd\":\"exit\"}");
                mc.stop();
            }
            default -> log("{\"cmd\":" + q(line) + ",\"error\":\"unknown\"}");
        }
    }

    private static InputConstants.Key key(KeyMapping m) throws Exception {
        Field f = KeyMapping.class.getDeclaredField("key");
        f.setAccessible(true);
        return (InputConstants.Key) f.get(m);
    }

    private String state(Minecraft mc) {
        StringBuilder b = new StringBuilder();
        b.append("\"tick\":").append(tick);
        b.append(",\"screen\":").append(q(mc.gui.screen() == null ? "none" : mc.gui.screen().getClass().getSimpleName()));
        // Full-screen overlay class (AchieveToDo's locked-VISION black overlay is GameRendererMixin$VisionOverlay).
        b.append(",\"guiOverlay\":").append(q(mc.gui.overlay() == null ? "none" : mc.gui.overlay().getClass().getName()));
        if (mc.player != null) {
            var p = mc.player;
            b.append(",\"mode\":").append(q(mc.gameMode == null ? "?" : String.valueOf(mc.gameMode.getPlayerMode())));
            b.append(",\"pos\":").append(q(String.format(java.util.Locale.ROOT, "%.3f %.3f %.3f %.2f %.2f", p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot())));
            b.append(",\"mainhand\":").append(q(p.getItemBySlot(EquipmentSlot.MAINHAND).toString()));
            b.append(",\"offhand\":").append(q(p.getItemBySlot(EquipmentSlot.OFFHAND).toString()));
            b.append(",\"selectedSlot\":").append(p.getInventory().getSelectedSlot());
            b.append(",\"usingItem\":").append(p.isUsingItem());
            b.append(",\"crouching\":").append(p.isCrouching());
            b.append(",\"sprinting\":").append(p.isSprinting());
            b.append(",\"swimming\":").append(p.isSwimming());
            b.append(",\"sleeping\":").append(p.isSleeping());
            b.append(",\"vehicle\":").append(q(p.getVehicle() == null ? "" : String.valueOf(p.getVehicle().getType())));
            b.append(",\"fallFlying\":").append(p.isFallFlying());
            b.append(",\"onGround\":").append(p.onGround());
            b.append(",\"dim\":").append(q(String.valueOf(p.level().dimension())));
            b.append(",\"armor\":").append(q(p.getItemBySlot(EquipmentSlot.HEAD) + "|" + p.getItemBySlot(EquipmentSlot.CHEST) + "|" + p.getItemBySlot(EquipmentSlot.LEGS) + "|" + p.getItemBySlot(EquipmentSlot.FEET)));
        }
        b.append(",\"overlay\":").append(q(overlay(mc)));
        HitResult h = mc.hitResult;
        if (h instanceof BlockHitResult bh && h.getType() == HitResult.Type.BLOCK) {
            b.append(",\"hit\":").append(q("block " + bh.getBlockPos().toShortString() + " " + bh.getDirection() + " "
                    + (mc.level == null ? "" : mc.level.getBlockState(bh.getBlockPos()).toString())));
        } else if (h instanceof EntityHitResult eh) {
            b.append(",\"hit\":").append(q("entity " + eh.getEntity().getType() + " " + eh.getEntity().getUUID()));
        } else {
            b.append(",\"hit\":\"miss\"");
        }
        return b.toString();
    }

    private static String chat(Minecraft mc, int count) {
        try {
            Field chatField = mc.gui.hud.getClass().getDeclaredField("chat");
            chatField.setAccessible(true);
            Object chat = chatField.get(mc.gui.hud);
            Field all = chat.getClass().getDeclaredField("allMessages");
            all.setAccessible(true);
            java.util.List<?> messages = (java.util.List<?>) all.get(chat);
            StringBuilder b = new StringBuilder("[");
            // allMessages is newest first.
            for (int i = 0; i < Math.min(count, messages.size()); i++) {
                var message = (net.minecraft.client.multiplayer.chat.GuiMessage) messages.get(i);
                var hovers = new java.util.ArrayList<String>();
                message.content().visit((style, text) -> {
                    if (style.getHoverEvent() instanceof net.minecraft.network.chat.HoverEvent.ShowText show) {
                        String value = show.value().getString();
                        if (!hovers.contains(value)) hovers.add(value);
                    }
                    return java.util.Optional.empty();
                }, net.minecraft.network.chat.Style.EMPTY);
                if (i > 0) b.append(',');
                b.append("{\"text\":").append(q(message.content().getString())).append(",\"hovers\":[");
                for (int h = 0; h < hovers.size(); h++) b.append(h > 0 ? "," : "").append(q(hovers.get(h)));
                b.append("]}");
            }
            return b.append(']').toString();
        } catch (Throwable e) {
            return q("?" + e);
        }
    }

    private static String overlay(Minecraft mc) {
        try {
            Object hud = mc.gui.hud;
            Field text = hud.getClass().getDeclaredField("overlayMessageString");
            Field time = hud.getClass().getDeclaredField("overlayMessageTime");
            text.setAccessible(true); time.setAccessible(true);
            Object c = text.get(hud);
            int t = time.getInt(hud);
            return c == null || t <= 0 ? "" : ((net.minecraft.network.chat.Component) c).getString();
        } catch (Throwable e) {
            return "?" + e.getClass().getSimpleName();
        }
    }

    private void log(String json) {
        try {
            String line = "{\"utc\":\"" + Instant.now() + "\"," + json.substring(1) + "\n";
            Files.writeString(dir.resolve("journal.jsonl"), line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
        }
    }

    private static String q(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\"";
    }
}
