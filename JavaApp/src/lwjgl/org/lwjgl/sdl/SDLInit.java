/*
 * Amethyst SDL3 shim: replaces the LWJGL binding of the same name, which would
 * need a real libSDL3. See PojavSDL.
 */
package org.lwjgl.sdl;

public class SDLInit {
    protected SDLInit() {}

    public static boolean SDL_Init(int flags) {
        return PojavSDL.init();
    }

    public static boolean SDL_InitSubSystem(int flags) {
        return PojavSDL.init();
    }

    public static void SDL_QuitSubSystem(int flags) {}

    public static int SDL_WasInit(int flags) {
        return flags;
    }

    public static void SDL_Quit() {
        PojavSDL.quit();
    }

    public static boolean SDL_SetAppMetadataProperty(CharSequence name, CharSequence value) {
        return true;
    }

    public static boolean SDL_SetAppMetadata(CharSequence appname, CharSequence appversion, CharSequence appidentifier) {
        return true;
    }
}
