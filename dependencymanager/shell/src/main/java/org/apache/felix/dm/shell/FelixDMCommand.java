package org.apache.felix.dm.shell;

import org.apache.felix.shell.Command;
import org.osgi.framework.BundleContext;

public class FelixDMCommand extends DMCommand implements Command {
    public FelixDMCommand(BundleContext context) {
        super(context);
    }
}
