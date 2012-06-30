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
