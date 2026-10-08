/*
 * Amethyst SDL3 shim: replaces the LWJGL binding of the same name, which would
 * need a real libSDL3. See PojavSDL.
 */
package org.lwjgl.sdl;

public class SDLLog {
    protected SDLLog() {}

    public static void SDL_SetLogOutputFunction(SDL_LogOutputFunctionI callback, long userdata) {}

    public static void SDL_SetLogPriorities(int priority) {}

    public static void SDL_SetLogPriority(int category, int priority) {}
}
