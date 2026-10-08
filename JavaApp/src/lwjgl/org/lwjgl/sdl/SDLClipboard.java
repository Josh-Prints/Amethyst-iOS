/*
 * Amethyst SDL3 shim: replaces the LWJGL binding of the same name, which would
 * need a real libSDL3. See PojavSDL.
 */
package org.lwjgl.sdl;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.lwjgl.glfw.CallbackBridge;

import static org.lwjgl.system.MemoryUtil.*;

public class SDLClipboard {
    protected SDLClipboard() {}

    public static String SDL_GetClipboardText() {
        String s = CallbackBridge.nativeClipboard(CallbackBridge.CLIPBOARD_PASTE, null);
        return s == null ? "" : s;
    }

    public static boolean SDL_SetClipboardText(CharSequence text) {
        CallbackBridge.nativeClipboard(CallbackBridge.CLIPBOARD_COPY, text.toString().getBytes(StandardCharsets.UTF_8));
        return true;
    }

    public static boolean SDL_SetClipboardText(ByteBuffer text) {
        return SDL_SetClipboardText(memUTF8(text));
    }

    public static boolean SDL_HasClipboardText() {
        return !SDL_GetClipboardText().isEmpty();
    }
}
