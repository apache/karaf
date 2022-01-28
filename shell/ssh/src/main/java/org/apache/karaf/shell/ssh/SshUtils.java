/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.ssh;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.sshd.common.kex.KeyExchangeFactory;
import org.apache.sshd.server.ServerBuilder;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.cipher.Cipher;
import org.apache.sshd.common.mac.Mac;
import org.apache.sshd.common.signature.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SshUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshUtils.class);

    public static <S> List<NamedFactory<S>> filter(Class<S> type,
            Collection<NamedFactory<S>> factories, String[] names) {
        List<NamedFactory<S>> list = new ArrayList<>();
        for (String name : names) {
            name = name.trim();
            boolean found = false;
            for (NamedFactory<S> factory : factories) {
                if (factory.getName().equals(name)) {
                    list.add(factory);
                    found = true;
                    break;
                }
            }
            if (!found) {
                LOGGER.warn("Configured " + type.getSimpleName().toLowerCase()
                        + " '" + name + "' not available");
            }
        }
        return list;
    }

    public static List<KeyExchangeFactory> filter(List<KeyExchangeFactory> factories, String[] names) {
        List<KeyExchangeFactory> list = new ArrayList<>();
        for (String name : names) {
            name = name.trim();
            boolean found = false;
            for (KeyExchangeFactory factory : factories) {
                if (factory.getName().equals(name)) {
                    list.add(factory);
                    found = true;
                    break;
                }
            }
            if (!found) {
                LOGGER.warn("Configured KeyExchangeFactory '" + name + "' not available");
            }
        }
        return list;
    }

    public static List<NamedFactory<Mac>> buildMacs(String[] names) {
        return filter(Mac.class, new ServerConfig().getMacFactories(), names);
    }

    public static List<NamedFactory<Cipher>> buildCiphers(String[] names) {
        ServerConfig defaults = new ServerConfig();
        List<NamedFactory<Cipher>> avail = defaults.getCipherFactories();
        return filter(Cipher.class, avail, names);
    }

    public static List<KeyExchangeFactory> buildKexAlgorithms(String[] names) {
        ServerConfig defaults = new ServerConfig();
        List<KeyExchangeFactory> avail = defaults.getKeyExchangeFactories();

        return filter(avail, names);
    }

    public static List<NamedFactory<Signature>> buildSigAlgorithms(String[] names) {
        ServerConfig defaults = new ServerConfig();
        List<NamedFactory<Signature>> avail = defaults.getSignatureAlgorithms();

        return filter(Signature.class, avail, names);
    }

    /**
     * Simple helper class to avoid duplicating available configuration entries.
     */
    private static final class ServerConfig extends ServerBuilder {

        public ServerConfig() {
            this.build();
        }

        /**
         * Just initializes the default configuration - does not create a
         * server instance.
         *
         * @return always <code>null</code>
         */
        @Override
        public SshServer build() {
            return this.build(true);
        }

        /**
         * Just initializes the default configuration - does not create a
         * server instance.
         *
         * @return always <code>null</code>
         */
        @Override
        public SshServer build(boolean isFillWithDefaultValues) {
            if (isFillWithDefaultValues) {
                this.fillWithDefaultValues();
             }
            return null;
        }

        public List<KeyExchangeFactory> getKeyExchangeFactories() {
            return keyExchangeFactories;
        }

        public List<NamedFactory<Cipher>> getCipherFactories() {
            return cipherFactories;
        }

        public List<NamedFactory<Mac>> getMacFactories() {
            return macFactories;
        }

        public List<NamedFactory<Signature>> getSignatureAlgorithms() {
            return signatureFactories;
        }
    }

}
