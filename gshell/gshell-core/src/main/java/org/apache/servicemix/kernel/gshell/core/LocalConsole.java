package org.apache.servicemix.kernel.gshell.core;

import javax.annotation.PreDestroy;
import javax.annotation.PostConstruct;

import org.apache.geronimo.gshell.shell.Shell;
import org.apache.geronimo.gshell.notification.ExitNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.osgi.context.BundleContextAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class LocalConsole implements Runnable, BundleContextAware {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Shell shell;

    private boolean createLocalShell;

    private BundleContext bundleContext;

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

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
        } catch (ExitNotification e) {
            new Thread() {
                public void run() {
                    try {
                        Bundle bundle = getBundleContext().getBundle(0);
                        bundle.stop();
                    } catch (Exception e) {
                        log.error("Error when shutting down ServiceMix Kernel", e);
                    }
                }
            }.start();
        } catch (Throwable e) {
            log.error("Error running shell", e);
        } finally {
            shell.close();
        }
    }
}
