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

import java.math.BigInteger;

import org.apache.sshd.common.KeyExchange;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.digest.SHA256;
import org.apache.sshd.common.kex.DH;

public class FixedDHGEX256 extends FixedDHGEX {

    /**
     * Named factory for DHGEX key exchange
     */
    public static class Factory implements NamedFactory<KeyExchange> {

        public String getName() {
            return "diffie-hellman-group-exchange-sha256";
        }

        public KeyExchange create() {
            return new FixedDHGEX256();
        }

    }

    public FixedDHGEX256() {
    }

    @Override
    protected DH getDH(BigInteger p, BigInteger g) throws Exception {
        DH dh = new DH(new SHA256.Factory());
        dh.setP(p);
        dh.setG(g);
        return dh;
    }

}