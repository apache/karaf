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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import org.apache.mina.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KnownHostsManager {
	Logger LOG = LoggerFactory.getLogger(KnownHostsManager.class);
	
	private final File knownHosts;

	public KnownHostsManager(File knownHosts) {
		this.knownHosts = knownHosts;
		this.knownHosts.getParentFile().mkdirs();
		if (!this.knownHosts.exists()) {
			try {
				knownHosts.createNewFile();
			} catch (IOException e) {
				throw new RuntimeException("Error creating file for known hosts at: " + knownHosts);
			}
		}
	}
	
	public PublicKey getKnownKey(SocketAddress remoteAddress, String checkAlgorithm) throws InvalidKeySpecException {
		FileReader fr = null;
		BufferedReader reader = null;
		try {
			fr = new FileReader(knownHosts);
			reader = new BufferedReader(fr);
			return getKnownKeyInternal(remoteAddress, checkAlgorithm, reader);
		} catch (IOException e) {
			throw new RuntimeException("Error reading known_hosts " + knownHosts, e);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} finally {
			close(reader);
			close(fr);
		}
	}

	private PublicKey getKnownKeyInternal(SocketAddress remoteAddress,
			String checkAlgorithm, BufferedReader reader) throws IOException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		String checkServerAddress = getAddressString(remoteAddress);

		String line = reader.readLine();
		while (line != null) {
			String[] lineParts = line.split(" ");
			String serverAddress = lineParts[0];
			String algorithm = lineParts[1];
			if (checkServerAddress.equals(serverAddress) && checkAlgorithm.equals(algorithm)) {
				byte[] key = Base64.decodeBase64(lineParts[2].getBytes());
				KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
				X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key);
				return keyFactory.generatePublic(keySpec);
			}
			line = reader.readLine();
		}
		return null;
	}
	
	public void storeKeyForHost(SocketAddress remoteAddress,
			PublicKey serverKey) {
		FileWriter ps = null;
		BufferedWriter bw = null;
		try {
			ps = new FileWriter(knownHosts, true);
			bw = new BufferedWriter(ps);
			writeKey(bw, remoteAddress, serverKey);
		} catch (Exception e) {
			throw new RuntimeException("Error storing key for host" + remoteAddress, e);
		} finally {
			close(bw);
			close(ps);
		}
	}

	private void writeKey(BufferedWriter bw, SocketAddress remoteAddress,
			PublicKey serverKey) throws IOException {
		bw.append(getAddressString(remoteAddress));
		bw.append(" ");
		bw.append(serverKey.getAlgorithm());
		bw.append(" ");
		serverKey.getEncoded();
		bw.append(new String(Base64.encodeBase64(serverKey.getEncoded()),
				"UTF-8"));
	}

	String getAddressString(SocketAddress address) {
		if (address instanceof InetSocketAddress) {
			InetSocketAddress inetAddress = (InetSocketAddress) address;
			return String.format("%s,%s:%s", inetAddress.getHostName(),
					inetAddress.getAddress().getHostAddress(),
					inetAddress.getPort());
		}
		return "";
	}
	
	private void close(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
				LOG.warn("Error closing: " + e.getMessage(), e);
			}
		}
	}

}
