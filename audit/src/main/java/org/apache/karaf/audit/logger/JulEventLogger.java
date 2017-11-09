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
package org.apache.karaf.audit.logger;

import org.apache.karaf.audit.Event;
import org.apache.karaf.audit.EventLayout;
import org.apache.karaf.audit.EventLogger;

import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JulEventLogger implements EventLogger {

    private final String logger;
    private final Level level;
    private final EventLayout layout;

    public JulEventLogger(String logger, String level, EventLayout layout) {
        this.logger = logger;
        this.level = Level.parse(level.toUpperCase(Locale.ENGLISH));
        this.layout = layout;
    }

    @Override
    public void write(Event event) throws IOException {
        getLogger(event.type() + "." + event.subtype())
                .log(getLevel(event), layout.format(event).toString());
    }

    protected Level getLevel(Event event) {
        return level;
    }

    protected Logger getLogger(String t) {
        return Logger.getLogger(this.logger + "." + t);
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }
}
