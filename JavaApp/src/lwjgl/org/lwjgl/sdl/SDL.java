/*
 * Amethyst SDL3 shim: replaces the LWJGL binding of the same name, which would
 * need a real libSDL3. See PojavSDL.
 */
package org.lwjgl.sdl;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.SharedLibrary;

public final class SDL {
    private SDL() {}

    public static SharedLibrary getLibrary() {
        return GLFW.getLibrary();
    }
}
