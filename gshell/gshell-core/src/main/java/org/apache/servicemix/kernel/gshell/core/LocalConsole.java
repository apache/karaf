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
package org.apache.servicemix.kernel.gshell.core;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;

import org.apache.geronimo.gshell.shell.Shell;
import org.apache.geronimo.gshell.notification.ExitNotification;
import org.apache.servicemix.kernel.main.spi.MainService;
import org.springframework.osgi.context.BundleContextAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkEvent;

public class LocalConsole implements Runnable, BundleContextAware {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Shell shell;

    private boolean createLocalShell;

    private BundleContext bundleContext;

    private MainService mainService;

    private CountDownLatch frameworkStarted;
    
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public MainService getMainService() {
        return mainService;
    }

    public void setMainService(MainService mainService) {
        this.mainService = mainService;
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

    public void init() {
        frameworkStarted = new CountDownLatch(1);
		getBundleContext().addFrameworkListener(new FrameworkListener(){
			public void frameworkEvent(FrameworkEvent event) {
				log.debug("Got event: " + event.getType());
				if( event.getType() == FrameworkEvent.STARTED ) {
					frameworkStarted.countDown();
				}
			}
		});
        if (createLocalShell) {
            new Thread(this, "localShell").start();
        }
    }

    public void destroy() {
        if (createLocalShell) {
            shell.close();
        }
    }

    public void run() {
        try {
            String[] args = mainService.getArgs();
            // If a command was specified on the command line, then just execute that command.
            if (args != null && args.length > 0) {
                waitForFrameworkToStart();
                log.info("Executing Shell with arguments: " + Arrays.toString(args));
                StringBuilder sb = new StringBuilder();
                for (String arg : args) {
                    sb.append(arg).append(" ");
                }
                Object value = shell.execute(sb.toString());
                if (mainService != null) {
                    if (value instanceof Number) {
                        mainService.setExitCode(((Number) value).intValue());
                    } else {
                        mainService.setExitCode(value != null ? 1 : 0);
                    }
                    log.info("Exiting shell due to terminated command");
                }
            } else {
                shell.run();
            }
        } catch (ExitNotification e) {
            if (mainService != null) {
                mainService.setExitCode(0);
            }
            log.info("Exiting shell due received exit notification");
        } catch (Throwable e) {
            if (mainService != null) {
                mainService.setExitCode(-1);
            }
            log.error("Exiting shell due to caught exception " + e, e);
        } finally {
            try {
                shell.close();
            } catch (Throwable t) {}
            asyncShutdown();
        }
    }

    /**
     * Blocks until the framework has finished starting.  We do this so that any installed
     * bundles for commands get fully registered.
     *
     * @throws InterruptedException
     */
    private void waitForFrameworkToStart() throws InterruptedException {
        log.info("Waiting from framework to start.");
        if (frameworkStarted.await(60, TimeUnit.SECONDS)) {
			log.info("System completed startup.");
		} else {
			log.warn("System took too long startup... continuing");
		}
    }

    private void asyncShutdown() {
        new Thread() {
            public void run() {
                try {
                    getBundleContext().getBundle(0).stop();
                } catch (BundleException e) {
                    log.info("Caught exception while shutting down framework: " + e, e);
                }
            }
        }.start();
    }

}
