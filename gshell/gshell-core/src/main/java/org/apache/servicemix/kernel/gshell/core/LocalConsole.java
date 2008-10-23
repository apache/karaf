package org.apache.servicemix.kernel.gshell.core;

import javax.annotation.PreDestroy;
import javax.annotation.PostConstruct;

import org.apache.geronimo.gshell.shell.Shell;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalConsole implements Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Shell shell;

    private boolean createLocalShell;

    public Shell getShell() {
        return shell;
    }

    public void setShell(Shell shell) {
        this.shell = shell;
    }

    public boolean isCreateLocalShell() {
        return createLocalShell;
    }

    public void setCreateLocalShell(boolean createLocalShell) {
        this.createLocalShell = createLocalShell;
    }

    @PostConstruct
    public void init() {
        if (createLocalShell) {
            new Thread(this, "localShell").start();
        }
    }

    @PreDestroy
    public void destroy() {
        if (createLocalShell) {
            shell.close();
        }
    }

    public void run() {
        try {
            shell.run();
        } catch (Exception e2) {
            log.error("Error running shell", e2);
        } finally {
            shell.close();
        }
    }
}
