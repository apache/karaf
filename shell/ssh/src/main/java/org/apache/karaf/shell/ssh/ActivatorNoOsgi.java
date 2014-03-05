package org.apache.karaf.shell.ssh;

import java.io.File;

import org.apache.karaf.shell.api.action.lifecycle.Destroy;
import org.apache.karaf.shell.api.action.lifecycle.Init;
import org.apache.karaf.shell.api.action.lifecycle.Manager;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.console.SessionFactory;

public class ActivatorNoOsgi {

    @Reference
    SessionFactory sessionFactory;

    KarafAgentFactory agentFactory;
    SshClientFactory sshClientFactory;

    @Init
    public void init() {
        agentFactory = new KarafAgentFactory();
        sshClientFactory = new SshClientFactory(agentFactory, new File(System.getProperty("user.home"), ".sshkaraf/known_hosts"));
        sessionFactory.getRegistry().register(sshClientFactory);
        sessionFactory.getRegistry().getService(Manager.class).register(SshAction.class);
    }

    @Destroy
    public void destroy() {
        sessionFactory.getRegistry().register(sshClientFactory);
        sessionFactory.getRegistry().getService(Manager.class).register(SshAction.class);
    }

}
