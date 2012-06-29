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

import java.net.SocketAddress;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import org.apache.sshd.ClientSession;
import org.apache.sshd.client.ServerKeyVerifier;

public class ServerKeyVerifierImpl implements ServerKeyVerifier {
    private final KnownHostsManager knownHostsManager;

	public ServerKeyVerifierImpl(KnownHostsManager knownHostsManager) {
		this.knownHostsManager = knownHostsManager;
    	
	}

	@Override
	public boolean verifyServerKey(ClientSession sshClientSession,
			SocketAddress remoteAddress, PublicKey serverKey) {
		PublicKey knownKey;
		try {
			knownKey = knownHostsManager.getKnownKey(remoteAddress, serverKey.getAlgorithm());
		} catch (InvalidKeySpecException e) {
			System.err.println("Invalid key stored for host " + remoteAddress + ". Terminating session.");
			return false;
		}
		if (knownKey == null) {
			System.out.println("Connecting to this server for the first time. Storing the server key.");
			knownHostsManager.storeKeyForHost(remoteAddress, serverKey);
			return true;
		}
		
		boolean verifed = (knownKey.equals(serverKey));
		if (!verifed) {
			System.err.println("Server key for host " + remoteAddress + " does not match the stored key !! Terminating session.");
		}
		return verifed;
	}



}
