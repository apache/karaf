package org.apache.karaf.system.commands;

import org.apache.karaf.shell.console.AbstractAction;
import org.apache.karaf.system.SystemService;

public abstract class AbstractSystemAction extends AbstractAction {
    protected SystemService systemService;

    public void setSystemService(SystemService systemService) {
        this.systemService = systemService;
    }
}
