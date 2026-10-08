/*
 * Amethyst SDL3 shim: replaces the LWJGL binding of the same name, which would
 * need a real libSDL3. See PojavSDL.
 */
package org.lwjgl.sdl;

import java.nio.IntBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.FunctionProvider;

import static org.lwjgl.system.MemoryUtil.*;

public class SDLVideo {
    protected SDLVideo() {}

    // ---- drivers / displays ------------------------------------------------

    public static int SDL_GetNumVideoDrivers() {
        return 1;
    }

    public static String SDL_GetVideoDriver(int index) {
        return "uikit";
    }

    public static String SDL_GetCurrentVideoDriver() {
        return "uikit";
    }

    public static IntBuffer SDL_GetDisplays() {
        IntBuffer displays = memAllocInt(1);
        displays.put(0, PojavSDL.DISPLAY_ID);
        return displays;
    }

    public static int SDL_GetPrimaryDisplay() {
        return PojavSDL.DISPLAY_ID;
    }

    public static String SDL_GetDisplayName(int displayID) {
        return "iOS Display";
    }

    public static boolean SDL_GetDisplayBounds(int displayID, SDL_Rect rect) {
        rect.x(0).y(0).w(PojavSDL.width()).h(PojavSDL.height());
        return true;
    }

    public static boolean SDL_GetDisplayUsableBounds(int displayID, SDL_Rect rect) {
        return SDL_GetDisplayBounds(displayID, rect);
    }

    public static float SDL_GetDisplayContentScale(int displayID) {
        return 1f;
    }

    public static PointerBuffer SDL_GetFullscreenDisplayModes(int displayID) {
        PointerBuffer modes = memAllocPointer(1);
        modes.put(0, PojavSDL.displayMode().address());
        return modes;
    }

    public static boolean SDL_GetClosestFullscreenDisplayMode(int displayID, int w, int h, float refresh_rate, boolean include_high_density_modes, SDL_DisplayMode closest) {
        memCopy(PojavSDL.displayMode().address(), closest.address(), SDL_DisplayMode.SIZEOF);
        return true;
    }

    public static SDL_DisplayMode SDL_GetDesktopDisplayMode(int displayID) {
        return PojavSDL.displayMode();
    }

    public static SDL_DisplayMode SDL_GetCurrentDisplayMode(int displayID) {
        return PojavSDL.displayMode();
    }

    public static int SDL_GetDisplayForWindow(long window) {
        return PojavSDL.DISPLAY_ID;
    }

    public static int SDL_GetDisplayForPoint(SDL_Point point) {
        return PojavSDL.DISPLAY_ID;
    }

    public static int SDL_GetDisplayForRect(SDL_Rect rect) {
        return PojavSDL.DISPLAY_ID;
    }

    // ---- windows -------------------------------------------------------------

    public static long SDL_CreateWindow(CharSequence title, int w, int h, long flags) {
        return PojavSDL.createWindow(flags);
    }

    public static void SDL_DestroyWindow(long window) {
        PojavSDL.destroyWindow(window);
    }

    public static int SDL_GetWindowID(long window) {
        return (int) (window & 0xFFFFFFFFL);
    }

    public static long SDL_GetWindowFlags(long window) {
        PojavSDL.Window w = PojavSDL.window(window);
        return w == null ? 0L : w.flags;
    }

    public static float SDL_GetWindowPixelDensity(long window) {
        return 1f;
    }

    public static float SDL_GetWindowDisplayScale(long window) {
        return 1f;
    }

    public static boolean SDL_GetWindowPosition(long window, IntBuffer x, IntBuffer y) {
        if (x != null) x.put(x.position(), 0);
        if (y != null) y.put(y.position(), 0);
        return true;
    }

    public static boolean SDL_GetWindowSize(long window, IntBuffer w, IntBuffer h) {
        if (w != null) w.put(w.position(), PojavSDL.width());
        if (h != null) h.put(h.position(), PojavSDL.height());
        return true;
    }

    public static boolean SDL_GetWindowSizeInPixels(long window, IntBuffer w, IntBuffer h) {
        return SDL_GetWindowSize(window, w, h);
    }

    // The window always fills the screen; tell MC the real size back.
    public static boolean SDL_SetWindowSize(long window, int w, int h) {
        if (window == PojavSDL.mainWindow) PojavSDL.pushResize(PojavSDL.width(), PojavSDL.height());
        return true;
    }

    public static boolean SDL_SetWindowPosition(long window, int x, int y) {
        return true;
    }

    public static boolean SDL_SetWindowMinimumSize(long window, int min_w, int min_h) {
        return true;
    }

    public static boolean SDL_SetWindowMaximumSize(long window, int max_w, int max_h) {
        return true;
    }

    public static boolean SDL_SetWindowTitle(long window, CharSequence title) {
        return true;
    }

    public static String SDL_GetWindowTitle(long window) {
        return "Minecraft";
    }

    public static boolean SDL_SetWindowIcon(long window, SDL_Surface icon) {
        return true;
    }

    public static boolean SDL_SetWindowBordered(long window, boolean bordered) {
        return true;
    }

    public static boolean SDL_SetWindowResizable(long window, boolean resizable) {
        return true;
    }

    public static boolean SDL_SetWindowMouseGrab(long window, boolean grabbed) {
        return true;
    }

    public static boolean SDL_SetWindowKeyboardGrab(long window, boolean grabbed) {
        return true;
    }

    public static boolean SDL_ShowWindow(long window) {
        return true;
    }

    public static boolean SDL_HideWindow(long window) {
        return true;
    }

    public static boolean SDL_RaiseWindow(long window) {
        return true;
    }

    public static boolean SDL_MaximizeWindow(long window) {
        return true;
    }

    public static boolean SDL_MinimizeWindow(long window) {
        return true;
    }

    public static boolean SDL_RestoreWindow(long window) {
        return true;
    }

    public static boolean SDL_SyncWindow(long window) {
        return true;
    }

    public static boolean SDL_FlashWindow(long window, int operation) {
        return true;
    }

    public static boolean SDL_SetWindowFullscreen(long window, boolean fullscreen) {
        PojavSDL.setFullscreen(window, fullscreen);
        return true;
    }

    public static boolean SDL_SetWindowFullscreenMode(long window, SDL_DisplayMode mode) {
        PojavSDL.Window w = PojavSDL.window(window);
        if (w != null) w.exclusiveMode = mode != null;
        return true;
    }

    public static SDL_DisplayMode SDL_GetWindowFullscreenMode(long window) {
        PojavSDL.Window w = PojavSDL.window(window);
        return w != null && w.exclusiveMode ? PojavSDL.displayMode() : null;
    }

    public static boolean SDL_ScreenSaverEnabled() {
        return false;
    }

    public static boolean SDL_EnableScreenSaver() {
        return true;
    }

    public static boolean SDL_DisableScreenSaver() {
        return true;
    }

    // ---- OpenGL ----------------------------------------------------------------

    public static boolean SDL_GL_LoadLibrary(CharSequence path) {
        return true;
    }

    public static void SDL_GL_UnloadLibrary() {}

    // Must hand out exactly what LWJGL's GL function provider does: MC compares
    // the two for glGetError before trusting the context.
    public static long SDL_GL_GetProcAddress(CharSequence proc) {
        FunctionProvider provider = GL.getFunctionProvider();
        return provider == null ? NULL : provider.getFunctionAddress(proc);
    }

    public static boolean SDL_GL_ExtensionSupported(CharSequence extension) {
        return false;
    }

    public static void SDL_GL_ResetAttributes() {}

    public static boolean SDL_GL_SetAttribute(int attr, int value) {
        PojavSDL.setGLAttribute(attr, value);
        return true;
    }

    public static boolean SDL_GL_GetAttribute(int attr, IntBuffer value) {
        value.put(value.position(), PojavSDL.getGLAttribute(attr));
        return true;
    }

    public static long SDL_GL_CreateContext(long window) {
        return PojavSDL.createGLContext(window);
    }

    public static boolean SDL_GL_MakeCurrent(long window, long context) {
        return PojavSDL.makeCurrent(window, context);
    }

    public static long SDL_GL_GetCurrentWindow() {
        return PojavSDL.mainWindow;
    }

    public static long SDL_GL_GetCurrentContext() {
        return PojavSDL.glContext;
    }

    public static boolean SDL_GL_SetSwapInterval(int interval) {
        GLFW.glfwSwapInterval(interval);
        return true;
    }

    public static boolean SDL_GL_GetSwapInterval(IntBuffer interval) {
        interval.put(interval.position(), 1);
        return true;
    }

    public static boolean SDL_GL_SwapWindow(long window) {
        if (PojavSDL.glContext != 0) GLFW.glfwSwapBuffers(PojavSDL.glContext);
        return true;
    }

    public static boolean SDL_GL_DestroyContext(long context) {
        return true;
    }
}
