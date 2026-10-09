package org.lwjgl.glfw;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Emerald: tells the launcher when Minecraft wants text typed, so the iOS
 * keyboard can open on its own and the game can be shifted above it.
 *
 * MC 26.3+ (SDL path) reports this itself via SDL_StartTextInput /
 * SDL_StopTextInput / SDL_SetTextInputArea. For GLFW versions there is no such
 * signal, so on unobfuscated versions (26.1+) we peek at the open screen and its
 * focused widget once every few frames. Obfuscated versions just do nothing.
 */
public final class TextInputWatcher {
    private TextInputWatcher() {}

    private static native void nativeSetTextInput(boolean active, float areaTop, float areaBottom);

    private static boolean nativeOk = true;
    private static boolean active;
    private static float areaTop = -1f, areaBottom = -1f;
    /** Set once MC drives text input itself (SDL path); disables polling. */
    private static volatile boolean explicitSignals;

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

    public static void sdlStart() {
        explicitSignals = true;
        set(true, areaTop, areaBottom);
    }

    public static void sdlStop() {
        explicitSignals = true;
        set(false, -1f, -1f);
    }

    public static void sdlArea(int y, int h, int windowHeight) {
        explicitSignals = true;
        if (windowHeight <= 0) return;
        set(active, y / (float) windowHeight, (y + h) / (float) windowHeight);
    }

    public static boolean isActive() {
        return active;
    }

    // ---- GLFW path: reflection poll ----

    private static boolean pollBroken;
    private static int frame;
    private static Object mcInstance;
    private static Field screenField, guiField;
    private static Method guiScreenMethod;
    private static Class<?> containerClass, editBoxClass, multiEditBoxClass;
    private static Method getFocusedMethod;

    public static void poll() {
        if (explicitSignals || pollBroken) return;
        if ((++frame & 7) != 0) return;
        try {
            if (mcInstance == null && !init()) return;
            Object screen = currentScreen();
            boolean typing = false;
            if (screen != null) {
                String name = screen.getClass().getName();
                if (name.endsWith("SignEditScreen") || name.endsWith("BookEditScreen")) {
                    typing = true;
                } else {
                    Object focused = screen;
                    for (int depth = 0; depth < 8 && focused != null && containerClass.isInstance(focused); depth++) {
                        focused = getFocusedMethod.invoke(focused);
                        if (isTextBox(focused)) {
                            typing = true;
                            break;
                        }
                    }
                }
            }
            set(typing, -1f, -1f);
        } catch (Throwable t) {
            pollBroken = true;
        }
    }

    private static Object currentScreen() throws Exception {
        if (screenField != null) return screenField.get(mcInstance);
        Object gui = guiField.get(mcInstance);
        return gui == null ? null : guiScreenMethod.invoke(gui);
    }

    private static boolean isTextBox(Object o) {
        if (o == null) return false;
        return (editBoxClass != null && editBoxClass.isInstance(o))
            || (multiEditBoxClass != null && multiEditBoxClass.isInstance(o));
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
        containerClass = find("net.minecraft.client.gui.components.events.ContainerEventHandler");
        editBoxClass = find("net.minecraft.client.gui.components.EditBox");
        multiEditBoxClass = find("net.minecraft.client.gui.components.MultiLineEditBox");
        if (containerClass == null || editBoxClass == null) {
            pollBroken = true;
            return false;
        }
        getFocusedMethod = containerClass.getMethod("getFocused");
        mcInstance = inst;
        return true;
    }
}
