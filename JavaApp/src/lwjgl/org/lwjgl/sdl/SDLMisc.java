/*
 * Amethyst SDL3 shim: replaces the LWJGL binding of the same name, which would
 * need a real libSDL3. See PojavSDL.
 */
package org.lwjgl.sdl;

public class SDLMisc {
    protected SDLMisc() {}

    // Amethyst hooks fork/exec of "open" and routes it to UIApplication openURL.
    public static boolean SDL_OpenURL(CharSequence url) {
        try {
            new ProcessBuilder("open", url.toString()).start();
            return true;
        } catch (Throwable t) {
            PojavSDL.lastError = String.valueOf(t);
            return false;
        }
    }
}
