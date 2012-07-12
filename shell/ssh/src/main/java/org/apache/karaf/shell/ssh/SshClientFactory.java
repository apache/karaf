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

import org.apache.sshd.SshClient;
import org.apache.sshd.agent.SshAgentFactory;
import org.apache.sshd.client.ServerKeyVerifier;

public class SshClientFactory {

	private SshAgentFactory agentFactory;
	private File knownHosts;
	
	public SshClientFactory(SshAgentFactory agentFactory, File knownHosts) {
		this.agentFactory = agentFactory;
		this.knownHosts = knownHosts;
	}

	public SshClient create(boolean quiet) {
		SshClient client = SshClient.setUpDefaultClient();
        client.setAgentFactory(agentFactory);
        KnownHostsManager knownHostsManager = new KnownHostsManager(knownHosts);
		ServerKeyVerifier serverKeyVerifier = new ServerKeyVerifierImpl(knownHostsManager, quiet);
		client.setServerKeyVerifier(serverKeyVerifier );
		return client;
	}
}
