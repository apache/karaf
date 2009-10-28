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
package org.apache.felix.http.jetty.internal;

import org.osgi.framework.BundleContext;
import java.util.Dictionary;
import java.util.Properties;
import java.util.Hashtable;

public final class JettyConfig
{
    /** Standard OSGi port property for HTTP service */
    private static final String HTTP_PORT = "org.osgi.service.http.port";

    /** Standard OSGi port property for HTTPS service */
    private static final String HTTPS_PORT = "org.osgi.service.http.port.secure";

    /** Felix specific property to enable debug messages */
    private static final String FELIX_HTTP_DEBUG = "org.apache.felix.http.debug";
    private static final String HTTP_DEBUG = "org.apache.felix.http.jetty.debug";

    /** Felix specific property to override the keystore file location. */
    private static final String FELIX_KEYSTORE = "org.apache.felix.https.keystore";
    private static final String OSCAR_KEYSTORE = "org.ungoverned.osgi.bundle.https.keystore";

    /** Felix specific property to override the keystore password. */
    private static final String FELIX_KEYSTORE_PASSWORD = "org.apache.felix.https.keystore.password";
    private static final String OSCAR_KEYSTORE_PASSWORD = "org.ungoverned.osgi.bundle.https.password";

    /** Felix specific property to override the keystore key password. */
    private static final String FELIX_KEYSTORE_KEY_PASSWORD = "org.apache.felix.https.keystore.key.password";
    private static final String OSCAR_KEYSTORE_KEY_PASSWORD = "org.ungoverned.osgi.bundle.https.key.password";

    /** Felix specific property to control whether to enable HTTPS. */
    private static final String FELIX_HTTPS_ENABLE = "org.apache.felix.https.enable";
    private static final String OSCAR_HTTPS_ENABLE   = "org.ungoverned.osgi.bundle.https.enable";

    /** Felix specific property to control whether to enable HTTP. */
    private static final String FELIX_HTTP_ENABLE = "org.apache.felix.http.enable";

    /** Felix specific property to override the truststore file location. */
    private static final String FELIX_TRUSTSTORE = "org.apache.felix.https.truststore";

    /** Felix specific property to override the truststore password. */
    private static final String FELIX_TRUSTSTORE_PASSWORD = "org.apache.felix.https.truststore.password";

    /** Felix specific property to control whether to want or require HTTPS client certificates. Valid values are "none", "wants", "needs". Default is "none". */
    private static final String FELIX_HTTPS_CLIENT_CERT = "org.apache.felix.https.clientcertificate";
    
    private final BundleContext context;
    private boolean debug;
    private int httpPort;
    private int httpsPort;
    private String keystore;
    private String password;
    private String keyPassword;
    private boolean useHttps;
    private String truststore;
    private String trustPassword;
    private boolean useHttp;
    private String clientcert;

    public JettyConfig(BundleContext context)
    {
        this.context = context;
        reset();
    }

    public boolean isDebug()
    {
        return this.debug;
    }

    public boolean isUseHttp()
    {
        return this.useHttp;
    }

    public boolean isUseHttps()
    {
        return this.useHttps;
    }

    public int getHttpPort()
    {
        return this.httpPort;
    }

    public int getHttpsPort()
    {
        return this.httpsPort;
    }

    public String getKeystore()
    {
        return this.keystore;
    }

    public String getPassword()
    {
        return this.password;
    }

    public String getTruststore()
    {
        return this.truststore;
    }

    public String getTrustPassword()
    {
        return this.trustPassword;
    }

    public String getKeyPassword()
    {
        return this.keyPassword;
    }

    public String getClientcert()
    {
        return this.clientcert;
    }

    public void reset()
    {
        update(null);
    }

    public void update(Dictionary props)
    {
        if (props == null) {
            props = new Properties();
        }

        this.debug = getBooleanProperty(props, FELIX_HTTP_DEBUG, getBooleanProperty(props, HTTP_DEBUG, false));
        this.httpPort = getIntProperty(props, HTTP_PORT, 8080);
        this.httpsPort = getIntProperty(props, HTTPS_PORT, 433);
        this.keystore = getProperty(props, FELIX_KEYSTORE, this.context.getProperty(OSCAR_KEYSTORE));
        this.password = getProperty(props, FELIX_KEYSTORE_PASSWORD, this.context.getProperty(OSCAR_KEYSTORE_PASSWORD));
        this.keyPassword = getProperty(props, FELIX_KEYSTORE_KEY_PASSWORD, this.context.getProperty(OSCAR_KEYSTORE_KEY_PASSWORD));
        this.useHttps = getBooleanProperty(props, FELIX_HTTPS_ENABLE, getBooleanProperty(props, OSCAR_HTTPS_ENABLE, false));
        this.useHttp = getBooleanProperty(props, FELIX_HTTP_ENABLE, true);
        this.truststore = getProperty(props, FELIX_TRUSTSTORE, null);
        this.trustPassword = getProperty(props, FELIX_TRUSTSTORE_PASSWORD, null);
        this.clientcert = getProperty(props, FELIX_HTTPS_CLIENT_CERT, "none");
    }

    private String getProperty(Dictionary props, String name, String defValue)
    {
        String value = (String)props.get(name);
        if (value == null) {
            value = this.context.getProperty(name);
        }

        return value != null ? value : defValue;
    }

    private boolean getBooleanProperty(Dictionary props, String name, boolean defValue)
    {
        String value = getProperty(props, name, null);
        if (value != null) {
            return (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
        }

        return defValue;
    }

    private int getIntProperty(Dictionary props, String name, int defValue)
    {
        try {
            return Integer.parseInt(getProperty(props, name, null));
        } catch (Exception e) {
            return defValue;
        }
    }

    public void setServiceProperties(Hashtable<String, Object> props)
    {
        props.put(HTTP_PORT, String.valueOf(this.httpPort));
        props.put(HTTPS_PORT, String.valueOf(this.httpsPort));
        props.put(FELIX_HTTP_ENABLE, String.valueOf(this.useHttp));
        props.put(FELIX_HTTPS_ENABLE, String.valueOf(this.useHttps));
    }
}
