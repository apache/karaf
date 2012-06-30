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

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class ServerKeyVerifierImplTest {

	private static final InetSocketAddress LOCALHOST = new InetSocketAddress("localhost", 1001);
	private static final String ALGORITHM = "DSA";

	private PublicKey createPubKey() throws NoSuchAlgorithmException {
		KeyPairGenerator gen = KeyPairGenerator.getInstance(ALGORITHM);
		KeyPair keyPair = gen.generateKeyPair();
		return keyPair.getPublic();
	}
	
	@Test
	public void testNewKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
		SocketAddress address = LOCALHOST;
		PublicKey validServerKey = createPubKey();
		
		KnownHostsManager knowHostsManager = EasyMock.createMock(KnownHostsManager.class);
		EasyMock.expect(knowHostsManager.getKnownKey(address, ALGORITHM)).andReturn(null);
		knowHostsManager.storeKeyForHost(address, validServerKey);
		EasyMock.expectLastCall();
		EasyMock.replay(knowHostsManager);

		ServerKeyVerifierImpl verifier = new ServerKeyVerifierImpl(knowHostsManager, true);		
		boolean verified = verifier.verifyServerKey(null, address, validServerKey);
		Assert.assertTrue("Key should be verified as the key is new", verified);
	}
	
	@Test
	public void testKnownAndCorrectKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
		SocketAddress address = LOCALHOST;
		PublicKey validServerKey = createPubKey();
		
		KnownHostsManager knowHostsManager = EasyMock.createMock(KnownHostsManager.class);
		EasyMock.expect(knowHostsManager.getKnownKey(address, ALGORITHM)).andReturn(validServerKey);
		EasyMock.replay(knowHostsManager);

		ServerKeyVerifierImpl verifier = new ServerKeyVerifierImpl(knowHostsManager, true);		
		boolean verified = verifier.verifyServerKey(null, address, validServerKey);
		Assert.assertTrue("Key should be verified as the key is known and matches the key we verify", verified);
	}
	
	@Test
	public void testKnownAndIncorrectKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
		SocketAddress address = LOCALHOST;
		PublicKey validServerKey = createPubKey();
		PublicKey otherServerKey = createPubKey();
		
		KnownHostsManager knowHostsManager = EasyMock.createMock(KnownHostsManager.class);
		EasyMock.expect(knowHostsManager.getKnownKey(address, ALGORITHM)).andReturn(otherServerKey);
		EasyMock.replay(knowHostsManager);

		ServerKeyVerifierImpl verifier = new ServerKeyVerifierImpl(knowHostsManager, true);		
		boolean verified = verifier.verifyServerKey(null, address, validServerKey);
		Assert.assertFalse("Key should not be verified as the key is known and does not match the key we verify", verified);
	}
}
