package org.apache.karaf.tooling;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Provide the read version of Karaf by reading the processed resource file {@code versions.properties}.
 */
public class RealKarafVersion {

    public String get() {
        try (InputStream is = getClass().getResourceAsStream("versions.properties")) {
            Properties versions = new Properties();
            versions.load(is);
            return versions.getProperty("karaf-version");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
