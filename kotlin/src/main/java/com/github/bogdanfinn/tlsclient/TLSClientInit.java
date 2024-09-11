package com.github.bogdanfinn.tlsclient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class TLSClientInit {
    public static void init() throws IOException {
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");

        String libName = null;
        if (osName.contains("Windows")) {
            libName = "tls-client-windows-" + osArch + ".dll";
        } else if (osName.contains("Linux")) {
            libName = "tls-client-linux-" + osArch + ".so";
        } else if (osName.contains("Mac")) {
            libName = "tls-client-darwin-" + osArch + ".dylib";
        }

        if (libName == null) throw new IOException("Unsupported platform.");

        String tlsClientDev = System.getProperty("com.github.bogdanfinn.tlsclient.dev");
        if (tlsClientDev != null && tlsClientDev.equals("1")) {
            System.load(new File("../go/dist/" + libName).getAbsolutePath());
            return;
        }

        try (InputStream stream = TLSClientInit.class.getResourceAsStream("/com/github/bogdanfinn/tlsclient/resources/" + libName)) {
            if (stream == null) throw new IOException("Library " + libName + " was not found in resources.");

            File tempFile = Files.createTempFile("temp_", libName).toFile();
            tempFile.deleteOnExit();

            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                stream.transferTo(out);
            }

            System.load(tempFile.getAbsolutePath());
        }
    }
}
