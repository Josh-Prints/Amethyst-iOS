/*
 * Amethyst SDL3 shim: replaces the LWJGL binding of the same name, which would
 * need a real libSDL3. See PojavSDL.
 */
package org.lwjgl.sdl;

import java.nio.FloatBuffer;

public class SDLMouse {
    protected SDLMouse() {}

    public static int SDL_GetMouseState(FloatBuffer x, FloatBuffer y) {
        if (x != null) x.put(x.position(), PojavSDL.mouseX);
        if (y != null) y.put(y.position(), PojavSDL.mouseY);
        return 0;
    }

    public static int SDL_GetGlobalMouseState(FloatBuffer x, FloatBuffer y) {
        return SDL_GetMouseState(x, y);
    }

    public static int SDL_GetRelativeMouseState(FloatBuffer x, FloatBuffer y) {
        if (x != null) x.put(x.position(), 0f);
        if (y != null) y.put(y.position(), 0f);
        return 0;
    }

    public static void SDL_WarpMouseInWindow(long window, float x, float y) {
        PojavSDL.warpMouse(x, y);
    }

    public static boolean SDL_WarpMouseGlobal(float x, float y) {
        PojavSDL.warpMouse(x, y);
        return true;
    }

    public static boolean SDL_SetWindowRelativeMouseMode(long window, boolean enabled) {
        PojavSDL.setRelativeMouse(enabled);
        return true;
    }

    public static boolean SDL_CaptureMouse(boolean enabled) {
        return true;
    }

    public static long SDL_CreateSystemCursor(int id) {
        return 1L;
    }

    public static long SDL_GetDefaultCursor() {
        return 1L;
    }

    public static long SDL_GetCursor() {
        return 1L;
    }

    public static boolean SDL_SetCursor(long cursor) {
        return true;
    }

    public static void SDL_DestroyCursor(long cursor) {}

    public static boolean SDL_ShowCursor() {
        return true;
    }

    public static boolean SDL_HideCursor() {
        return true;
    }

    public static boolean SDL_CursorVisible() {
        return true;
    }

    public static boolean SDL_HasMouse() {
        return true;
    }
}
