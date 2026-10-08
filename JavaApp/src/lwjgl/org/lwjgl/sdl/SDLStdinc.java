/*
 * Amethyst SDL3 shim: replaces the LWJGL binding of the same name, which would
 * need a real libSDL3. See PojavSDL.
 */
package org.lwjgl.sdl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.PointerBuffer;

import static org.lwjgl.system.MemoryUtil.*;

public class SDLStdinc {
    protected SDLStdinc() {}

    public static void nSDL_free(long mem) {
        nmemFree(mem);
    }

    public static void SDL_free(ByteBuffer mem) {
        memFree(mem);
    }

    public static void SDL_free(IntBuffer mem) {
        memFree(mem);
    }

    public static void SDL_free(PointerBuffer mem) {
        if (mem != null) nmemFree(mem.address());
    }
}
