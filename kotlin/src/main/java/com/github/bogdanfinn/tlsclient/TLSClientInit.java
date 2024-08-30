package com.github.bogdanfinn.tlsclient;

import java.io.File;
import java.io.IOException;

public class TLSClientInit {
    public static void init() throws IOException {
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");

        // TODO: macos lol

        if (osName.contains("Windows")) {
            System.load(new File("../go/dist/tls-client-windows-" + osArch + ".dll").getCanonicalPath());
        } else if (osName.contains("Linux")) {
            System.load(new File("../go/dist/tls-client-linux-" + osArch + ".so").getCanonicalPath());
        } else {
            System.err.println("Warning: could not determine platform! Failing to load TLS client library");
        }
    }
}
