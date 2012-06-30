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

import java.io.IOException;
import java.net.SocketAddress;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import org.apache.sshd.ClientSession;
import org.apache.sshd.client.ServerKeyVerifier;

public class ServerKeyVerifierImpl implements ServerKeyVerifier {
    private final KnownHostsManager knownHostsManager;
	private final boolean quiet;

	public ServerKeyVerifierImpl(KnownHostsManager knownHostsManager, boolean quiet) {
		this.knownHostsManager = knownHostsManager;
		this.quiet = quiet;
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
			boolean confirm;
			if (!quiet) {
				System.out.println("Connecting to unknown server. Add this server to known hosts ? (y/n)");
				confirm = getConfirmation();
			} else {
				System.out.println("Connecting to unknown server. Automatically adding to known hosts.");
				confirm = true;
			}
			if (confirm) {
				knownHostsManager.storeKeyForHost(remoteAddress, serverKey);
				System.out.println("Storing the server key in known_hosts.");
			} else {
				System.out.println("Aborting connection");
			}
			return confirm;
		}
		
		boolean verifed = (knownKey.equals(serverKey));
		if (!verifed) {
			System.err.println("Server key for host " + remoteAddress + " does not match the stored key !! Terminating session.");
		}
		return verifed;
	}

	private boolean getConfirmation() {
		int ch;
		try {
			do {
				ch = System.in.read();
			} while (ch != 'y' && ch != 'n');
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		boolean confirm = ch == 'y';
		return confirm;
	}



}
