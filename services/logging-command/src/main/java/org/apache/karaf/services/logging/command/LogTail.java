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
package org.apache.karaf.services.logging.command;

import java.io.PrintStream;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.util.tracker.ServiceTracker;

@Command(scope = "log", name = "tail", description = "Continuously display log entries. Use ctrl-c to quit this command")
@Service
public class LogTail extends DisplayLog {

    static final int QUEUE_CAPACITY = 10000;
    private static final String SSHD_LOGGER = "org.apache.sshd";

    @Reference
    Session session;

    @Reference
    BundleContext bundleContext;

    @Override
    public Object execute() throws Exception {
        if (entries == 0) {
            entries = 50;
        }
        LogLevel minLevel = getMinLevel(level);
        LogEntryFormatter formatter = createFormatter();
        // Do not use System.out as it may write to the wrong console depending on the thread
        PrintStream out = session.getConsole();

        Tail tail = new Tail(entry -> accept(entry, minLevel) && !isSshdEntry(entry));
        LogReaderTracker tracker = new LogReaderTracker(bundleContext, tail);
        tracker.open();
        try {
            // the listener is added before reading the log, so that no entry is lost in between
            for (LogEntry entry : tail.start(Collections.list(logReaderService.getLog()), entries)) {
                if (accept(entry, minLevel)) {
                    out.append(formatter.format(entry, noColor));
                }
            }
            out.flush();
            while (true) {
                LogEntry entry = tail.next(500, TimeUnit.MILLISECONDS);
                long dropped = tail.dropped();
                if (dropped > 0) {
                    out.println("... " + dropped + " log entries dropped");
                }
                if (entry != null) {
                    out.append(formatter.format(entry, noColor));
                    out.flush();
                } else if (tracker.isStopped()) {
                    out.println("Stopping tail as the log service was unregistered.");
                    break;
                }
            }
        } catch (InterruptedException e) {
            // Ignore as it will happen if the user breaks the tail using Ctrl-C
        } finally {
            tracker.close();
        }
        out.println();
        return null;
    }

    /**
     * Writing a log entry to an SSH session may log entries for the SSH server, which would be displayed, and so on:
     * like the log:tail command of the Apache Karaf distribution, which sets the SSH server logger to ERROR while it
     * runs, don't display the other SSH server entries.
     */
    static boolean isSshdEntry(LogEntry entry) {
        String name = entry.getLoggerName();
        return name != null && name.startsWith(SSHD_LOGGER) && !LogLevel.ERROR.implies(PatternLayout.level(entry));
    }

    @FunctionalInterface
    interface EntryFilter {
        boolean accept(LogEntry entry);
    }

    /**
     * Queues the new log entries, so that they are written by the command thread: a slow console doesn't block the
     * threads logging, and the entries which can't be queued are counted as dropped.
     */
    static final class Tail implements LogListener {

        private final EntryFilter filter;
        private final BlockingQueue<LogEntry> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        private final AtomicLong dropped = new AtomicLong();
        private final Set<LogEntry> displayed = Collections.newSetFromMap(new IdentityHashMap<>());

        Tail(EntryFilter filter) {
            this.filter = filter;
        }

        @Override
        public void logged(LogEntry entry) {
            if (filter.accept(entry) && !queue.offer(entry)) {
                dropped.incrementAndGet();
            }
        }

        /**
         * @param log   the log entries read once the listener is added, most recent first.
         * @param count the number of entries to display first.
         * @return the entries to display first, oldest first.
         */
        List<LogEntry> start(List<LogEntry> log, int count) {
            // the entries logged after the listener is added but before the log is read are queued too
            displayed.addAll(log);
            return lastEntries(log, count);
        }

        LogEntry next(long timeout, TimeUnit unit) throws InterruptedException {
            while (true) {
                LogEntry entry = queue.poll(timeout, unit);
                if (entry == null || displayed.isEmpty() || !displayed.remove(entry)) {
                    return entry;
                }
            }
        }

        long dropped() {
            return dropped.getAndSet(0);
        }
    }

    /**
     * Adds the listener to the log reader service, and stops the tail when the service is unregistered.
     */
    private static final class LogReaderTracker extends ServiceTracker<LogReaderService, LogReaderService> {

        private final LogListener listener;
        private LogReaderService active;
        private volatile boolean stopped;

        LogReaderTracker(BundleContext context, LogListener listener) {
            super(context, LogReaderService.class, null);
            this.listener = listener;
        }

        @Override
        public LogReaderService addingService(ServiceReference<LogReaderService> reference) {
            LogReaderService service = super.addingService(reference);
            if (service != null) {
                synchronized (this) {
                    if (active == null) {
                        active = service;
                        service.addLogListener(listener);
                    }
                }
            }
            return service;
        }

        @Override
        public void removedService(ServiceReference<LogReaderService> reference, LogReaderService service) {
            synchronized (this) {
                if (service == active) {
                    service.removeLogListener(listener);
                    active = null;
                    stopped = true;
                }
            }
            super.removedService(reference, service);
        }

        boolean isStopped() {
            return stopped;
        }
    }

}
