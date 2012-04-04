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
import org.apache.karaf.log.core.LogService;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;

/**
 * Displays the last log entries
 */
@Command(scope = "log", name = "display", description = "Displays log entries.")
public class DisplayLog extends OsgiCommandSupport {

    @Option(name = "-n", aliases = {}, description="Number of entries to display", required = false, multiValued = false)
    protected int entries;

    @Option(name = "-p", aliases = {}, description="Pattern for formatting the output", required = false, multiValued = false)
    protected String overridenPattern;

    @Option(name = "--no-color", description="Disable syntax coloring of log events", required = false, multiValued = false)
    protected boolean noColor;

    @Argument(index = 0, name = "logger", description = "The name of the logger. This can be ROOT, ALL, or the name of a logger specified in the org.ops4j.pax.logger.cfg file.", required = false, multiValued = false)
    String logger;

    protected final LogService logService;

    protected final LogEventFormatter formatter;
    
    public DisplayLog(LogService logService, LogEventFormatter formatter) {
        this.logService = logService;
        this.formatter = formatter;
    }

    protected Object doExecute() throws Exception {
        
        final PrintStream out = System.out;

        Iterable<PaxLoggingEvent> le = logService.getEvents(entries == 0 ? Integer.MAX_VALUE : entries);
        for (PaxLoggingEvent event : le) {
            printEvent(out, event);
        }
        out.println();
        return null;
    }
        
    protected boolean checkIfFromRequestedLog(PaxLoggingEvent event) {
    	return (event.getLoggerName().lastIndexOf(logger)>=0) ? true : false;
    }

    protected void printEvent(final PrintStream out, PaxLoggingEvent event) {
        if ((logger != null) && 
            (event != null)&&
            (checkIfFromRequestedLog(event))) {
                out.append(formatter.format(event, overridenPattern, noColor));
            }
            else if ((event != null)&&(logger == null)){
                out.append(formatter.format(event, overridenPattern, noColor));
        }
    }
}
