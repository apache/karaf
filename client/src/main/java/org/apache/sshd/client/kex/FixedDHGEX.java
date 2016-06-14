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
package org.apache.sshd.client.kex;

import javax.crypto.spec.DHParameterSpec;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sshd.common.KeyExchange;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixedDHGEX extends DHGEX {

    /**
     * Named factory for DHGEX key exchange
     */
    public static class Factory implements NamedFactory<KeyExchange> {

        public String getName() {
            return "diffie-hellman-group-exchange-sha1";
        }

        public KeyExchange create() {
            return new FixedDHGEX();
        }

    }

    public FixedDHGEX() {
        min = MIN_DHGEX_KEY_SIZE;
        max = getMaxDHGroupExchangeKeySize();
        prf = Math.min(PREFERRED_DHGEX_KEY_SIZE, max);
    }

    /**
     * System property used to configure the value for the maximum supported Diffie-Hellman
     * Group Exchange key size. If not set, then an internal auto-discovery mechanism is employed.
     * If set to negative value then Diffie-Hellman Group Exchange is disabled. If set to a
     * negative value then Diffie-Hellman Group Exchange is disabled
     */
    public static final String MAX_DHGEX_KEY_SIZE_PROP = "org.apache.sshd.maxDHGexKeySize";

    /**
     * The min. key size value used for testing whether Diffie-Hellman Group Exchange
     * is supported or not. According to <A HREF="https://tools.ietf.org/html/rfc4419">RFC 4419</A>
     * section 3: &quot;Servers and clients SHOULD support groups with a modulus length of k
     * bits, where 1024 <= k <= 8192&quot;.
     * </code>
     */
    public static final int MIN_DHGEX_KEY_SIZE = 1024;
    // Keys of size > 1024 are not support by default with JCE
    public static final int DEFAULT_DHGEX_KEY_SIZE = MIN_DHGEX_KEY_SIZE;
    public static final int PREFERRED_DHGEX_KEY_SIZE = 4096;
    public static final int MAX_DHGEX_KEY_SIZE = 8192;

    private static final AtomicInteger MAX_DHG_KEY_SIZE_HOLDER = new AtomicInteger(0);

    static int getMaxDHGroupExchangeKeySize() {
        int maxSupportedKeySize;
        synchronized (MAX_DHG_KEY_SIZE_HOLDER) {
            maxSupportedKeySize = MAX_DHG_KEY_SIZE_HOLDER.get();
            if (maxSupportedKeySize != 0) { // 1st time we are called ?
                return maxSupportedKeySize;
            }
            String propValue = System.getProperty(MAX_DHGEX_KEY_SIZE_PROP);
            if (propValue == null || propValue.isEmpty()) {
                maxSupportedKeySize = -1;
                // Go down from max. to min. to ensure we stop at 1st maximum value success
                for (int testKeySize = MAX_DHGEX_KEY_SIZE; testKeySize >= MIN_DHGEX_KEY_SIZE; testKeySize -= 1024) {
                    if (isDHGroupExchangeSupported(testKeySize)) {
                        maxSupportedKeySize = testKeySize;
                        break;
                    }
                }
            } else {
                Logger logger = LoggerFactory.getLogger(SecurityUtils.class);
                logger.info("Override max. DH group exchange key size: " + propValue);
                maxSupportedKeySize = Integer.parseInt(propValue);
            }
            MAX_DHG_KEY_SIZE_HOLDER.set(maxSupportedKeySize);
        }
        return maxSupportedKeySize;
    }

    static boolean isDHGroupExchangeSupported(int maxKeySize) {
        try {
            BigInteger r = new BigInteger("0").setBit(maxKeySize - 1);
            DHParameterSpec dhSkipParamSpec = new DHParameterSpec(r, r);
            KeyPairGenerator kpg = SecurityUtils.getKeyPairGenerator("DH");
            kpg.initialize(dhSkipParamSpec);
            return true;
        } catch (GeneralSecurityException t) {
            return false;
        }
    }

}