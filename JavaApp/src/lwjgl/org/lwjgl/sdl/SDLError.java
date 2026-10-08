/*
 * Amethyst SDL3 shim: replaces the LWJGL binding of the same name, which would
 * need a real libSDL3. See PojavSDL.
 */
package org.lwjgl.sdl;

public class SDLError {
    protected SDLError() {}

    public static String SDL_GetError() {
        return PojavSDL.lastError;
    }

    public static boolean SDL_ClearError() {
        PojavSDL.lastError = "";
        return true;
    }

    public static boolean SDL_SetError(CharSequence error) {
        PojavSDL.lastError = error.toString();
        return false;
    }
}
