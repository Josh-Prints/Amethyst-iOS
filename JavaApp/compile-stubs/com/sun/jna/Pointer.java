package com.sun.jna;

// Compile-time only: lets the ca.weblite.objc stubs declare the real
// NSObject(Pointer) constructor. Never packaged; JNA itself comes from the
// Minecraft libraries at runtime.
public class Pointer {
    public Pointer(long peer) {}
}
