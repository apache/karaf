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
package org.apache.karaf.bundle.command;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import jline.console.ConsoleReader;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.apache.karaf.shell.inject.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.wiring.FrameworkWiring;

@Command(scope = "bundle", name = "load-test", description = "Load test bundle lifecycle")
@Service
public class LoadTest extends OsgiCommandSupport {

    @Option(name = "--threads", description = "number of concurrent threads")
    int threads = 2;

    @Option(name = "--delay", description = "maximum delay between actions")
    int delay = 1;

    @Option(name = "--iterations", description = "number of iterations per thread")
    int iterations = 100;

    @Option(name = "--refresh", description = "percentage of bundle refresh vs restart")
    int refresh = 20;

    @Option(name = "--excludes", description = "List of bundles (ids or symbolic names) to exclude")
    List<String> excludes = Arrays.asList("0", "org.ops4j.pax.url.mvn", "org.ops4j.pax.logging.pax-logging-api", "org.ops4j.pax.logging.pax-logging-service");

    @Override
    protected Object doExecute() throws Exception {
        if (!confirm(session)) {
            return null;
        }
        final BundleContext bundleContext = getBundleContext().getBundle(0).getBundleContext();
        final FrameworkWiring wiring = bundleContext.getBundle().adapt(FrameworkWiring.class);
        final CountDownLatch latch = new CountDownLatch(threads);
        final Bundle[] bundles = bundleContext.getBundles();
        final AtomicBoolean[] locks = new AtomicBoolean[bundles.length];
        for (int b = 0; b < locks.length; b++) {
            locks[b] = new AtomicBoolean(true);
            // Avoid touching excluded bundles
            if (excludes.contains(Long.toString(bundles[b].getBundleId()))
                    || excludes.contains(bundles[b].getSymbolicName())) {
                continue;
            }
            // Only touch active bundles
            if (bundles[b].getState() != Bundle.ACTIVE) {
                continue;
            }
            // Now set the lock to available
            locks[b].set(false);
        }
        for (int i = 0; i < threads; i++) {
            new Thread() {
                public void run() {
                    try {
                        Random rand = new Random();
                        for (int j = 0; j < iterations; j++) {
                            for (;;) {
                                int b = rand.nextInt(bundles.length);
                                if (locks[b].compareAndSet(false, true)) {
                                    try {
                                        // Only touch active bundles
                                        if (bundles[b].getState() != Bundle.ACTIVE) {
                                            continue;
                                        }
                                        if (rand.nextInt(100) < refresh) {
                                            try {
                                                bundles[b].update();
                                                final CountDownLatch latch = new CountDownLatch(1);
                                                wiring.refreshBundles(Collections.singletonList(bundles[b]), new FrameworkListener() {
                                                    public void frameworkEvent(FrameworkEvent event) {
                                                        latch.countDown();
                                                    }
                                                });
                                                latch.await();
                                            } finally {
                                                while (true) {
                                                    try {
                                                        bundles[b].start(Bundle.START_TRANSIENT);
                                                        break;
                                                    } catch (Exception e) {
                                                        Thread.sleep(1);
                                                    }
                                                }
                                            }
                                        } else {
                                            try {
                                                bundles[b].stop(Bundle.STOP_TRANSIENT);
                                            } finally {
                                                while (true) {
                                                    try {
                                                        bundles[b].start(Bundle.START_TRANSIENT);
                                                        break;
                                                    } catch (Exception e) {
                                                        Thread.sleep(1);
                                                    }
                                                }
                                            }
                                        }
                                        Thread.sleep(rand.nextInt(delay));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    } finally {
                                        locks[b].set(false);
                                    }
                                }
                                break;
                            }
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }
            }.start();
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.err.println("Load test finished");
            }
        }.start();
        return null;
    }

    private boolean confirm(CommandSession session) throws IOException {
        for (;;) {
            ConsoleReader reader = (ConsoleReader) session.get(".jline.reader");
            String msg = "You are about to perform a start/stop/refresh load test on bundles.\nDo you wish to continue (yes/no): ";
            String str = reader.readLine(msg);
            if ("yes".equalsIgnoreCase(str)) {
                return true;
            }
            if ("no".equalsIgnoreCase(str)) {
                return false;
            }
        }
    }
}
