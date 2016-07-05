/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.apache.karaf.jaas.modules.ldap;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Comparator;
import javax.net.ssl.SSLSocketFactory;

public class ManagedSSLSocketFactory extends SSLSocketFactory implements Comparator<Object> {

    private static final ThreadLocal<SSLSocketFactory> factories = new ThreadLocal<SSLSocketFactory>();

    public static void setSocketFactory(SSLSocketFactory factory) {
        factories.set(factory);
    }

    public static SSLSocketFactory getDefault() {
        SSLSocketFactory factory = factories.get();
        if (factory == null) {
            throw new IllegalStateException("No SSLSocketFactory parameters have been set!");
        }
        return factory;
    }

    private SSLSocketFactory delegate;

    // When <code>java.naming.ldap.factory.socket</code> property configures custom
    // {@link SSLSocketFactory}, LdapCtx invokes special compare method to enable pooling.

    public ManagedSSLSocketFactory(SSLSocketFactory delegate) {
        this.delegate = delegate;
    }

    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return delegate.createSocket(s, host, port, autoClose);
    }

    public Socket createSocket(String host, int port) throws IOException {
        return delegate.createSocket(host, port);
    }

    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return delegate.createSocket(host, port, localHost, localPort);
    }

    public Socket createSocket(InetAddress host, int port) throws IOException {
        return delegate.createSocket(host, port);
    }

    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return delegate.createSocket(address, port, localAddress, localPort);
    }

    /**
     * For com.sun.jndi.ldap.ClientId#invokeComparator(com.sun.jndi.ldap.ClientId, com.sun.jndi.ldap.ClientId)
     * @param f1
     * @param f2
     * @return
     */
    public int compare(Object f1, Object f2) {
        if (f1 == null && f2 == null) {
            return 0;
        }
        if (f1 == null) {
            return 1;
        }
        if (f2 == null) {
            return -1;
        }
        // com.sun.jndi.ldap.ClientId#invokeComparator() passes com.sun.jndi.ldap.ClientId.socketFactory as f1 and f2
        // these are String values
        if (f1 instanceof String && f2 instanceof String) {
            return ((String) f1).compareTo((String) f2);
        }
        // fallback to undefined behavior
        return f1.toString().compareTo(f2.toString());
    }

}
