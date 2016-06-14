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
package org.apache.karaf.shell.commands.impl;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Command(scope = "shell", name = "date", description = "Display the current time in the given FORMAT")
@Service
public class DateAction implements Action {

    @Option(name = "-d", aliases = { "--date" }, description = "Display time described, not now", multiValued = false, required = false)
    private String date;

    @Argument(index = 0, name = "format", description = "Output format", multiValued = false, required = false)
    private String format;

    @Override
    public Object execute() throws Exception {
        Date d;
        if (date == null || date.equalsIgnoreCase("now")) {
            d = new Date();
        } else {
            if (date.startsWith("@")) {
                d = new Date(Long.parseLong(date.substring(1)));
            } else {
                d = new Date(date);
            }
        }
        if (format == null) {
            format = "%+";
        }
        // transform Unix format to Java SimpleDateFormat (if required)
        StringBuilder sb = new StringBuilder();
        boolean quote = false;
        for (int i = 0; i < format.length(); i++) {
            char c = format.charAt(i);
            if (c == '%') {
                if (i + 1 < format.length()) {
                    if (quote) {
                        sb.append('\'');
                        quote = false;
                    }
                    c = format.charAt(++i);
                    switch (c) {
                        case '+':
                        case 'A': sb.append("MMM EEE d HH:mm:ss yyyy"); break;
                        case 'a': sb.append("EEE"); break;
                        case 'B': sb.append("MMMMMMM"); break;
                        case 'b': sb.append("MMM"); break;
                        case 'C': sb.append("yy"); break;
                        case 'c': sb.append("MMM EEE d HH:mm:ss yyyy"); break;
                        case 'D': sb.append("MM/dd/yy"); break;
                        case 'd': sb.append("dd"); break;
                        case 'e': sb.append("dd"); break;
                        case 'F': sb.append("yyyy-MM-dd"); break;
                        case 'G': sb.append("YYYY"); break;
                        case 'g': sb.append("YY"); break;
                        case 'H': sb.append("HH"); break;
                        case 'h': sb.append("MMM"); break;
                        case 'I': sb.append("hh"); break;
                        case 'j': sb.append("DDD"); break;
                        case 'k': sb.append("HH"); break;
                        case 'l': sb.append("hh"); break;
                        case 'M': sb.append("mm"); break;
                        case 'm': sb.append("MM"); break;
                        case 'N': sb.append("S"); break;
                        case 'n': sb.append("\n"); break;
                        case 'P': sb.append("aa"); break;
                        case 'p': sb.append("aa"); break;
                        case 'r': sb.append("hh:mm:ss aa"); break;
                        case 'R': sb.append("HH:mm"); break;
                        case 'S': sb.append("ss"); break;
                        case 's': sb.append("S"); break;
                        case 'T': sb.append("HH:mm:ss"); break;
                        case 't': sb.append("\t"); break;
                        case 'U': sb.append("w"); break;
                        case 'u': sb.append("u"); break;
                        case 'V': sb.append("W"); break;
                        case 'v': sb.append("dd-MMM-yyyy"); break;
                        case 'W': sb.append("w"); break;
                        case 'w': sb.append("u"); break;
                        case 'X': sb.append("HH:mm:ss"); break;
                        case 'x': sb.append("MM/dd/yy"); break;
                        case 'Y': sb.append("yyyy"); break;
                        case 'y': sb.append("yy"); break;
                        case 'Z': sb.append("z"); break;
                        case 'z': sb.append("X"); break;
                        case '%': sb.append("%"); break;
                    }
                } else {
                    if (!quote) {
                        sb.append('\'');
                    }
                    sb.append(c);
                    sb.append('\'');
                }
            } else {
                if ((c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z') && !quote) {
                    sb.append('\'');
                    quote = true;
                }
                sb.append(c);
            }
        }
        DateFormat df = new SimpleDateFormat(sb.toString());
        System.out.println(df.format(d));
        return null;
    }

}
