package com.github.bogdanfinn.tlsclient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

public class TLSClientInit {
    private static boolean initialized;

    private static String getLibName() {
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");

        if (osName.contains("Windows")) {
            return "tls-client-windows-" + osArch + ".dll";
        } else if (osName.contains("Linux")) {
            return "tls-client-linux-" + osArch + ".so";
        } else if (osName.contains("Mac")) {
            return "tls-client-darwin-" + osArch + ".dylib";
        }

        throw new UnsupportedOperationException("Loading tls-client is not supported on this platform.");
    }

    public static void initFromPath(File base) {
        if (initialized) return;
        System.load(new File(base, getLibName()).getAbsolutePath());
        initialized = true;
    }

    public static void initFromResource(InputStream stream) throws IOException {
        if (initialized) return;

        File tempFile = Files.createTempFile("temp_", getLibName()).toFile();
        tempFile.deleteOnExit();

        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            stream.transferTo(out);
        }

        System.load(tempFile.getAbsolutePath());

        initialized = true;
    }

    public static void init() throws IOException {
        if (initialized) return;

        String tlsClientDev = System.getProperty("com.github.bogdanfinn.tlsclient.dev");
        if (tlsClientDev != null && tlsClientDev.equals("1")) {
            initFromPath(new File("../go/dist/"));
            return;
        }

        try (InputStream stream = TLSClientInit.class.getResourceAsStream("/com/github/bogdanfinn/tlsclient/resources/" + getLibName())) {
            if (stream != null) {
                initFromResource(stream);
                return;
            }
        }

        String httpFallbackBase = "https://cdn.kekr.io/tls/binary/"; // provided by kekr
        HttpURLConnection connection = (HttpURLConnection) new URL(httpFallbackBase + getLibName()).openConnection();
        connection.setRequestProperty("User-Agent", "Wget/1.13.4 (linux-gnu)"); // i was told to give this to the cdn
        initFromResource(connection.getInputStream());
    }
}
