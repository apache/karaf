/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.services.web.internal;

import java.io.File;
import java.util.Arrays;
import java.util.Dictionary;

/**
 * The configuration of the web container, from the {@code org.apache.karaf.services.web} PID.
 */
public class WebConfig {

    public static final String PID = "org.apache.karaf.services.web";

    String host = "0.0.0.0";
    boolean httpEnabled = true;
    int httpPort = 8181;
    boolean httpsEnabled = false;
    int httpsPort = 8443;
    String keystore = "${karaf.etc}/keystore";
    String keystorePassword;
    String keystoreType = "PKCS12";
    String keyAlias;
    String keyPassword;
    int maxThreads = 200;
    int sessionTimeout = 30;
    String realm = "karaf";
    long undeployTimeout = 30000;

    public static WebConfig from(Dictionary<String, ?> properties) {
        WebConfig config = new WebConfig();
        if (properties == null) {
            return config;
        }
        config.host = getString(properties, "host", config.host);
        config.httpEnabled = getBoolean(properties, "http.enabled", config.httpEnabled);
        config.httpPort = getInt(properties, "http.port", config.httpPort);
        config.httpsEnabled = getBoolean(properties, "https.enabled", config.httpsEnabled);
        config.httpsPort = getInt(properties, "https.port", config.httpsPort);
        config.keystore = getString(properties, "https.keystore", config.keystore);
        config.keystorePassword = getString(properties, "https.keystore.password", config.keystorePassword);
        config.keystoreType = getString(properties, "https.keystore.type", config.keystoreType);
        config.keyAlias = getString(properties, "https.key.alias", config.keyAlias);
        config.keyPassword = getString(properties, "https.key.password", config.keyPassword);
        config.maxThreads = getInt(properties, "max.threads", config.maxThreads);
        config.sessionTimeout = getInt(properties, "session.timeout", config.sessionTimeout);
        config.realm = getString(properties, "realm", config.realm);
        config.undeployTimeout = getInt(properties, "undeploy.timeout", (int) config.undeployTimeout);
        return config;
    }

    /**
     * @return the name of the JAAS realm authenticating the users of the web application bundles.
     */
    public String getRealm() {
        return realm;
    }

    /**
     * @return the absolute path of the keystore, a relative path being resolved from the Karaf base directory.
     */
    String getKeystorePath() {
        String path = keystore.replace("${karaf.etc}", System.getProperty("karaf.etc", "etc"));
        File file = new File(path);
        if (!file.isAbsolute()) {
            file = new File(System.getProperty("karaf.base", "."), path);
        }
        return file.getAbsolutePath();
    }

    private Object[] values() {
        return new Object[]{host, httpEnabled, httpPort, httpsEnabled, httpsPort, keystore, keystorePassword,
                keystoreType, keyAlias, keyPassword, maxThreads, sessionTimeout, realm, undeployTimeout};
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof WebConfig && Arrays.equals(values(), ((WebConfig) other).values());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values());
    }

    private static String getString(Dictionary<String, ?> properties, String key, String def) {
        Object value = properties.get(key);
        if (value == null || value.toString().trim().isEmpty()) {
            return def;
        }
        return value.toString().trim();
    }

    private static int getInt(Dictionary<String, ?> properties, String key, int def) {
        String value = getString(properties, key, null);
        if (value == null) {
            return def;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid value for " + key + ": " + value, e);
        }
    }

    private static boolean getBoolean(Dictionary<String, ?> properties, String key, boolean def) {
        String value = getString(properties, key, null);
        return value == null ? def : Boolean.parseBoolean(value);
    }

}
