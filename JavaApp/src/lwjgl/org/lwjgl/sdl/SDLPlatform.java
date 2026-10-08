/*
 * Amethyst SDL3 shim: replaces the LWJGL binding of the same name, which would
 * need a real libSDL3. See PojavSDL.
 */
package org.lwjgl.sdl;

public class SDLPlatform {
    protected SDLPlatform() {}

    public static String SDL_GetPlatform() {
        return "iOS";
    }
}
