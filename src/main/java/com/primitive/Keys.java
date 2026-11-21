package com.primitive;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * All Primitive keybinds. They are real Minecraft keybinds, so they show up in
 * Options > Controls > Key Binds under "Primitive" AND can be changed inside the Primitive GUI.
 */
public final class Keys {
    private Keys() {}

    public static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("primitive", "main"));

    public static final KeyBinding OPEN_GUI   = reg("gui",        GLFW.GLFW_KEY_RIGHT_SHIFT);
    public static final KeyBinding RECORD     = reg("record",     GLFW.GLFW_KEY_R);
    public static final KeyBinding RECORD_MOD = reg("record_mod", GLFW.GLFW_KEY_UNKNOWN); // optional 2nd key
    public static final KeyBinding PLAY       = reg("play",       GLFW.GLFW_KEY_G);
    public static final KeyBinding PLAY_MOD   = reg("play_mod",   GLFW.GLFW_KEY_UNKNOWN); // optional 2nd key

    private static KeyBinding reg(String id, int defaultKey) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.primitive." + id, InputUtil.Type.KEYSYM, defaultKey, CATEGORY));
    }

    /** Forces the class to load (and therefore register the keybinds). */
    public static void init() {}

    /** Rebind from the Primitive GUI. Also updates the Controls screen and options.txt. */
    public static void rebind(KeyBinding kb, InputUtil.Key key) {
        kb.setBoundKey(key);
        KeyBinding.updateKeysByCode();
        MinecraftClient.getInstance().options.write();
    }
}
