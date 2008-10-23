package org.apache.servicemix.kernel.gshell.core;

import java.util.List;
import java.util.ArrayList;

import org.apache.geronimo.gshell.command.CommandAction;
import org.apache.geronimo.gshell.command.CommandContext;
import org.apache.geronimo.gshell.command.Variables;
import org.apache.geronimo.gshell.io.IO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.osgi.context.BundleContextAware;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public abstract class OsgiCommandSupport implements CommandAction, BundleContextAware {

    protected Log log = LogFactory.getLog(getClass());
    protected BundleContext bundleContext;
    protected CommandContext commandContext;
    protected IO io;
    protected Variables variables;
    protected List<ServiceReference> usedReferences;
    
    public Object execute(CommandContext commandContext) throws Exception {
        this.commandContext = commandContext;
        this.io = commandContext.getIo();
        this.variables = commandContext.getVariables();
        try {
            return doExecute();
        } finally {
            ungetServices();
        }
    }

    protected abstract Object doExecute() throws Exception;

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    protected <T> List<T> getAllServices(Class<T> clazz, String filter) throws Exception {
        ServiceReference[] references = getBundleContext().getAllServiceReferences(clazz.getName(), filter);
        if (references == null) {
            return null;
        }
        List<T> services = new ArrayList<T>();
        for (ServiceReference ref : references) {
            T t = getService(clazz, ref);
            services.add(t);
        }
        return services;
    }

    protected <T> T getService(Class<T> clazz, ServiceReference reference) {
        T t = (T) getBundleContext().getService(reference);
        if (t != null) {
            if (usedReferences == null) {
                usedReferences = new ArrayList<ServiceReference>();
            }
            usedReferences.add(reference);
        }
        return t;
    }

    protected void ungetServices() {
        if (usedReferences != null) {
            for (ServiceReference ref : usedReferences) {
                getBundleContext().ungetService(ref);
            }
        }
    }

}
