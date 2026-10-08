/*
 * Amethyst SDL3 shim: replaces the LWJGL binding of the same name, which would
 * need a real libSDL3. See PojavSDL.
 */
package org.lwjgl.sdl;

import java.nio.IntBuffer;

public class SDLMessageBox {
    protected SDLMessageBox() {}

    public static boolean SDL_ShowSimpleMessageBox(int flags, CharSequence title, CharSequence message, long window) {
        System.err.println("[SDL MessageBox] " + title + ": " + message);
        return true;
    }

    public static boolean SDL_ShowMessageBox(SDL_MessageBoxData messageboxdata, IntBuffer buttonid) {
        PojavSDL.lastError = "Message boxes are not supported";
        return false;
    }
}
