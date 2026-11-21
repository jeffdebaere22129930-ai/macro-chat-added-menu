package com.primitive;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

/**
 * Menu support. We sit in front of Minecraft's own GLFW input callbacks so we can
 *  - record clicks / scrolling / ESC / the inventory key while a menu (like /shop) is open, and
 *  - replay them by calling Minecraft's own callbacks, exactly as if you had done it yourself.
 * Cursor positions are stored as a fraction of the window, so they survive a resized window.
 */
public final class GuiInput {
    private GuiInput() {}

    public static final int EV_MOUSE = 0;
    public static final int EV_KEY = 1;
    public static final int EV_SCROLL = 2;

    private static boolean installed;
    private static GLFWMouseButtonCallback prevMouse;
    private static GLFWKeyCallback prevKey;
    private static GLFWScrollCallback prevScroll;
    private static GLFWCursorPosCallback prevCursor;

    /** Set when the play key is pressed while a menu is open (keybinds do not fire inside menus). */
    public static volatile boolean stopRequested;

    public static void install(MinecraftClient mc) {
        if (installed || mc.getWindow() == null) return;
        installed = true;
        long win = mc.getWindow().getHandle();

        prevMouse = GLFW.glfwSetMouseButtonCallback(win, GuiInput::onMouse);
        prevKey = GLFW.glfwSetKeyCallback(win, GuiInput::onKey);
        prevScroll = GLFW.glfwSetScrollCallback(win, GuiInput::onScroll);

        // we only need a handle to the cursor callback, so put it straight back
        prevCursor = GLFW.glfwSetCursorPosCallback(win, null);
        GLFW.glfwSetCursorPosCallback(win, prevCursor);
    }

    // ================================================================ recording side

    private static void onMouse(long win, int button, int action, int mods) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (action != GLFW.GLFW_REPEAT && MacroEngine.canRecordGui(mc, true)) {
            float[] c = cursor(mc);
            MacroEngine.recordEvent(EV_MOUSE, button, action, mods, c[0], c[1]);
        }
        if (prevMouse != null) prevMouse.invoke(win, button, action, mods);
    }

    private static void onScroll(long win, double dx, double dy) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (MacroEngine.canRecordGui(mc, true)) {
            MacroEngine.recordEvent(EV_SCROLL, 0, 0, 0, (float) dx, (float) dy);
        }
        if (prevScroll != null) prevScroll.invoke(win, dx, dy);
    }

    private static void onKey(long win, int key, int scancode, int action, int mods) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (action != GLFW.GLFW_REPEAT) {
            if (MacroEngine.canRecordGui(mc, false) && isMenuKey(mc, key)) {
                MacroEngine.recordEvent(EV_KEY, key, action, mods, 0f, 0f);
            }
            if (action == GLFW.GLFW_PRESS && MacroEngine.state == MacroEngine.State.PLAYING
                    && mc.currentScreen != null && isBound(Keys.PLAY, key)) {
                stopRequested = true;
            }
        }
        if (prevKey != null) prevKey.invoke(win, key, scancode, action, mods);
    }

    /** Keys we record for menus: Escape and whatever "Open Inventory" is bound to (E by default). */
    private static boolean isMenuKey(MinecraftClient mc, int key) {
        return key == GLFW.GLFW_KEY_ESCAPE || isBound(mc.options.inventoryKey, key);
    }

    private static boolean isBound(net.minecraft.client.option.KeyBinding kb, int key) {
        InputUtil.Key bound = KeyBindingHelper.getBoundKeyOf(kb);
        return bound.getCategory() == InputUtil.Type.KEYSYM && bound.getCode() == key;
    }

    /** Current cursor position as a fraction (0..1) of the window. */
    public static float[] cursor(MinecraftClient mc) {
        long win = mc.getWindow().getHandle();
        double[] x = new double[1];
        double[] y = new double[1];
        int[] w = new int[1];
        int[] h = new int[1];
        GLFW.glfwGetCursorPos(win, x, y);
        GLFW.glfwGetWindowSize(win, w, h);
        return new float[]{(float) (x[0] / Math.max(1, w[0])), (float) (y[0] / Math.max(1, h[0]))};
    }

    // ================================================================ playback side

    public static void moveCursor(MinecraftClient mc, float fx, float fy) {
        long win = mc.getWindow().getHandle();
        int[] w = new int[1];
        int[] h = new int[1];
        GLFW.glfwGetWindowSize(win, w, h);
        double x = fx * w[0];
        double y = fy * h[0];
        GLFW.glfwSetCursorPos(win, x, y);
        if (prevCursor != null) prevCursor.invoke(win, x, y);
    }

    public static void sendMouse(MinecraftClient mc, int button, int action, int mods, float fx, float fy) {
        moveCursor(mc, fx, fy);
        if (prevMouse != null) prevMouse.invoke(mc.getWindow().getHandle(), button, action, mods);
    }

    public static void sendKey(MinecraftClient mc, int key, int action, int mods) {
        if (prevKey != null) {
            prevKey.invoke(mc.getWindow().getHandle(), key, GLFW.glfwGetKeyScancode(key), action, mods);
        }
    }

    public static void sendScroll(MinecraftClient mc, float dx, float dy) {
        if (prevScroll != null) prevScroll.invoke(mc.getWindow().getHandle(), (double) dx, (double) dy);
    }
}
