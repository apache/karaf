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
package org.apache.felix.karaf.shell.log;

import org.apache.felix.karaf.shell.log.layout.PatternConverter;
import org.apache.felix.karaf.shell.log.layout.PatternParser;
import org.apache.felix.karaf.shell.console.OsgiCommandSupport;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.gogo.commands.Command;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;

/**
 * Displays the last log entries
 */
@Command(scope = "log", name = "display", description = "Display log entries.")
public class DisplayLog extends OsgiCommandSupport {

    @Option(name = "-n", description="Number of entries to display")
    protected int entries;

    @Option(name = "-p", description="Output formatting pattern")
    protected String overridenPattern;

    protected String pattern;

    protected LruList events;

    public LruList getEvents() {
        return events;
    }

    public void setEvents(LruList events) {
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
            System.out.print(sb.toString());
            if (event.getThrowableStrRep() != null) {
                for (String r : event.getThrowableStrRep()) {
                    System.out.println(r);
                }
            }
        }
        System.out.println();
        
        return null;
    }

}
