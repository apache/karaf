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

import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.cipher.BuiltinCiphers;
import org.apache.sshd.common.cipher.Cipher;
import org.apache.sshd.common.mac.BuiltinMacs;
import org.apache.sshd.common.mac.Mac;

public class SshUtils {

    public static <S> List<NamedFactory<S>> filter(Collection<NamedFactory<S>> factories, String names) {
        List<NamedFactory<S>> list = new ArrayList<NamedFactory<S>>();
        for (String name : names.split(",")) {
            for (NamedFactory<S> factory : factories) {
                if (factory.getName().equals(name)) {
                    list.add(factory);
                }
            }
        }
        return list;
    }

    public static List<NamedFactory<Mac>> buildMacs(String names) {
        return filter(Arrays.<NamedFactory<Mac>>asList(
                        BuiltinMacs.hmacmd5,
                        BuiltinMacs.hmacsha1,
                        BuiltinMacs.hmacmd596,
                        BuiltinMacs.hmacsha196),
                names);
    }

    public static List<NamedFactory<Cipher>> buildCiphers(String names) {
        List<NamedFactory<Cipher>> avail = new LinkedList<NamedFactory<Cipher>>();
        avail.add(BuiltinCiphers.aes128ctr);
        avail.add(BuiltinCiphers.aes256ctr);
        avail.add(BuiltinCiphers.arcfour128);
        avail.add(BuiltinCiphers.arcfour256);
        avail.add(BuiltinCiphers.aes128cbc);
        avail.add(BuiltinCiphers.tripledescbc);
        avail.add(BuiltinCiphers.blowfishcbc);
        avail.add(BuiltinCiphers.aes192cbc);
        avail.add(BuiltinCiphers.aes256cbc);

        avail = filter(avail, names);

        for (Iterator<NamedFactory<Cipher>> i = avail.iterator(); i.hasNext();) {
            final NamedFactory<Cipher> f = i.next();
            try {
                final Cipher c = f.create();
                final byte[] key = new byte[c.getBlockSize()];
                final byte[] iv = new byte[c.getIVSize()];
                c.init(Cipher.Mode.Encrypt, key, iv);
            } catch (InvalidKeyException e) {
                i.remove();
            } catch (Exception e) {
                i.remove();
            }
        }
        return avail;
    }

}
