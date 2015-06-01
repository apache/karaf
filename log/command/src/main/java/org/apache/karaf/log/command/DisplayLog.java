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

import org.apache.karaf.log.core.LogEventFormatter;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;

/**
 * Displays the last log entries
 */
@Command(scope = "log", name = "display", description = "Displays log entries.")
public class DisplayLog extends LogCommandSupport {

    public final static int ERROR_INT = 3;
    public final static int WARN_INT  = 4;
    public final static int INFO_INT  = 6;
    public final static int DEBUG_INT = 7;

    @Option(name = "-n", aliases = {}, description="Number of entries to display", required = false, multiValued = false)
    protected int entries;

    @Option(name = "-p", aliases = {}, description="Pattern for formatting the output", required = false, multiValued = false)
    protected String overridenPattern;

    @Option(name = "--no-color", description="Disable syntax coloring of log events", required = false, multiValued = false)
    protected boolean noColor;

    @Option(name = "-l", aliases = { "--level" }, description = "The minimal log level to display", required = false, multiValued = false)
    String level;

    @Argument(index = 0, name = "logger", description = "The name of the logger. This can be ROOT, ALL, or the name of a logger specified in the org.ops4j.pax.logger.cfg file.", required = false, multiValued = false)
    String logger;

    protected LogEventFormatter formatter;
    
    public void setFormatter(LogEventFormatter formatter) {
        this.formatter = formatter;
    }

    protected Object doExecute() throws Exception {

        int minLevel = Integer.MAX_VALUE;
        if (level != null) {
            String lvl = level.toLowerCase();
            if ("debug".equals(lvl)) {
                minLevel = DEBUG_INT;
            } else if ("info".equals(lvl)) {
                minLevel = INFO_INT;
            } else if ("warn".equals(lvl)) {
                minLevel = WARN_INT;
            } else if ("error".equals(lvl)) {
                minLevel = ERROR_INT;
            }
        }

        final PrintStream out = System.out;

        Iterable<PaxLoggingEvent> le = logService.getEvents(entries == 0 ? Integer.MAX_VALUE : entries);
        for (PaxLoggingEvent event : le) {
            int sl = event.getLevel().getSyslogEquivalent();
            if (sl <= minLevel) {
                printEvent(out, event);
            }
        }
        out.println();
        return null;
    }
        
    protected boolean checkIfFromRequestedLog(PaxLoggingEvent event) {
    	return event.getLoggerName().contains(logger);
    }

    protected void printEvent(final PrintStream out, PaxLoggingEvent event) {
        try {
            if ((logger != null) &&
                    (event != null) &&
                    (checkIfFromRequestedLog(event))) {
                out.append(formatter.format(event, overridenPattern, noColor));
            } else if ((event != null) && (logger == null)) {
                out.append(formatter.format(event, overridenPattern, noColor));
            }
        } catch (NoClassDefFoundError e) {
            // KARAF-3350: Ignore NoClassDefFoundError exceptions
            // Those exceptions may happen if the underlying pax-logging service
            // bundle has been refreshed somehow.
        }
    }
}
