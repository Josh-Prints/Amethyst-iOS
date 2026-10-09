/*
 * Amethyst SDL3 shim: replaces the LWJGL binding of the same name, which would
 * need a real libSDL3. See PojavSDL.
 */
package org.lwjgl.sdl;

import java.nio.ByteBuffer;

import org.lwjgl.glfw.TextInputWatcher;

public class SDLKeyboard {
    protected SDLKeyboard() {}

    public static ByteBuffer SDL_GetKeyboardState() {
        return PojavSDL.keyboardState;
    }

    public static short SDL_GetModState() {
        return PojavSDL.modState();
    }

    public static void SDL_SetModState(short modstate) {}

    public static int SDL_GetKeyFromScancode(int scancode, short modstate, boolean key_event) {
        return PojavSDL.scancodeToKeycode(scancode);
    }

    public static String SDL_GetKeyName(int key) {
        return PojavSDL.keyName(key);
    }

    public static String SDL_GetScancodeName(int scancode) {
        return PojavSDL.keyName(PojavSDL.scancodeToKeycode(scancode));
    }

    // Emerald: MC calls these when a text box gains/loses focus; the launcher
    // opens the iOS keyboard and lifts the game above it.
    public static boolean SDL_StartTextInput(long window) {
        TextInputWatcher.sdlStart();
        return true;
    }

    public static boolean SDL_StopTextInput(long window) {
        TextInputWatcher.sdlStop();
        return true;
    }

    public static boolean SDL_TextInputActive(long window) {
        return TextInputWatcher.isActive();
    }

    public static boolean SDL_ClearComposition(long window) {
        return true;
    }

    public static boolean SDL_SetTextInputArea(long window, SDL_Rect.Buffer rect, int cursor) {
        if (rect != null) TextInputWatcher.sdlArea(rect.y(), rect.h(), PojavSDL.height());
        return true;
    }

    public static boolean SDL_SetTextInputArea(long window, SDL_Rect rect, int cursor) {
        if (rect != null) TextInputWatcher.sdlArea(rect.y(), rect.h(), PojavSDL.height());
        return true;
    }

    public static boolean SDL_HasKeyboard() {
        return true;
    }

    public static boolean SDL_HasScreenKeyboardSupport() {
        return false;
    }
}
