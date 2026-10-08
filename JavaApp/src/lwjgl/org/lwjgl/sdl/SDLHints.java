/*
 * Amethyst SDL3 shim: replaces the LWJGL binding of the same name, which would
 * need a real libSDL3. See PojavSDL.
 */
package org.lwjgl.sdl;

import java.nio.ByteBuffer;

public class SDLHints {
    protected SDLHints() {}

    public static boolean SDL_SetHint(CharSequence name, CharSequence value) {
        return true;
    }

    public static boolean SDL_SetHint(ByteBuffer name, ByteBuffer value) {
        return true;
    }

    public static boolean SDL_SetHintWithPriority(CharSequence name, CharSequence value, int priority) {
        return true;
    }

    public static String SDL_GetHint(CharSequence name) {
        return null;
    }
}
