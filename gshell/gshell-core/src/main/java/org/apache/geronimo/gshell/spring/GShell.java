/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.geronimo.gshell.spring;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.geronimo.gshell.DefaultEnvironment;
import org.apache.geronimo.gshell.DefaultShell;
import org.apache.geronimo.gshell.ExitNotification;
import org.apache.geronimo.gshell.command.IO;
import org.apache.geronimo.gshell.common.Arguments;
import org.apache.geronimo.gshell.console.Console;
import org.apache.geronimo.gshell.layout.NotFoundException;
import org.apache.geronimo.gshell.shell.Environment;
import org.apache.geronimo.gshell.shell.InteractiveShell;
import org.apache.servicemix.kernel.main.spi.MainService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.osgi.context.BundleContextAware;

/**
 * This class represents the local shell console and is also used when passing a command to execute on the command line.
 * Such mechanism is done using the {@link MainService} service registered in the OSGi registry, which contains the
 * command line arguments and a place holder for the exit code.
 */
public class GShell implements Runnable, BundleContextAware {

    private static final Logger log = LoggerFactory.getLogger(GShell.class);

    private InteractiveShell shell;
    private Thread thread;
    private IO io;
    private Environment env;
    private boolean start;
    private MainService mainService;
    private BundleContext bundleContext;
    private CountDownLatch frameworkStarted;
    private volatile boolean closed;

    public GShell(InteractiveShell shell) throws IOException {
        this.shell = shell;
        if (shell instanceof DefaultShell) {
            DefaultShell sh = (DefaultShell) shell;
            sh.setErrorHandler(wrapErrorHandler(sh.getErrorHandler()));
        }
        this.io = new IO(new NoCloseInputStream(System.in),
                         new NoCloseOutputStream(System.out),
                         new NoCloseOutputStream(System.err));
        this.env = new DefaultEnvironment(new ProxyIO());
    }

    public void setStart(boolean start) {
        this.start = start;
    }

    public void start() {
        frameworkStarted = new CountDownLatch(1);
		getBundleContext().addFrameworkListener(new FrameworkListener(){
			public void frameworkEvent(FrameworkEvent event) {
				log.debug("Got event: " + event.getType());
				if( event.getType() == FrameworkEvent.STARTED ) {
					frameworkStarted.countDown();
				}
			}
		});
        if (start) {
            thread = new Thread(this);
            thread.start();
        }
    }

    public void stop() throws Exception {
        closed = true;
        io.close();
        if (thread != null) {
            frameworkStarted.countDown();
            thread.interrupt();
            thread.join();
            thread = null;
        }
    }

    public void run() {
        try {
            ProxyIO.setIO(io);
            EnvironmentTargetSource.setEnvironment(env);

            String[] args = null;
            if (mainService != null) {
                args = mainService.getArgs();
            }

            // If a command was specified on the command line, then just execute that command.
            if (args != null && args.length > 0) {
                waitForFrameworkToStart();
                log.info("Executing Shell with arguments: " + Arguments.asString(args));
                Object value = shell.execute((Object[]) args);
                if (mainService != null) {
                    if (value instanceof Number) {
                        mainService.setExitCode(((Number) value).intValue());
                    } else {
                        mainService.setExitCode(value != null ? 1 : 0);
                    }
                    log.info("Exiting shell due to terminated command");
                    try {
                        getBundleContext().getBundle(0).stop();
                    } catch (BundleException e2) {
                        log.info("Caught exception while shutting down framework: " + e2, e2);
                    }
                }
            } else {
                // Otherwise go into a command shell.
                shell.run();
            }

        } catch (Throwable e) {
            if (e instanceof ExitNotification) {
                if (closed) {
                    // Ignore notifications coming because the spring app has been destroyed
                    return;
                }
                if (mainService != null) {
                    mainService.setExitCode(0);
                }
                log.info("Exiting shell due received exit notification");
            } else {
                if (mainService != null) {
                    mainService.setExitCode(-1);
                }
                log.info("Exiting shell due to caught exception " + e, e);
            }
            new Thread() {
                public void run() {
                    try {
                        getBundleContext().getBundle(0).stop();
                    } catch (BundleException e2) {
                        log.info("Caught exception while shutting down framework: " + e2, e2);
                    }
                }
            }.start();
        }
    }

    /**
     * Blocks until the framework has finished starting.  We do this so that any installed
     * bundles for commands get fully registered.
     *
     * @throws InterruptedException
     */
    private void waitForFrameworkToStart() throws InterruptedException {
		if( frameworkStarted.await(5, TimeUnit.SECONDS) ) {
			log.info("System completed startup.");
		} else {
			log.warn("System took too long startup... continuing");
		}
    }

    public MainService getMainService() {
        return mainService;
    }

    public void setMainService(MainService main) {
        this.mainService = main;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
	}

    protected Console.ErrorHandler wrapErrorHandler(final Console.ErrorHandler handler) {
        return new Console.ErrorHandler() {
            public Result handleError(Throwable error) {
                if (closed) {
                    throw new ExitNotification();
                } else if (error instanceof NotFoundException) {
                    // Spit out the terse reason why we've failed
                    io.err.println("@|bold,red ERROR| Command not found: @|bold,red " + error.getMessage() + "|");
                    return Result.CONTINUE;
                } else {
                    return handler.handleError(error);
                }
            }
        };
    }


}
