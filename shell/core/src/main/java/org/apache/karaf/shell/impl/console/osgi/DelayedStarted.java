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
package org.apache.karaf.shell.impl.console.osgi;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.startlevel.FrameworkStartLevel;

/**
 * Delay the start of the console until the desired start level is reached or enter is pressed
 */
class DelayedStarted extends Thread implements FrameworkListener {
    private static final String SYSTEM_PROP_KARAF_CONSOLE_STARTED = "karaf.console.started";

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final InputStream in;
    private final Runnable console;
    private final BundleContext bundleContext;

    DelayedStarted(Runnable console, String name, BundleContext bundleContext, InputStream in) {
        super(name);
        this.console = console;
        this.bundleContext = bundleContext;
        this.in = in;
        int defaultStartLevel = Integer.parseInt(System.getProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL));
        int startLevel = bundleContext.getBundle(0).adapt(FrameworkStartLevel.class).getStartLevel();
        if (startLevel >= defaultStartLevel) {
            started.set(true);
        } else {
            bundleContext.addFrameworkListener(this);
            frameworkEvent(new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(), null));
        }
    }

    public void run() {
        try {
            while (!started.get() && !stopped.get()) {
                if (in.available() == 0) {
                    Thread.sleep(10);
                }
                while (in.available() > 0) {
                    char ch = (char) in.read();
                    if (ch == '\r' || ch == '\n') {
                        started.set(true);
                        break;
                    }
                }
            }
        } catch (Throwable t) {
            // Ignore
        }


        if (!stopped.get()) {
            // Signal to the main module that it can stop displaying the startup progress
            System.setProperty(SYSTEM_PROP_KARAF_CONSOLE_STARTED, "true");
            this.bundleContext.removeFrameworkListener(this);
            console.run();
        }
    }

    public void stopDelayed() {
        stopped.set(true);
    }

    public void frameworkEvent(FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.STARTLEVEL_CHANGED) {
            int defaultStartLevel = Integer.parseInt(System.getProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL));
            int startLevel = this.bundleContext.getBundle(0).adapt(FrameworkStartLevel.class).getStartLevel();
            if (startLevel >= defaultStartLevel) {
                started.set(true);
            }
        }
    }
}
