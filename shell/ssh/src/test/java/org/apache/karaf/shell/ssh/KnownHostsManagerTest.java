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

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import org.junit.Assert;
import org.junit.Test;

public class KnownHostsManagerTest {
	private static final String ALGORITHM = "DSA";

	private PublicKey createPubKey() throws NoSuchAlgorithmException {
		KeyPairGenerator gen = KeyPairGenerator.getInstance(ALGORITHM);
		KeyPair keyPair = gen.generateKeyPair();
		return keyPair.getPublic();
	}
	
	@Test
	public void testStoreAndRetrieve() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		SocketAddress address = new InetSocketAddress("localhost", 1001);
		File hostsFile = File.createTempFile("hosts", "");
		KnownHostsManager manager = new KnownHostsManager(hostsFile);

		PublicKey foundKey1 = manager.getKnownKey(address, ALGORITHM);
		Assert.assertNull(foundKey1);
		
		PublicKey serverKey = createPubKey();
		manager.storeKeyForHost(address, serverKey);
		PublicKey foundKey2 = manager.getKnownKey(address, ALGORITHM);
		Assert.assertEquals(serverKey, foundKey2);
	}
	
}
