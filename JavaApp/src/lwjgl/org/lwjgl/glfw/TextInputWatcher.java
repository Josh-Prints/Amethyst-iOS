package org.lwjgl.glfw;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Emerald: tells the launcher when the chat is open, so the iOS keyboard can
 * open on its own and the game can be shifted above it. Only the chat screen
 * counts; other text boxes (signs, search, world names) leave the keyboard alone.
 *
 * On unobfuscated versions (26.1+) we peek at the open screen once every few
 * frames. MC 26.3+ (SDL path) also reports where the focused text box is via
 * SDL_SetTextInputArea, which positions the shift. Obfuscated versions do nothing.
 */
public final class TextInputWatcher {
    private TextInputWatcher() {}

    private static native void nativeSetTextInput(boolean active, float areaTop, float areaBottom);

    private static boolean nativeOk = true;
    private static boolean active;
    private static float areaTop = -1f, areaBottom = -1f;

    public static synchronized void set(boolean nowActive, float top, float bottom) {
        if (nowActive == active && top == areaTop && bottom == areaBottom) return;
        active = nowActive;
        areaTop = top;
        areaBottom = bottom;
        if (!nativeOk) return;
        try {
            nativeSetTextInput(nowActive, top, bottom);
        } catch (Throwable t) {
            nativeOk = false;
        }
    }

    // ---- SDL path ----

    // Whether MC itself has text input switched on (SDL path); reported back to
    // MC through SDL_TextInputActive. The keyboard follows the chat screen only.
    private static volatile boolean sdlTextInput;
    private static float sdlTop = -1f, sdlBottom = -1f;

    public static void sdlStart() {
        sdlTextInput = true;
    }

    public static void sdlStop() {
        sdlTextInput = false;
        sdlTop = sdlBottom = -1f;
    }

    public static void sdlArea(int y, int h, int windowHeight) {
        if (windowHeight <= 0) return;
        sdlTop = y / (float) windowHeight;
        sdlBottom = (y + h) / (float) windowHeight;
        if (active) set(true, sdlTop, sdlBottom);
    }

    public static boolean isActive() {
        return sdlTextInput;
    }

    // ---- GLFW path: reflection poll ----

    private static boolean pollBroken;
    private static int frame;
    private static Object mcInstance;
    private static Field screenField, guiField;
    private static Method guiScreenMethod;
    public static void poll() {
        if (pollBroken) return;
        if ((++frame & 7) != 0) return;
        try {
            if (mcInstance == null && !init()) return;
            boolean chat = isChatScreen(currentScreen());
            set(chat, chat ? sdlTop : -1f, chat ? sdlBottom : -1f);
        } catch (Throwable t) {
            pollBroken = true;
        }
    }

    /** ChatScreen, and InBedChatScreen (chat while sleeping). */
    private static boolean isChatScreen(Object screen) {
        if (screen == null) return false;
        for (Class<?> c = screen.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            if (c.getName().endsWith(".ChatScreen")) return true;
        }
        return false;
    }

    private static Object currentScreen() throws Exception {
        if (screenField != null) return screenField.get(mcInstance);
        Object gui = guiField.get(mcInstance);
        return gui == null ? null : guiScreenMethod.invoke(gui);
    }

    private static Class<?> find(String name) {
        ClassLoader[] loaders = {
            Thread.currentThread().getContextClassLoader(),
            ClassLoader.getSystemClassLoader()
        };
        for (ClassLoader cl : loaders) {
            if (cl == null) continue;
            try {
                return Class.forName(name, false, cl);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static boolean init() throws Exception {
        Class<?> mc = find("net.minecraft.client.Minecraft");
        if (mc == null) {
            // Obfuscated version, or MC not loaded yet. Give it a while.
            if (frame > 8 * 600) pollBroken = true;
            return false;
        }
        Object inst = mc.getMethod("getInstance").invoke(null);
        if (inst == null) return false;
        try {
            screenField = mc.getField("screen");
        } catch (NoSuchFieldException e) {
            // 26.2+: the screen lives on Gui
            guiField = mc.getField("gui");
            guiScreenMethod = guiField.getType().getMethod("screen");
        }
        mcInstance = inst;
        return true;
    }
}
