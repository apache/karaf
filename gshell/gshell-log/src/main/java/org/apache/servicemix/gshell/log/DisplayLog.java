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
package org.apache.servicemix.gshell.log;

import org.apache.geronimo.gshell.clp.Option;
import org.apache.geronimo.gshell.command.annotation.CommandComponent;
import org.apache.geronimo.gshell.support.OsgiCommandSupport;
import org.apache.servicemix.gshell.log.layout.PatternConverter;
import org.apache.servicemix.gshell.log.layout.PatternParser;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;

/**
 * Displays the last log entries
 */
@CommandComponent(id = "log:display", description = "Display log entries")
public class DisplayLog extends OsgiCommandSupport {

    @Option(name = "-n", description="Number of entries to display")
    protected int entries;

    @Option(name = "-p", description="Output formatting pattern")
    protected String overridenPattern;

    protected String pattern;

    protected LruList<PaxLoggingEvent> events;

    @Override
    protected OsgiCommandSupport createCommand() throws Exception {
        DisplayLog command = new DisplayLog();
        command.setEvents(getEvents());
        command.setPattern(getPattern());
        return command;
    }

    public LruList<PaxLoggingEvent> getEvents() {
        return events;
    }

    public void setEvents(LruList<PaxLoggingEvent> events) {
        this.events = events;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    protected Object doExecute() throws Exception {
        PatternConverter cnv = new PatternParser(overridenPattern != null ? overridenPattern : pattern).parse();

        Iterable<PaxLoggingEvent> le = events.getElements(entries == 0 ? Integer.MAX_VALUE : entries);
        StringBuffer sb = new StringBuffer();
        for (PaxLoggingEvent event : le) {
            sb.setLength(0);
            for (PatternConverter pc = cnv; pc != null; pc = pc.next) {
                pc.format(sb, event);
            }
            io.out.print(sb.toString());
            if (event.getThrowableStrRep() != null) {
                for (String r : event.getThrowableStrRep()) {
                    io.out.println(r);
                }
            }
        }
        io.out.println();
        
        return SUCCESS;
    }

}
