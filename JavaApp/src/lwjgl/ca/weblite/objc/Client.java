package ca.weblite.objc;

/**
 * Inert stand-in for java-objc-bridge. Minecraft only uses the bridge for macOS
 * AppKit menu tweaks (MacosUtil), which don't exist on iOS, and the real bridge
 * needs JNA's native library, which can't load here (unsigned temp extraction).
 * The launcher drops the real library from the classpath; this answers every
 * message with nothing.
 */
public class Client {
    private static final Client INSTANCE = new Client();

    public static Client getInstance() {
        return INSTANCE;
    }

    public static Client getRawClient() {
        return INSTANCE;
    }

    public Proxy sendProxy(String receiver, String selector, Object... args) {
        return Proxy.NULL;
    }

    public Proxy sendProxy(Object receiver, String selector, Object... args) {
        return Proxy.NULL;
    }

    public Object send(String receiver, String selector, Object... args) {
        return null;
    }

    public Object send(Object receiver, String selector, Object... args) {
        return null;
    }

    public int sendInt(String receiver, String selector, Object... args) {
        return 0;
    }

    public int sendInt(Object receiver, String selector, Object... args) {
        return 0;
    }

    public boolean sendBoolean(String receiver, String selector, Object... args) {
        return false;
    }

    public boolean sendBoolean(Object receiver, String selector, Object... args) {
        return false;
    }

    public String sendString(String receiver, String selector, Object... args) {
        return null;
    }

    public String sendString(Object receiver, String selector, Object... args) {
        return null;
    }

    public Object sendRaw(String receiver, String selector, Object... args) {
        return 0L;
    }

    public Object sendRaw(Object receiver, String selector, Object... args) {
        return 0L;
    }
}
