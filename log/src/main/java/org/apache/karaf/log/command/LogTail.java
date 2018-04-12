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
package org.apache.karaf.log.command;

import java.io.PrintStream;

import org.apache.karaf.log.core.LogService;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.ops4j.pax.logging.spi.PaxAppender;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

@Command(scope = "log", name = "tail", description = "Continuously display log entries. Use ctrl-c to quit this command")
@Service
public class LogTail extends DisplayLog {
    @Reference
    Session session;

    @Reference
    BundleContext context;

    @Override
    public Object execute() throws Exception {
        if (entries == 0) {
            entries = 50;
        }
        int minLevel = getMinLevel(level);
        // Do not use System.out as it may write to the wrong console depending on the thread that calls our log handler
        PrintStream out = session.getConsole();
        display(out, minLevel);
        out.flush();

        PaxAppender appender = event -> printEvent(out, event, minLevel);
        ServiceTracker<LogService, LogService> tracker = new LogServiceTracker(context, LogService.class, null, appender);
        tracker.open();
        try {
            synchronized (this) {
                wait();
            }
            out.println("Stopping tail as log.core bundle was stopped.");
        } catch (InterruptedException e) {
            // Ignore as it will happen if the user breaks the tail using Ctrl-C
        } finally {
            tracker.close();
        }
        out.println();
        return null;
    }
    
    private synchronized void stopTail() {
        notifyAll();
    }

    /**
     * Track LogService dynamically so we can react when the log core bundle stops even while we block for the tail
     */
    private final class LogServiceTracker extends ServiceTracker<LogService, LogService> {

        private final static String SSHD_LOGGER = "org.apache.sshd";

        private final PaxAppender appender;

        private String sshdLoggerLevel;
    
        private LogServiceTracker(BundleContext context, Class<LogService> clazz,
                                  ServiceTrackerCustomizer<LogService, LogService> customizer,
                                  PaxAppender appender) {
            super(context, clazz, customizer);
            this.appender = appender;
        }
    
        @Override
        public LogService addingService(ServiceReference<LogService> reference) {
            LogService service = super.addingService(reference);
            sshdLoggerLevel = service.getLevel(SSHD_LOGGER).get(SSHD_LOGGER);
            service.setLevel(SSHD_LOGGER, "ERROR");
            service.addAppender(appender);
            return service;
        }
    
        @Override
        public void removedService(ServiceReference<LogService> reference, LogService service) {
            if (sshdLoggerLevel != null) {
                service.setLevel(SSHD_LOGGER, sshdLoggerLevel);
            }
            service.removeAppender(appender);
            stopTail();
        }
    }

}
