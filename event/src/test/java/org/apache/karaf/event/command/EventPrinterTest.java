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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.apache.karaf.event.command.EventPrinter;
import org.junit.Test;
import org.osgi.service.event.Event;

public class EventPrinterTest {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @Test
    public void testPrint() throws UnsupportedEncodingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);
        new EventPrinter(out, false).accept(event());
        String result = baos.toString("utf-8");
        assertThat(result, equalTo("2016-01-01 12:00:00 - myTopic\n"));
    }
    
    @Test
    public void testPrintVerbose() throws UnsupportedEncodingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);
        new EventPrinter(out, true).accept(event());
        String result = baos.toString("utf-8");
        assertThat(result, equalTo("2016-01-01 12:00:00 - myTopic\n" 
            + "a: b\n"
            + "c: [d, e]\n\n"));
    }

    private Event event() {
        HashMap<String, Object> props = new HashMap<>();
        props.put("a", "b");
        props.put("c", new String[]{"d", "e"});
        Date date;
        try {
            date = df.parse("2016-01-01 12:00:00");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        props.put("timestamp",  date.getTime());
        return new Event("myTopic", props);
    }
}
