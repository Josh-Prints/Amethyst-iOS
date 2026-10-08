/*
 * Amethyst SDL3 shim: replaces the LWJGL binding of the same name, which would
 * need a real libSDL3. See PojavSDL.
 */
package org.lwjgl.sdl;

public class SDLEvents {
    protected SDLEvents() {}

    public static void SDL_PumpEvents() {
        PojavSDL.pump();
    }

    public static boolean SDL_PollEvent(SDL_Event event) {
        return PojavSDL.poll(event);
    }

    public static void SDL_FlushEvents(int minType, int maxType) {
        PojavSDL.flush(minType, maxType);
    }

    public static void SDL_FlushEvent(int type) {
        PojavSDL.flush(type, type);
    }

    // The event struct may already be reused by the time MC asks (it defers
    // the lookup), so don't read it: there is only one real window.
    public static long SDL_GetWindowFromEvent(SDL_Event event) {
        return PojavSDL.mainWindow;
    }
}
