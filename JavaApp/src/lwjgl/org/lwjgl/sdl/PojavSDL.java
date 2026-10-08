package org.lwjgl.sdl;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.lwjgl.glfw.CallbackBridge;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.system.MemoryUtil.*;

/**
 * Minecraft 26.3+ talks to SDL3 instead of GLFW. Amethyst has no real SDL, so the
 * org.lwjgl.sdl function classes MC uses are replaced with this shim, which runs on
 * top of the existing fake GLFW (context creation, swap, input queue, resize).
 * GLFW input callbacks are turned into queued SDL events for SDL_PollEvent.
 */
public final class PojavSDL {
    private PojavSDL() {}

    // SDL3 event types (SDL_events.h)
    static final int EVENT_QUIT = 0x100;
    static final int EVENT_WINDOW_SHOWN = 514;
    static final int EVENT_WINDOW_RESIZED = 518;
    static final int EVENT_WINDOW_PIXEL_SIZE_CHANGED = 519;
    static final int EVENT_WINDOW_MOUSE_ENTER = 524;
    static final int EVENT_WINDOW_FOCUS_GAINED = 526;
    static final int EVENT_WINDOW_ENTER_FULLSCREEN = 535;
    static final int EVENT_WINDOW_LEAVE_FULLSCREEN = 536;
    static final int EVENT_KEY_DOWN = 768;
    static final int EVENT_KEY_UP = 769;
    static final int EVENT_TEXT_INPUT = 771;
    static final int EVENT_MOUSE_MOTION = 1024;
    static final int EVENT_MOUSE_BUTTON_DOWN = 1025;
    static final int EVENT_MOUSE_BUTTON_UP = 1026;
    static final int EVENT_MOUSE_WHEEL = 1027;

    static final long WINDOW_FULLSCREEN = 0x1L;
    static final long WINDOW_HIDDEN = 0x8L;
    static final long WINDOW_INPUT_FOCUS = 0x200L;
    static final long WINDOW_MOUSE_FOCUS = 0x400L;

    static final int DISPLAY_ID = 1;
    static final int PIXELFORMAT_XRGB8888 = 0x16161804;
    static final int NUM_SCANCODES = 512;

    static final class Window {
        final long handle;
        long flags;
        boolean exclusiveMode;
        Window(long handle, long flags) {
            this.handle = handle;
            this.flags = flags;
        }
    }

    static final class Event {
        int type;
        int i1, i2, i3;
        float f1, f2, f3, f4;
        boolean b1;
        String text;
    }

    private static final Map<Long, Window> windows = new HashMap<>();
    private static long nextWindowHandle = 0x5D10_0001L;
    static long mainWindow;
    static long glContext;
    private static boolean initialized;
    private static final long startTime = System.nanoTime();

    private static final ArrayDeque<Event> queue = new ArrayDeque<>();
    private static final List<Long> pendingStrings = new ArrayList<>();
    static String lastError = "";

    static final ByteBuffer keyboardState = memCalloc(NUM_SCANCODES);
    private static short modState;
    static float mouseX, mouseY;
    private static boolean haveMousePos;

    static final int[] gl = new int[64];

    // ---- init / shutdown -------------------------------------------------

    static synchronized boolean init() {
        if (initialized) return true;
        if (!GLFW.glfwInit()) {
            lastError = "pojavInit failed";
            return false;
        }
        GLFW.glfwSetKeyCallback(0, (w, key, scancode, action, mods) -> onKey(key, action));
        GLFW.glfwSetCharCallback(0, (w, codepoint) -> onChar(codepoint));
        GLFW.glfwSetCursorPosCallback(0, (w, x, y) -> onCursorPos((float) x, (float) y));
        GLFW.glfwSetMouseButtonCallback(0, (w, button, action, mods) -> onMouseButton(button, action));
        GLFW.glfwSetScrollCallback(0, (w, x, y) -> onScroll((float) x, (float) y));
        GLFW.glfwSetFramebufferSizeCallback(0, (w, width, height) -> onResize(width, height));
        initialized = true;
        return true;
    }

    static synchronized void quit() {
        if (!initialized) return;
        initialized = false;
        GLFW.glfwTerminate();
    }

    static long ticksNS() {
        return System.nanoTime() - startTime;
    }

    // ---- windows / GL context -------------------------------------------

    static int width() {
        return Math.max(1, GLFW.mGLFWWindowWidth);
    }

    static int height() {
        return Math.max(1, GLFW.mGLFWWindowHeight);
    }

    static synchronized long createWindow(long flags) {
        long handle = nextWindowHandle++;
        windows.put(handle, new Window(handle, flags | WINDOW_INPUT_FOCUS | WINDOW_MOUSE_FOCUS));
        if ((flags & WINDOW_HIDDEN) == 0) {
            mainWindow = handle;
            // Real SDL on a phone opens the window at screen size and reports it,
            // which is how MC learns its real size and focus state.
            push(simple(EVENT_WINDOW_SHOWN));
            pushResize(width(), height());
            push(simple(EVENT_WINDOW_MOUSE_ENTER));
            push(simple(EVENT_WINDOW_FOCUS_GAINED));
            if (glContext != 0) GLFW.glfwShowWindow(glContext);
        }
        return handle;
    }

    static synchronized void destroyWindow(long handle) {
        windows.remove(handle);
        if (handle == mainWindow) mainWindow = 0;
    }

    static synchronized Window window(long handle) {
        return windows.get(handle);
    }

    static synchronized long createGLContext(long window) {
        if (glContext == 0) {
            int w = width(), h = height();
            glContext = GLFW.glfwCreateWindow(w, h, "Minecraft", 0, 0);
            if (glContext == 0) {
                lastError = "pojavCreateContext failed";
                return 0;
            }
            GLFW.glfwShowWindow(glContext);
        }
        return glContext;
    }

    static boolean makeCurrent(long window, long context) {
        if (context == 0) return true;
        GLFW.glfwMakeContextCurrent(context);
        return true;
    }

    static void setGLAttribute(int attr, int value) {
        if (attr >= 0 && attr < gl.length) gl[attr] = value;
        switch (attr) {
            case 17: // SDL_GL_CONTEXT_MAJOR_VERSION
                GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, value);
                break;
            case 18: // SDL_GL_CONTEXT_MINOR_VERSION
                GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, value);
                break;
        }
    }

    static int getGLAttribute(int attr) {
        if (attr == 17 && gl[17] == 0) return 3;
        if (attr == 18 && gl[18] == 0 && gl[17] == 0) return 3;
        return attr >= 0 && attr < gl.length ? gl[attr] : 0;
    }

    static synchronized void setFullscreen(long handle, boolean fullscreen) {
        Window w = windows.get(handle);
        if (w == null) return;
        boolean was = (w.flags & WINDOW_FULLSCREEN) != 0;
        if (fullscreen) w.flags |= WINDOW_FULLSCREEN; else w.flags &= ~WINDOW_FULLSCREEN;
        if (was != fullscreen) push(simple(fullscreen ? EVENT_WINDOW_ENTER_FULLSCREEN : EVENT_WINDOW_LEAVE_FULLSCREEN));
        pushResize(width(), height());
    }

    // ---- events ------------------------------------------------------------

    private static Event simple(int type) {
        Event e = new Event();
        e.type = type;
        return e;
    }

    private static synchronized void push(Event e) {
        if (queue.size() < 8192) queue.add(e);
    }

    static synchronized void pushResize(int w, int h) {
        Event e = simple(EVENT_WINDOW_RESIZED);
        e.i1 = w;
        e.i2 = h;
        push(e);
        e = simple(EVENT_WINDOW_PIXEL_SIZE_CHANGED);
        e.i1 = w;
        e.i2 = h;
        push(e);
    }

    static void pump() {
        synchronized (PojavSDL.class) {
            for (long p : pendingStrings) nmemFree(p);
            pendingStrings.clear();
        }
        if (initialized) GLFW.glfwPollEvents();
    }

    static synchronized void flush(int minType, int maxType) {
        Iterator<Event> it = queue.iterator();
        while (it.hasNext()) {
            int t = it.next().type;
            if (t >= minType && t <= maxType) it.remove();
        }
    }

    static boolean poll(SDL_Event out) {
        Event e;
        synchronized (PojavSDL.class) {
            e = queue.poll();
        }
        if (e == null) {
            pump();
            synchronized (PojavSDL.class) {
                e = queue.poll();
            }
        }
        if (e == null) return false;
        if (out != null) write(e, out.address());
        return true;
    }

    private static void write(Event e, long a) {
        memSet(a, 0, SDL_Event.SIZEOF);
        long ts = ticksNS();
        int windowID = (int) (mainWindow & 0xFFFFFFFFL);
        switch (e.type) {
            case EVENT_KEY_DOWN:
            case EVENT_KEY_UP: {
                long k = a + SDL_Event.KEY;
                memPutInt(k + SDL_KeyboardEvent.TYPE, e.type);
                memPutLong(k + SDL_KeyboardEvent.TIMESTAMP, ts);
                memPutInt(k + SDL_KeyboardEvent.WINDOWID, windowID);
                memPutInt(k + SDL_KeyboardEvent.SCANCODE, e.i1);
                memPutInt(k + SDL_KeyboardEvent.KEY, e.i2);
                memPutShort(k + SDL_KeyboardEvent.MOD, (short) e.i3);
                memPutShort(k + SDL_KeyboardEvent.RAW, (short) e.i1);
                memPutByte(k + SDL_KeyboardEvent.DOWN, (byte) (e.type == EVENT_KEY_DOWN ? 1 : 0));
                memPutByte(k + SDL_KeyboardEvent.REPEAT, (byte) (e.b1 ? 1 : 0));
                break;
            }
            case EVENT_TEXT_INPUT: {
                long t = a + SDL_Event.TEXT;
                long str = nmemAllocChecked(16);
                ByteBuffer buf = memByteBuffer(str, 16);
                int len = memUTF8(e.text, true, buf);
                synchronized (PojavSDL.class) {
                    pendingStrings.add(str);
                }
                memPutInt(t + SDL_TextInputEvent.TYPE, e.type);
                memPutLong(t + SDL_TextInputEvent.TIMESTAMP, ts);
                memPutInt(t + SDL_TextInputEvent.WINDOWID, windowID);
                memPutAddress(t + SDL_TextInputEvent.TEXT, str);
                break;
            }
            case EVENT_MOUSE_MOTION: {
                long m = a + SDL_Event.MOTION;
                memPutInt(m + SDL_MouseMotionEvent.TYPE, e.type);
                memPutLong(m + SDL_MouseMotionEvent.TIMESTAMP, ts);
                memPutInt(m + SDL_MouseMotionEvent.WINDOWID, windowID);
                memPutFloat(m + SDL_MouseMotionEvent.X, e.f1);
                memPutFloat(m + SDL_MouseMotionEvent.Y, e.f2);
                memPutFloat(m + SDL_MouseMotionEvent.XREL, e.f3);
                memPutFloat(m + SDL_MouseMotionEvent.YREL, e.f4);
                break;
            }
            case EVENT_MOUSE_BUTTON_DOWN:
            case EVENT_MOUSE_BUTTON_UP: {
                long b = a + SDL_Event.BUTTON;
                memPutInt(b + SDL_MouseButtonEvent.TYPE, e.type);
                memPutLong(b + SDL_MouseButtonEvent.TIMESTAMP, ts);
                memPutInt(b + SDL_MouseButtonEvent.WINDOWID, windowID);
                memPutByte(b + SDL_MouseButtonEvent.BUTTON, (byte) e.i1);
                memPutByte(b + SDL_MouseButtonEvent.DOWN, (byte) (e.type == EVENT_MOUSE_BUTTON_DOWN ? 1 : 0));
                memPutByte(b + SDL_MouseButtonEvent.CLICKS, (byte) 1);
                memPutFloat(b + SDL_MouseButtonEvent.X, e.f1);
                memPutFloat(b + SDL_MouseButtonEvent.Y, e.f2);
                break;
            }
            case EVENT_MOUSE_WHEEL: {
                long w = a + SDL_Event.WHEEL;
                memPutInt(w + SDL_MouseWheelEvent.TYPE, e.type);
                memPutLong(w + SDL_MouseWheelEvent.TIMESTAMP, ts);
                memPutInt(w + SDL_MouseWheelEvent.WINDOWID, windowID);
                memPutFloat(w + SDL_MouseWheelEvent.X, e.f1);
                memPutFloat(w + SDL_MouseWheelEvent.Y, e.f2);
                memPutFloat(w + SDL_MouseWheelEvent.MOUSE_X, mouseX);
                memPutFloat(w + SDL_MouseWheelEvent.MOUSE_Y, mouseY);
                memPutInt(w + SDL_MouseWheelEvent.INTEGER_X, (int) e.f1);
                memPutInt(w + SDL_MouseWheelEvent.INTEGER_Y, (int) e.f2);
                break;
            }
            default: {
                long w = a + SDL_Event.WINDOW;
                memPutInt(w + SDL_WindowEvent.TYPE, e.type);
                memPutLong(w + SDL_WindowEvent.TIMESTAMP, ts);
                memPutInt(w + SDL_WindowEvent.WINDOWID, windowID);
                memPutInt(w + SDL_WindowEvent.DATA1, e.i1);
                memPutInt(w + SDL_WindowEvent.DATA2, e.i2);
                break;
            }
        }
    }

    // ---- GLFW callback -> SDL event ----------------------------------------

    private static void onKey(int glfwKey, int action) {
        int scancode = glfwKeyToScancode(glfwKey);
        if (scancode == 0) return;
        boolean down = action != 0;
        keyboardState.put(scancode, (byte) (down ? 1 : 0));
        updateModState(scancode, down);
        Event e = simple(down ? EVENT_KEY_DOWN : EVENT_KEY_UP);
        e.i1 = scancode;
        e.i2 = scancodeToKeycode(scancode);
        e.i3 = modState & 0xFFFF;
        e.b1 = action == 2;
        push(e);
    }

    private static void onChar(int codepoint) {
        if (codepoint <= 0 || Character.isISOControl(codepoint)) return;
        Event e = simple(EVENT_TEXT_INPUT);
        e.text = new String(Character.toChars(codepoint));
        push(e);
    }

    private static void onCursorPos(float x, float y) {
        float dx = haveMousePos ? x - mouseX : 0f;
        float dy = haveMousePos ? y - mouseY : 0f;
        mouseX = x;
        mouseY = y;
        haveMousePos = true;
        Event e = simple(EVENT_MOUSE_MOTION);
        e.f1 = x;
        e.f2 = y;
        e.f3 = dx;
        e.f4 = dy;
        push(e);
    }

    private static void onMouseButton(int glfwButton, int action) {
        int button;
        switch (glfwButton) {
            case 0: button = 1; break; // left
            case 1: button = 3; break; // right
            case 2: button = 2; break; // middle
            default: button = glfwButton + 1; break;
        }
        Event e = simple(action != 0 ? EVENT_MOUSE_BUTTON_DOWN : EVENT_MOUSE_BUTTON_UP);
        e.i1 = button;
        e.f1 = mouseX;
        e.f2 = mouseY;
        push(e);
    }

    private static void onScroll(float x, float y) {
        Event e = simple(EVENT_MOUSE_WHEEL);
        e.f1 = x;
        e.f2 = y;
        push(e);
    }

    private static void onResize(int w, int h) {
        if (w > 0 && h > 0) pushResize(w, h);
    }

    static void warpMouse(float x, float y) {
        mouseX = x;
        mouseY = y;
        haveMousePos = true;
        if (glContext != 0) GLFW.glfwSetCursorPos(glContext, x, y);
    }

    static void setRelativeMouse(boolean enabled) {
        CallbackBridge.nativeSetGrabbing(enabled);
    }

    // ---- keyboard ------------------------------------------------------------

    static short modState() {
        return modState;
    }

    private static void updateModState(int scancode, boolean down) {
        int bit;
        switch (scancode) {
            case 225: bit = 0x0001; break; // LSHIFT
            case 229: bit = 0x0002; break; // RSHIFT
            case 224: bit = 0x0040; break; // LCTRL
            case 228: bit = 0x0080; break; // RCTRL
            case 226: bit = 0x0100; break; // LALT
            case 230: bit = 0x0200; break; // RALT
            case 227: bit = 0x0400; break; // LGUI
            case 231: bit = 0x0800; break; // RGUI
            case 83:  if (down) modState ^= 0x1000; return; // NUMLOCK toggles
            case 57:  if (down) modState ^= 0x2000; return; // CAPSLOCK toggles
            default: return;
        }
        if (down) modState |= bit; else modState &= ~bit;
    }

    private static final int[] GLFW_TO_SCANCODE = buildKeyTable();

    static int glfwKeyToScancode(int key) {
        return key >= 0 && key < GLFW_TO_SCANCODE.length ? GLFW_TO_SCANCODE[key] : 0;
    }

    // Default US layout, matching SDL_GetKeyFromScancode with no remapping.
    static int scancodeToKeycode(int sc) {
        if (sc >= 4 && sc <= 29) return 'a' + (sc - 4);
        if (sc >= 30 && sc <= 38) return '1' + (sc - 30);
        switch (sc) {
            case 39: return '0';
            case 40: return '\r';
            case 41: return 0x1B;
            case 42: return '\b';
            case 43: return '\t';
            case 44: return ' ';
            case 45: return '-';
            case 46: return '=';
            case 47: return '[';
            case 48: return ']';
            case 49: return '\\';
            case 51: return ';';
            case 52: return '\'';
            case 53: return '`';
            case 54: return ',';
            case 55: return '.';
            case 56: return '/';
            case 76: return 0x7F;
            case 0: return 0;
            default: return sc | 0x40000000;
        }
    }

    static String keyName(int keycode) {
        if (keycode > 0x20 && keycode < 0x7F) {
            return String.valueOf((char) Character.toUpperCase(keycode));
        }
        return "";
    }

    private static int[] buildKeyTable() {
        int[] m = new int[349];
        m[32] = 44; // SPACE
        m[39] = 52; // APOSTROPHE
        m[44] = 54; // COMMA
        m[45] = 45; // MINUS
        m[46] = 55; // PERIOD
        m[47] = 56; // SLASH
        m[48] = 39; // 0
        m[49] = 30; // 1
        m[50] = 31; // 2
        m[51] = 32; // 3
        m[52] = 33; // 4
        m[53] = 34; // 5
        m[54] = 35; // 6
        m[55] = 36; // 7
        m[56] = 37; // 8
        m[57] = 38; // 9
        m[59] = 51; // SEMICOLON
        m[61] = 46; // EQUAL
        m[65] = 4; // A
        m[66] = 5; // B
        m[67] = 6; // C
        m[68] = 7; // D
        m[69] = 8; // E
        m[70] = 9; // F
        m[71] = 10; // G
        m[72] = 11; // H
        m[73] = 12; // I
        m[74] = 13; // J
        m[75] = 14; // K
        m[76] = 15; // L
        m[77] = 16; // M
        m[78] = 17; // N
        m[79] = 18; // O
        m[80] = 19; // P
        m[81] = 20; // Q
        m[82] = 21; // R
        m[83] = 22; // S
        m[84] = 23; // T
        m[85] = 24; // U
        m[86] = 25; // V
        m[87] = 26; // W
        m[88] = 27; // X
        m[89] = 28; // Y
        m[90] = 29; // Z
        m[91] = 47; // LEFT_BRACKET
        m[92] = 49; // BACKSLASH
        m[93] = 48; // RIGHT_BRACKET
        m[96] = 53; // GRAVE_ACCENT
        m[161] = 100; // WORLD_1
        m[162] = 135; // WORLD_2
        m[256] = 41; // ESCAPE
        m[257] = 40; // ENTER
        m[258] = 43; // TAB
        m[259] = 42; // BACKSPACE
        m[260] = 73; // INSERT
        m[261] = 76; // DELETE
        m[262] = 79; // RIGHT
        m[263] = 80; // LEFT
        m[264] = 81; // DOWN
        m[265] = 82; // UP
        m[266] = 75; // PAGE_UP
        m[267] = 78; // PAGE_DOWN
        m[268] = 74; // HOME
        m[269] = 77; // END
        m[280] = 57; // CAPS_LOCK
        m[281] = 71; // SCROLL_LOCK
        m[282] = 83; // NUM_LOCK
        m[283] = 70; // PRINT_SCREEN
        m[284] = 72; // PAUSE
        m[290] = 58; // F1
        m[291] = 59; // F2
        m[292] = 60; // F3
        m[293] = 61; // F4
        m[294] = 62; // F5
        m[295] = 63; // F6
        m[296] = 64; // F7
        m[297] = 65; // F8
        m[298] = 66; // F9
        m[299] = 67; // F10
        m[300] = 68; // F11
        m[301] = 69; // F12
        m[302] = 104; // F13
        m[303] = 105; // F14
        m[304] = 106; // F15
        m[305] = 107; // F16
        m[306] = 108; // F17
        m[307] = 109; // F18
        m[308] = 110; // F19
        m[309] = 111; // F20
        m[310] = 112; // F21
        m[311] = 113; // F22
        m[312] = 114; // F23
        m[313] = 115; // F24
        m[320] = 98; // KP_0
        m[321] = 89; // KP_1
        m[322] = 90; // KP_2
        m[323] = 91; // KP_3
        m[324] = 92; // KP_4
        m[325] = 93; // KP_5
        m[326] = 94; // KP_6
        m[327] = 95; // KP_7
        m[328] = 96; // KP_8
        m[329] = 97; // KP_9
        m[330] = 99; // KP_DECIMAL
        m[331] = 84; // KP_DIVIDE
        m[332] = 85; // KP_MULTIPLY
        m[333] = 86; // KP_SUBTRACT
        m[334] = 87; // KP_ADD
        m[335] = 88; // KP_ENTER
        m[336] = 103; // KP_EQUAL
        m[340] = 225; // LEFT_SHIFT
        m[341] = 224; // LEFT_CONTROL
        m[342] = 226; // LEFT_ALT
        m[343] = 227; // LEFT_SUPER
        m[344] = 229; // RIGHT_SHIFT
        m[345] = 228; // RIGHT_CONTROL
        m[346] = 230; // RIGHT_ALT
        m[347] = 231; // RIGHT_SUPER
        m[348] = 118; // MENU
        return m;
    }

    // ---- display -------------------------------------------------------------

    private static SDL_DisplayMode displayMode;
    private static SDL_PixelFormatDetails formatDetails;

    static synchronized SDL_DisplayMode displayMode() {
        if (displayMode == null) displayMode = SDL_DisplayMode.calloc();
        long a = displayMode.address();
        float refresh = 60f;
        try {
            refresh = Float.parseFloat(System.getProperty("UIScreen.maximumFramesPerSecond", "60"));
        } catch (NumberFormatException ignored) {
        }
        memPutInt(a + SDL_DisplayMode.DISPLAYID, DISPLAY_ID);
        memPutInt(a + SDL_DisplayMode.FORMAT, PIXELFORMAT_XRGB8888);
        memPutInt(a + SDL_DisplayMode.W, width());
        memPutInt(a + SDL_DisplayMode.H, height());
        memPutFloat(a + SDL_DisplayMode.PIXEL_DENSITY, 1f);
        memPutFloat(a + SDL_DisplayMode.REFRESH_RATE, refresh);
        memPutInt(a + SDL_DisplayMode.REFRESH_RATE_NUMERATOR, (int) refresh);
        memPutInt(a + SDL_DisplayMode.REFRESH_RATE_DENOMINATOR, 1);
        return displayMode;
    }

    static synchronized SDL_PixelFormatDetails formatDetails() {
        if (formatDetails == null) {
            formatDetails = SDL_PixelFormatDetails.create(nmemCallocChecked(1, SDL_PixelFormatDetails.SIZEOF));
            long a = formatDetails.address();
            memPutInt(a + SDL_PixelFormatDetails.FORMAT, PIXELFORMAT_XRGB8888);
            memPutByte(a + SDL_PixelFormatDetails.BITS_PER_PIXEL, (byte) 24);
            memPutByte(a + SDL_PixelFormatDetails.BYTES_PER_PIXEL, (byte) 4);
            memPutInt(a + SDL_PixelFormatDetails.RMASK, 0x00FF0000);
            memPutInt(a + SDL_PixelFormatDetails.GMASK, 0x0000FF00);
            memPutInt(a + SDL_PixelFormatDetails.BMASK, 0x000000FF);
            memPutByte(a + SDL_PixelFormatDetails.RBITS, (byte) 8);
            memPutByte(a + SDL_PixelFormatDetails.GBITS, (byte) 8);
            memPutByte(a + SDL_PixelFormatDetails.BBITS, (byte) 8);
            memPutByte(a + SDL_PixelFormatDetails.RSHIFT, (byte) 16);
            memPutByte(a + SDL_PixelFormatDetails.GSHIFT, (byte) 8);
        }
        return formatDetails;
    }
}
