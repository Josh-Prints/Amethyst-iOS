/*
 * Amethyst SDL3 shim: replaces the LWJGL binding of the same name, which would
 * need a real libSDL3. See PojavSDL.
 */
package org.lwjgl.sdl;

public class SDLTimer {
    protected SDLTimer() {}

    public static long SDL_GetTicksNS() {
        return PojavSDL.ticksNS();
    }

    public static long SDL_GetTicks() {
        return PojavSDL.ticksNS() / 1000000L;
    }

    public static long SDL_GetPerformanceCounter() {
        return System.nanoTime();
    }

    public static long SDL_GetPerformanceFrequency() {
        return 1000000000L;
    }

    public static void SDL_Delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    public static void SDL_DelayNS(long ns) {
        try {
            Thread.sleep(ns / 1000000L, (int) (ns % 1000000L));
        } catch (InterruptedException ignored) {
        }
    }
}
