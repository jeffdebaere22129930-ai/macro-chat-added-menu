package com.primitive;

import java.util.Locale;
import java.util.UUID;

/** One recorded macro: a snapshot of the player's inputs for every client tick (20 per second). */
public class Macro {
    public String id = UUID.randomUUID().toString();
    public String name = "Macro";
    public long created = System.currentTimeMillis();

    public int[] keys = new int[0];      // bitmask of pressed keys, one entry per tick
    public float[] yaw = new float[0];   // look direction per tick
    public float[] pitch = new float[0];
    public byte[] slot = new byte[0];    // selected hotbar slot per tick

    // chat messages / commands typed while recording: sent again at the same tick on playback
    public int[] chatTick = new int[0];
    public String[] chatText = new String[0]; // starts with "/" for commands

    // menu support: cursor position per tick (fraction of the window, -1 = no menu open that tick)
    public float[] cx = new float[0];
    public float[] cy = new float[0];

    // menu events (clicks, scrolling, ESC / inventory key), replayed at the same tick
    public int[] evTick = new int[0];
    public int[] evType = new int[0];    // 0 = mouse button, 1 = key, 2 = scroll
    public int[] evCode = new int[0];    // button or key code
    public int[] evAction = new int[0];  // 1 = press, 0 = release
    public int[] evMods = new int[0];
    public float[] evX = new float[0];   // cursor fraction (scroll: horizontal amount)
    public float[] evY = new float[0];   // cursor fraction (scroll: vertical amount)

    public int length() {
        return keys.length;
    }

    /** True for macros recorded with menu support (older macros have no cursor data). */
    public boolean hasGui() {
        return keys.length > 0 && cx.length == keys.length && cy.length == keys.length;
    }

    public int clicks() {
        int n = 0;
        for (int i = 0; i < evType.length; i++) {
            if (evType[i] == 0 && evAction[i] == 1) n++;
        }
        return n;
    }

    public boolean valid() {
        if (id == null || name == null || keys == null || yaw == null || pitch == null || slot == null) return false;
        if (keys.length != yaw.length || yaw.length != pitch.length || pitch.length != slot.length) return false;
        if (chatTick == null || chatText == null || chatTick.length != chatText.length) return false;
        if (cx == null || cy == null || cx.length != cy.length) return false;
        if (evTick == null || evType == null || evCode == null || evAction == null
                || evMods == null || evX == null || evY == null) return false;
        int e = evTick.length;
        return evType.length == e && evCode.length == e && evAction.length == e
                && evMods.length == e && evX.length == e && evY.length == e;
    }

    public String duration() {
        return String.format(Locale.ROOT, "%.1fs", length() / 20f);
    }
}
