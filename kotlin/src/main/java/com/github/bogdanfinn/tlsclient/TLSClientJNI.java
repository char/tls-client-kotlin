package com.github.bogdanfinn.tlsclient;

public class TLSClientJNI {
    public static native void freeMemory(String responseId);
    public static native String destroyAll();
    public static native void destroySession(String sessionId);
    public static native String getCookiesFromSession(String paramsJson);
    public static native String addCookiesToSession(String paramsJson);
    public static native String request(byte[] paramsJson);
}
