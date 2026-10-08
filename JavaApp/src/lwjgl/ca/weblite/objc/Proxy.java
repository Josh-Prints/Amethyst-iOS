package ca.weblite.objc;

/** Inert stand-in for java-objc-bridge's Proxy; see {@link Client}. */
public class Proxy {
    static final Proxy NULL = new Proxy();

    public Proxy sendProxy(String selector, Object... args) {
        return NULL;
    }

    public Object send(String selector, Object... args) {
        return null;
    }

    public int sendInt(String selector, Object... args) {
        return 0;
    }

    public boolean sendBoolean(String selector, Object... args) {
        return false;
    }

    public String sendString(String selector, Object... args) {
        return null;
    }

    public Object sendRaw(String selector, Object... args) {
        return 0L;
    }

    public Object get(String key) {
        return null;
    }

    public void set(String key, Object value) {}

    public long getPeer() {
        return 0L;
    }
}
