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
package org.apache.karaf.audit.layout;

import org.apache.karaf.audit.Event;
import org.apache.karaf.audit.EventLayout;
import org.apache.karaf.audit.util.Buffer;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.CharBuffer;
import java.util.Enumeration;

public abstract class AbstractLayout implements EventLayout {

    protected final String hostName;
    protected final String appName;
    protected final String procId;
    
    protected final Buffer buffer;

    public AbstractLayout(Buffer buffer) {
        this.hostName = hostname();
        this.appName = System.getProperty("karaf.name", "-");
        this.procId = procId();
        this.buffer = buffer;
    }

    @Override
    public void format(Event event, Appendable to) throws IOException {
        doFormat(event);
        buffer.writeTo(to);
    }

    @Override
    public CharBuffer format(Event event) throws IOException {
        doFormat(event);
        return CharBuffer.wrap(buffer.buffer(), 0, buffer.position());
    }

    private void doFormat(Event event) throws IOException {
        buffer.clear();
        header(event);
        message(event);
        footer(event);
    }

    protected abstract void header(Event event) throws IOException;

    protected abstract void footer(Event event) throws IOException;

    protected void message(Event event) throws IOException {
        append("subject", event.subject());
        append("type", event.type());
        append("subtype", event.subtype());
        String message = null;
        switch (event.type()) {
            case Event.TYPE_SHELL: {
                append(event, "script");
                append(event, "command");
                append(event, "exception");
                break;
            }
            case Event.TYPE_LOGIN: {
                append(event, "username");
                break;
            }
            case Event.TYPE_JMX: {
                append(event, "method");
                append(event, "signature");
                append(event, "params");
                append(event, "result");
                append(event, "exception");
                break;
            }
            case Event.TYPE_LOG: {
                Bundle bundle = (Bundle) event.getProperty("bundle");
                if (bundle != null) {
                    append("bundle.id", bundle.getBundleId());
                    append("bundle.symbolicname", bundle.getSymbolicName());
                    append("bundle.version", bundle.getVersion());
                }
                append(event, "message");
                append(event, "exception");
                break;
            }
            case Event.TYPE_BUNDLE: {
                Bundle bundle = (Bundle) event.getProperty("bundle");
                append("bundle.id", bundle.getBundleId());
                append("bundle.symbolicname", bundle.getSymbolicName());
                append("bundle.version", bundle.getVersion());
                break;
            }
            case Event.TYPE_SERVICE: {
                ServiceEvent se = (ServiceEvent) event.getProperty("event");
                append("service.bundleid", se.getServiceReference().getProperty(Constants.SERVICE_BUNDLEID));
                append("service.id", se.getServiceReference().getProperty(Constants.SERVICE_ID));
                append("objectClass", se.getServiceReference().getProperty(Constants.OBJECTCLASS));
                break;
            }
            case Event.TYPE_WEB: {
                append(event, "servlet.servlet");
                append(event, "servlet.alias");
                break;
            }
            case Event.TYPE_REPOSITORIES: {
                append(event, "uri");
                break;
            }
            case Event.TYPE_FEATURES: {
                append(event, "name");
                append(event, "version");
                break;
            }
            case Event.TYPE_BLUEPRINT: {
                append(event, "bundle.id");
                append(event, "bundle.symbolicname");
                append(event, "bundle.version");
                break;
            }
            default: {
                for (String key : event.keys()) {
                    append(event, key);
                }
                break;
            }
        }
    }

    private void append(Event event, String key) throws IOException {
        append(key, event.getProperty(key));
    }

    protected abstract void append(String key, Object val) throws IOException;

    private static String hostname() {
        try {
            final InetAddress addr = InetAddress.getLocalHost();
            return addr.getHostName();
        } catch (final UnknownHostException uhe) {
            try {
                final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    final NetworkInterface nic = interfaces.nextElement();
                    final Enumeration<InetAddress> addresses = nic.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        final InetAddress address = addresses.nextElement();
                        if (!address.isLoopbackAddress()) {
                            final String hostname = address.getHostName();
                            if (hostname != null) {
                                return hostname;
                            }
                        }
                    }
                }
            } catch (final SocketException se) {
                // Ignore exception.
            }
            return "-";
        }
    }

    private static String procId() {
        try {
            return ManagementFactory.getRuntimeMXBean().getName().split("@")[0]; // likely works on most platforms
        } catch (final Exception ex) {
            try {
                return new File("/proc/self").getCanonicalFile().getName(); // try a Linux-specific way
            } catch (final IOException ignoredUseDefault) {
                // Ignore exception.
            }
        }
        return "-";
    }

}
