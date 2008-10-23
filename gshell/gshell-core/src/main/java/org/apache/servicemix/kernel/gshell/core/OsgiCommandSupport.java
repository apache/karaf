package org.apache.servicemix.kernel.gshell.core;

import org.apache.geronimo.gshell.command.CommandAction;
import org.apache.geronimo.gshell.command.CommandContext;
import org.apache.geronimo.gshell.io.IO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.osgi.context.BundleContextAware;
import org.osgi.framework.BundleContext;

public abstract class OsgiCommandSupport implements CommandAction, BundleContextAware {

    protected Log log = LogFactory.getLog(getClass());
    protected BundleContext bundleContext;
    protected CommandContext commandContext;
    protected IO io;

    public Object execute(CommandContext commandContext) throws Exception {
        this.commandContext = commandContext;
        this.io = commandContext.getIo();
        return doExecute();
    }

    protected abstract Object doExecute() throws Exception;

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
