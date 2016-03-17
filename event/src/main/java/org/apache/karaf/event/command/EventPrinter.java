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
package org.apache.karaf.event.command;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.function.Consumer;

import org.osgi.service.event.Event;

public class EventPrinter implements Consumer<Event>{
    private PrintStream out;
    private boolean verbose;

    public EventPrinter(PrintStream out, boolean verbose) {
        this.out = out;
        this.verbose = verbose;
    }
    

    @Override
    public void accept(Event event) {
        out.println(getTimeStamp(event) + " - " + event.getTopic());
        if (verbose) {
            for (String key : event.getPropertyNames()) {
                if (!key.equals("event.topics") && !key.equals("timestamp")) {
                    out.println(key + ": " + getPrintValue(event, key));
                }
            }
            out.println();
            out.flush();
        }
    }

    private String getTimeStamp(Event event) {
        Long ts = (Long)event.getProperty("timestamp");
        if (ts == null) {
            return "0000-00-00 00:00:00";
        }
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return df.format(new Date(ts));
    }

    private Object getPrintValue(Event event, String key) {
        Object value = event.getProperty(key);
        if (value.getClass().isArray()) {
            return Arrays.toString((Object[])value);
        }
        return value.toString();
    }

}
