/*
 * Amethyst SDL3 shim: replaces the LWJGL binding of the same name, which would
 * need a real libSDL3. See PojavSDL.
 */
package org.lwjgl.sdl;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.*;

public class SDLSurface {
    protected SDLSurface() {}

    public static SDL_Surface SDL_CreateSurfaceFrom(int width, int height, int format, ByteBuffer pixels, int pitch) {
        SDL_Surface s = SDL_Surface.calloc();
        long a = s.address();
        memPutInt(a + SDL_Surface.FORMAT, format);
        memPutInt(a + SDL_Surface.W, width);
        memPutInt(a + SDL_Surface.H, height);
        memPutInt(a + SDL_Surface.PITCH, pitch);
        memPutAddress(a + SDL_Surface.PIXELS, pixels == null ? NULL : memAddress(pixels));
        return s;
    }

    public static void SDL_DestroySurface(SDL_Surface surface) {
        if (surface != null) surface.free();
    }

    public static boolean SDL_AddSurfaceAlternateImage(SDL_Surface surface, SDL_Surface image) {
        return true;
    }
}
