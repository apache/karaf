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
package org.apache.karaf.shell.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Command(scope = "shell", name = "date", description = "Display the current time in the given FORMAT")
public class DateAction extends AbstractAction {

    @Option(name = "-d", aliases = { "--date" }, description = "Display time described, not now", multiValued = false, required = false)
    private String date;

    @Argument(index = 0, name = "format", description = "Output format", multiValued = false, required = false)
    private String format;

    protected Object doExecute() throws Exception {
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
            System.out.println(d);
        } else {
            // transform Unix format to Java SimpleDateFormat (if required)
            format = format.replaceAll("%a","EEE");
            format = format.replaceAll("%A", "EEEEEEE");
            format = format.replaceAll("%b", "MMM");
            format = format.replaceAll("%B", "MMMMMMM");
            format = format.replaceAll("%c", "MMM EEE d HH:mm:ss yyyy");
            format = format.replaceAll("%C","yy");
            format = format.replaceAll("%d", "dd");
            format = format.replaceAll("%D", "MM/dd/yy");
            format = format.replaceAll("%e", "dd");
            format = format.replaceAll("%F", "yyyy-MM-dd");
            format = format.replaceAll("%g", "YY");
            format = format.replaceAll("%G", "YYYY");
            format = format.replaceAll("%h", "MMM");
            format = format.replaceAll("%H", "HH");
            format = format.replaceAll("%I", "hh");
            format = format.replaceAll("%j", "DDD");
            format = format.replaceAll("%k", "HH");
            format = format.replaceAll("%l", "hh");
            format = format.replaceAll("%m", "MM");
            format = format.replaceAll("%M", "mm");
            format = format.replaceAll("%n", "\n");
            format = format.replaceAll("%N", "S");
            format = format.replaceAll("%p", "aa");
            format = format.replaceAll("%P", "aa");
            format = format.replaceAll("%r", "hh:mm:ss aa");
            format = format.replaceAll("%R", "HH:mm");
            format = format.replaceAll("%s", "S");
            format = format.replaceAll("%S", "ss");
            format = format.replaceAll("%t", "\t");
            format = format.replaceAll("%T", "HH:mm:ss");
            format = format.replaceAll("%u", "u");
            format = format.replaceAll("%U", "w");
            format = format.replaceAll("%V", "W");
            format = format.replaceAll("%w", "u");
            format = format.replaceAll("%W", "w");
            format = format.replaceAll("%x", "MM/dd/yy");
            format = format.replaceAll("%X", "HH:mm:ss");
            format = format.replaceAll("%y", "yy");
            format = format.replaceAll("%Y", "yyyy");
            format = format.replaceAll("%z", "X");
            format = format.replaceAll("%:z", "XXX");
            format = format.replaceAll("%::z", "XXXX");
            format = format.replaceAll("%:::z", "XXXX");
            format = format.replaceAll("%Z", "z");
            DateFormat df = new SimpleDateFormat(format);
            System.out.println(df.format(d));
        }
        return null;
    }

}
