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

import javax.net.ssl.SSLSocketFactory;

public abstract class ManagedSSLSocketFactory extends SSLSocketFactory {

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

}
