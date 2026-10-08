package ca.weblite.objc;

import com.sun.jna.Pointer;

/** Inert stand-in for java-objc-bridge's NSObject; see {@link Client}. */
public class NSObject extends Proxy {
    public NSObject() {}

    public NSObject(Pointer peer) {}

    public NSObject(String className) {}

    public Client getClient() {
        return Client.getInstance();
    }
}
