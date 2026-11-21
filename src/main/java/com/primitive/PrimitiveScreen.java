package com.primitive;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Four bordered panels like the reference screenshot:
 * MACRO (record / play), MACRO SAVES (select / rename / delete), KEYBINDS, SETTINGS.
 * Everything is drawn by hand; clickable areas are collected while drawing ("hits").
 */
public class PrimitiveScreen extends Screen {
    // palette
    private static final int GREEN = 0xFF39FF6A;
    private static final int GREEN_DIM = 0xFF1B7A38;
    private static final int BLUE = 0xFF3FA9FF;
    private static final int RED = 0xFFFF5555;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFF9A9AA3;
    private static final int DIM = 0xFF5C5C66;
    private static final int PANEL = 0xEE0B0B10;
    private static final int BORDER = 0xFFE6E6EA;
    private static final int HOVER = 0x26FFFFFF;
    private static final int TRACK = 0xFF34343C;

    private static final int ROW = 14;
    private static final int HEAD = 20;
    private static final int GAP = 8;

    private record Hit(int x, int y, int w, int h, Runnable left, Runnable right) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final List<Hit> hits = new ArrayList<>();

    private KeyBinding listening;            // keybind waiting for a key press
    private boolean renaming;
    private final StringBuilder renameBuf = new StringBuilder();
    private int scroll;
    private int visibleRows = 6;
    private int listX, listY, listW;         // for mouse-wheel hit testing
    private String armedId = "";             // delete needs two clicks
    private long armedAt;

    public PrimitiveScreen() {
        super(Text.literal("Primitive"));
    }

    // ================================================================ rendering

    /** Plain dim backdrop (no vanilla blur, which can only run once per frame). */
    @Override
    public void renderBackground(DrawContext c, int mouseX, int mouseY, float delta) {
        c.fillGradient(0, 0, width, height, 0xC0050806, 0xE0020E06);
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        hits.clear();
        renderBackground(c, mx, my, delta);

        float scale = Math.max(2f, Math.min(4f, width / 150f));
        drawTitle(c, scale);

        int top = 12 + (int) (9 * scale) + 18;
        float unit = Math.min(122f, (width - GAP * 5) / 4.6f);
        int w1 = (int) unit;
        int w2 = (int) (unit * 1.45f);
        int w3 = (int) (unit * 1.2f);
        int w4 = (int) unit;
        int x = (width - (w1 + w2 + w3 + w4 + GAP * 3)) / 2;

        visibleRows = Math.max(3, Math.min(8, (height - top - HEAD - 62) / ROW));

        macroPanel(c, x, top, w1, mx, my);
        savesPanel(c, x + w1 + GAP, top, w2, mx, my);
        keysPanel(c, x + w1 + w2 + GAP * 2, top, w3, mx, my);
        settingsPanel(c, x + w1 + w2 + w3 + GAP * 3, top, w4, mx, my);

        center(c, "LEFT-CLICK A KEYBIND TO CHANGE IT  -  RIGHT-CLICK TO CLEAR IT", width / 2, height - 24, DIM);
        center(c, "THE '+' KEYS ARE OPTIONAL: HOLD IT, THEN PRESS THE MAIN KEY", width / 2, height - 13, DIM);
        logo(c, width - 30, height - 30);
    }

    private void drawTitle(DrawContext c, float scale) {
        Text t = Text.literal("- Primitive -").formatted(Formatting.BOLD);
        int tw = textRenderer.getWidth(t);
        c.getMatrices().pushMatrix();
        c.getMatrices().translate(width / 2f, 12f);
        c.getMatrices().scale(scale, scale);
        int x = -tw / 2;
        int[][] glow = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}, {-1, -1}, {1, -1}, {-1, 1}, {1, 1}};
        for (int[] d : glow) {
            c.drawText(textRenderer, t, x + d[0], d[1], 0x2839FF6A, false);
        }
        c.drawText(textRenderer, t, x, 0, GREEN, false);
        c.getMatrices().popMatrix();
    }

    // ---------------------------------------------------------------- MACRO panel

    private void macroPanel(DrawContext c, int x, int y, int w, int mx, int my) {
        boolean rec = MacroEngine.state == MacroEngine.State.RECORDING;
        boolean play = MacroEngine.state == MacroEngine.State.PLAYING;

        panel(c, x, y, w, HEAD + 114, "MACRO");
        int ry = y + HEAD;

        toggleRow(c, x, ry, w, "RECORD", rec, mx, my, () -> {
            MacroEngine.toggleRecording(client);
            if (MacroEngine.state == MacroEngine.State.RECORDING) close(); // back to the game so it can record
        });
        ry += ROW;

        toggleRow(c, x, ry, w, "PLAY", play, mx, my, () -> {
            if (play) {
                MacroEngine.stopPlayback(client, "Macro stopped");
            } else {
                MacroEngine.play(Store.selected());
                if (MacroEngine.state == MacroEngine.State.PLAYING) close();
            }
        });
        ry += ROW + 4;

        c.fill(x + 8, ry, x + w - 8, ry + 1, 0x33FFFFFF);
        ry += 5;

        String status;
        int statusColor;
        if (rec) {
            status = "REC " + MacroEngine.recordedTicks() + "T";
            statusColor = RED;
        } else if (play) {
            Macro m = MacroEngine.playing();
            int pct = m == null ? 0 : (int) (100f * MacroEngine.playIndex() / Math.max(1, m.length()));
            status = "PLAY " + pct + "%";
            statusColor = GREEN;
        } else {
            status = "IDLE";
            statusColor = WHITE;
        }
        infoRow(c, x, ry, w, "STATUS", status, statusColor);
        ry += ROW;
        infoRow(c, x, ry, w, "SAVED", String.valueOf(Store.MACROS.size()), WHITE);
        ry += ROW;
        Macro sel = Store.selected();
        infoRow(c, x, ry, w, "ACTIVE", sel == null ? "NONE" : sel.name, sel == null ? DIM : BLUE);
        ry += ROW;
        infoRow(c, x, ry, w, "CHAT", sel == null ? "-" : String.valueOf(sel.chatText.length), sel == null ? DIM : WHITE);
        ry += ROW;
        infoRow(c, x, ry, w, "CLICKS", sel == null ? "-" : String.valueOf(sel.clicks()), sel == null ? DIM : WHITE);
    }

    // ---------------------------------------------------------------- MACRO SAVES panel

    private void savesPanel(DrawContext c, int x, int y, int w, int mx, int my) {
        int n = Store.MACROS.size();
        int rows = visibleRows;
        panel(c, x, y, w, HEAD + rows * ROW + 6 + 14 + 6, "MACRO SAVES");

        scroll = Math.max(0, Math.min(scroll, Math.max(0, n - rows)));
        listX = x;
        listY = y + HEAD;
        listW = w;

        Macro sel = Store.selected();
        boolean bar = n > rows;
        int rw = w - 4 - (bar ? 4 : 0);

        if (n == 0) {
            center(c, "NO MACROS YET", x + w / 2, y + HEAD + 10, GRAY);
            center(c, "PRESS RECORD", x + w / 2, y + HEAD + 24, DIM);
        }

        for (int i = 0; i < rows && scroll + i < n; i++) {
            Macro m = Store.MACROS.get(scroll + i);
            int ry = y + HEAD + i * ROW;
            boolean isSel = m == sel;

            if (inside(mx, my, x + 2, ry, rw, ROW)) c.fill(x + 2, ry, x + 2 + rw, ry + ROW, HOVER);
            if (isSel) {
                c.fill(x + 2, ry, x + 2 + rw, ry + ROW, 0x183FA9FF);
                c.fill(x + 3, ry + 1, x + 5, ry + ROW - 1, BLUE);
            }

            if (isSel && renaming) {
                c.fill(x + 7, ry + 1, x + 2 + rw - 2, ry + ROW - 1, 0xFF14141A);
                outline(c, x + 7, ry + 1, rw - 7, ROW - 2, GREEN);
                String shown = renameBuf.toString();
                while (shown.length() > 1 && textRenderer.getWidth(shown + "_") > rw - 22) {
                    shown = shown.substring(1);
                }
                boolean caret = (System.currentTimeMillis() / 450) % 2 == 0;
                c.drawText(textRenderer, shown + (caret ? "_" : ""), x + 11, ry + 3, WHITE, false);
            } else {
                String dur = m.duration();
                int dw = textRenderer.getWidth(dur);
                String nm = textRenderer.trimToWidth(m.name, rw - dw - 18);
                c.drawText(textRenderer, nm, x + 9, ry + 3, isSel ? WHITE : 0xFFC8C8D0, false);
                c.drawText(textRenderer, dur, x + 2 + rw - dw - 5, ry + 3, DIM, false);
            }

            hits.add(new Hit(x + 2, ry, rw, ROW, () -> {
                if (!renaming) Store.select(m);
            }, null));
        }

        if (bar) {
            int trackH = rows * ROW;
            c.fill(x + w - 6, y + HEAD, x + w - 4, y + HEAD + trackH, TRACK);
            int thumbH = Math.max(8, trackH * rows / n);
            int thumbY = y + HEAD + (trackH - thumbH) * scroll / Math.max(1, n - rows);
            c.fill(x + w - 6, thumbY, x + w - 4, thumbY + thumbH, GRAY);
        }

        // buttons
        int by = y + HEAD + rows * ROW + 6;
        boolean has = sel != null;

        if (renaming) {
            int bw = (w - 12 - 3) / 2;
            button(c, x + 6, by, bw, "SAVE", true, GREEN, mx, my, this::commitRename);
            button(c, x + 6 + bw + 3, by, bw, "CANCEL", true, RED, mx, my, () -> renaming = false);
        } else {
            int bw = (w - 12 - 6) / 3;
            button(c, x + 6, by, bw, "PLAY", has, GREEN, mx, my, () -> {
                MacroEngine.play(sel);
                if (MacroEngine.state == MacroEngine.State.PLAYING) close();
            });
            button(c, x + 6 + bw + 3, by, bw, "RENAME", has, WHITE, mx, my, () -> {
                listening = null;
                renaming = true;
                renameBuf.setLength(0);
                renameBuf.append(sel.name);
            });
            boolean armed = has && sel.id.equals(armedId) && System.currentTimeMillis() - armedAt < 3000;
            button(c, x + 6 + 2 * (bw + 3), by, bw, armed ? "SURE?" : "DELETE", has, RED, mx, my, () -> {
                if (armed) {
                    if (MacroEngine.playing() == sel) MacroEngine.stopPlayback(client, null);
                    Store.delete(sel);
                    armedId = "";
                } else {
                    armedId = sel.id;
                    armedAt = System.currentTimeMillis();
                }
            });
        }
    }

    private void commitRename() {
        Macro sel = Store.selected();
        if (sel != null && !renameBuf.toString().isBlank()) {
            Store.rename(sel, renameBuf.toString());
        }
        renaming = false;
    }

    // ---------------------------------------------------------------- KEYBINDS panel

    private void keysPanel(DrawContext c, int x, int y, int w, int mx, int my) {
        KeyBinding[] list = {Keys.OPEN_GUI, Keys.RECORD, Keys.RECORD_MOD, Keys.PLAY, Keys.PLAY_MOD};
        String[] names = {"OPEN GUI", "RECORD", "RECORD +", "PLAY", "PLAY +"};

        panel(c, x, y, w, HEAD + list.length * ROW + 6, "KEYBINDS");

        for (int i = 0; i < list.length; i++) {
            KeyBinding kb = list[i];
            int ry = y + HEAD + i * ROW;

            if (listening == kb) c.fill(x + 2, ry, x + w - 2, ry + ROW, 0x2239FF6A);
            else if (inside(mx, my, x + 2, ry, w - 4, ROW)) c.fill(x + 2, ry, x + w - 2, ry + ROW, HOVER);

            c.drawText(textRenderer, names[i], x + 8, ry + 3, WHITE, false);

            String v;
            int col;
            if (listening == kb) {
                v = "...";
                col = GREEN;
            } else if (kb.isUnbound()) {
                v = "NONE";
                col = DIM;
            } else {
                v = kb.getBoundKeyLocalizedText().getString().toUpperCase(Locale.ROOT)
                        .replace("RIGHT ", "R-").replace("LEFT ", "L-").replace("CONTROL", "CTRL");
                col = GREEN;
            }
            int avail = w - 16 - textRenderer.getWidth(names[i]) - 6;
            v = textRenderer.trimToWidth(v, Math.max(avail, 12));
            c.drawText(textRenderer, v, x + w - 8 - textRenderer.getWidth(v), ry + 3, col, false);

            hits.add(new Hit(x + 2, ry, w - 4, ROW,
                    () -> {
                        renaming = false;
                        listening = kb;
                    },
                    () -> Keys.rebind(kb, InputUtil.UNKNOWN_KEY)));
        }
    }

    // ---------------------------------------------------------------- SETTINGS panel

    private void settingsPanel(DrawContext c, int x, int y, int w, int mx, int my) {
        Store.Settings s = Store.settings;
        panel(c, x, y, w, HEAD + 3 * ROW + 6, "SETTINGS");
        int ry = y + HEAD;

        toggleRow(c, x, ry, w, "LOOP", s.loop, mx, my, () -> {
            s.loop = !s.loop;
            Store.saveSettings();
        });
        ry += ROW;
        toggleRow(c, x, ry, w, "REL AIM", s.relativeAim, mx, my, () -> {
            s.relativeAim = !s.relativeAim;
            Store.saveSettings();
        });
        ry += ROW;
        toggleRow(c, x, ry, w, "HUD", s.hud, mx, my, () -> {
            s.hud = !s.hud;
            Store.saveSettings();
        });
    }

    // ================================================================ drawing helpers

    private void panel(DrawContext c, int x, int y, int w, int h, String title) {
        c.fill(x, y, x + w, y + h, PANEL);
        c.fill(x + 1, y, x + w - 1, y + 1, BORDER);
        c.fill(x + 1, y + h - 1, x + w - 1, y + h, BORDER);
        c.fill(x, y + 1, x + 1, y + h - 1, BORDER);
        c.fill(x + w - 1, y + 1, x + w, y + h - 1, BORDER);
        center(c, title, x + w / 2, y + 6, WHITE);
        c.fill(x + 6, y + HEAD - 4, x + w - 6, y + HEAD - 3, 0x33FFFFFF);
    }

    private static void outline(DrawContext c, int x, int y, int w, int h, int color) {
        c.fill(x, y, x + w, y + 1, color);
        c.fill(x, y + h - 1, x + w, y + h, color);
        c.fill(x, y + 1, x + 1, y + h - 1, color);
        c.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private void toggleRow(DrawContext c, int x, int y, int w, String label, boolean on,
                           int mx, int my, Runnable click) {
        if (inside(mx, my, x + 2, y, w - 4, ROW)) c.fill(x + 2, y, x + w - 2, y + ROW, HOVER);
        c.drawText(textRenderer, label, x + 8, y + 3, WHITE, false);

        int px = x + w - 8 - 14;
        int py = y + 4;
        c.fill(px, py, px + 14, py + 7, on ? GREEN_DIM : TRACK);
        c.fill(on ? px + 8 : px + 1, py + 1, on ? px + 13 : px + 6, py + 6, on ? GREEN : GRAY);

        hits.add(new Hit(x + 2, y, w - 4, ROW, click, null));
    }

    private void infoRow(DrawContext c, int x, int y, int w, String label, String value, int color) {
        c.drawText(textRenderer, label, x + 8, y + 3, GRAY, false);
        int avail = w - 16 - textRenderer.getWidth(label) - 6;
        String v = textRenderer.trimToWidth(value, Math.max(avail, 10));
        c.drawText(textRenderer, v, x + w - 8 - textRenderer.getWidth(v), y + 3, color, false);
    }

    private void button(DrawContext c, int x, int y, int w, String label, boolean enabled, int accent,
                        int mx, int my, Runnable click) {
        boolean hover = enabled && inside(mx, my, x, y, w, 14);
        int edge = !enabled ? 0xFF3A3A42 : hover ? accent : 0xFF8A8A94;
        c.fill(x, y, x + w, y + 14, hover ? 0x33FFFFFF : 0x22000000);
        outline(c, x, y, w, 14, edge);
        center(c, label, x + w / 2, y + 3, !enabled ? DIM : hover ? accent : WHITE);
        if (enabled) hits.add(new Hit(x, y, w, 14, click, null));
    }

    private void center(DrawContext c, String s, int cx, int y, int color) {
        c.drawText(textRenderer, s, cx - textRenderer.getWidth(s) / 2, y, color, false);
    }

    /** A little green ring, our answer to the donut in the reference. */
    private void logo(DrawContext c, int cx, int cy) {
        int outerR = 13;
        int innerR = 7;
        for (int dy = -outerR; dy <= outerR; dy++) {
            int outer = (int) Math.floor(Math.sqrt(outerR * outerR - dy * dy));
            if (Math.abs(dy) < innerR) {
                int inner = (int) Math.ceil(Math.sqrt(innerR * innerR - dy * dy));
                c.fill(cx - outer, cy + dy, cx - inner + 1, cy + dy + 1, GREEN);
                c.fill(cx + inner, cy + dy, cx + outer + 1, cy + dy + 1, GREEN);
            } else {
                c.fill(cx - outer, cy + dy, cx + outer + 1, cy + dy + 1, GREEN);
            }
        }
        center(c, "P", cx, cy - 4, GREEN);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ================================================================ input

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int button = click.button();

        if (listening != null) {
            if (button >= 2) Keys.rebind(listening, InputUtil.Type.MOUSE.createFromCode(button));
            listening = null;
            return true;
        }

        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.contains(click.x(), click.y())) {
                Runnable r = button == 0 ? h.left() : button == 1 ? h.right() : null;
                if (r != null) {
                    r.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (inside(mx, my, listX, listY, listW, visibleRows * ROW)) {
            scroll -= (int) Math.signum(vertical);
            return true;
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();

        if (listening != null) {
            if (key != GLFW.GLFW_KEY_ESCAPE) {
                Keys.rebind(listening, InputUtil.Type.KEYSYM.createFromCode(key));
            }
            listening = null;
            return true;
        }

        if (renaming) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                renaming = false;
            } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                commitRename();
            } else if (key == GLFW.GLFW_KEY_BACKSPACE && renameBuf.length() > 0) {
                renameBuf.setLength(renameBuf.length() - 1);
            }
            return true;
        }

        // the GUI key also closes the GUI
        InputUtil.Key bound = KeyBindingHelper.getBoundKeyOf(Keys.OPEN_GUI);
        if (bound.getCategory() == InputUtil.Type.KEYSYM && bound.getCode() == key) {
            close();
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (renaming) {
            int cp = input.codepoint();
            if (cp >= 32 && cp != 127 && renameBuf.length() < 24) {
                renameBuf.appendCodePoint(cp);
            }
            return true;
        }
        return super.charTyped(input);
    }
}
