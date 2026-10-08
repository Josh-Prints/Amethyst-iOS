/*
 * Amethyst SDL3 shim: replaces the LWJGL binding of the same name, which would
 * need a real libSDL3. See PojavSDL.
 */
package org.lwjgl.sdl;

public class SDLPixels {
    protected SDLPixels() {}

    public static SDL_PixelFormatDetails SDL_GetPixelFormatDetails(int format) {
        return PojavSDL.formatDetails();
    }
}
