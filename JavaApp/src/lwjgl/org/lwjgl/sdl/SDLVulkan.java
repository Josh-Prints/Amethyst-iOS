/*
 * Amethyst SDL3 shim: replaces the LWJGL binding of the same name, which would
 * need a real libSDL3. See PojavSDL.
 */
package org.lwjgl.sdl;

import java.nio.LongBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;

// Only the OpenGL path is wired up; MC falls back to OpenGL when this fails.
public class SDLVulkan {
    protected SDLVulkan() {}

    public static boolean SDL_Vulkan_LoadLibrary(CharSequence path) {
        PojavSDL.lastError = "Vulkan is not supported by Amethyst's SDL shim";
        return false;
    }

    public static void SDL_Vulkan_UnloadLibrary() {}

    public static long SDL_Vulkan_GetVkGetInstanceProcAddr() {
        return 0L;
    }

    public static PointerBuffer SDL_Vulkan_GetInstanceExtensions() {
        return null;
    }

    public static boolean SDL_Vulkan_CreateSurface(long window, VkInstance instance, VkAllocationCallbacks allocator, LongBuffer surface) {
        PojavSDL.lastError = "Vulkan is not supported by Amethyst's SDL shim";
        return false;
    }

    public static boolean SDL_Vulkan_GetPresentationSupport(VkInstance instance, VkPhysicalDevice physicalDevice, int queueFamilyIndex) {
        return false;
    }
}
