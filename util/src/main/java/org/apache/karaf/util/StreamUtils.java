package org.apache.karaf.util;

import java.io.Closeable;
import java.io.IOException;

public class StreamUtils {
    
    private StreamUtils() {
    }
    

    public static void close(Closeable... closeables) {
        for (Closeable c : closeables) {
            try {
                c.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}
