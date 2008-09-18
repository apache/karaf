package org.apache.felix.dependencymanager.shell;

import org.apache.felix.shell.Command;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
    private ServiceRegistration m_serviceRegistration;

    public void start(BundleContext context) throws Exception {
        m_serviceRegistration = context.registerService(Command.class.getName(), new DMCommand(context), null);
    }

    public void stop(BundleContext context) throws Exception {
        m_serviceRegistration.unregister();
    }
}
