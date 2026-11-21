package com.primitive;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/** Small status badge in the top-left corner while recording or playing. */
public final class Hud {
    private Hud() {}

    private static final int BORDER = 0xFFE6E6EA;
    private static final int RED = 0xFFFF4D4D;
    private static final int GREEN = 0xFF39FF6A;

    public static void render(DrawContext c, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!Store.settings.hud || mc.player == null || mc.options.hudHidden) return;

        MacroEngine.State st = MacroEngine.state;
        if (st == MacroEngine.State.IDLE) return;

        TextRenderer tr = mc.textRenderer;
        String line;
        int accent;
        boolean dotOn;

        if (st == MacroEngine.State.RECORDING) {
            int secs = MacroEngine.recordedTicks() / 20;
            line = "REC  " + secs / 60 + ":" + String.format("%02d", secs % 60);
            accent = RED;
            dotOn = (System.currentTimeMillis() / 500) % 2 == 0;
        } else {
            Macro m = MacroEngine.playing();
            if (m == null) return;
            int pct = (int) (100f * MacroEngine.playIndex() / Math.max(1, m.length()));
            line = "PLAY  " + tr.trimToWidth(m.name, 90) + "  " + pct + "%";
            accent = GREEN;
            dotOn = true;
        }

        int x = 6, y = 6;
        int w = tr.getWidth(line) + 22;
        int h = 16;

        c.fill(x, y, x + w, y + h, 0xC00B0B10);
        c.fill(x + 1, y, x + w - 1, y + 1, BORDER);
        c.fill(x + 1, y + h - 1, x + w - 1, y + h, BORDER);
        c.fill(x, y + 1, x + 1, y + h - 1, BORDER);
        c.fill(x + w - 1, y + 1, x + w, y + h - 1, BORDER);

        if (dotOn) c.fill(x + 6, y + 5, x + 12, y + 11, accent);
        c.drawText(tr, line, x + 17, y + 4, 0xFFFFFFFF, false);
    }
}
