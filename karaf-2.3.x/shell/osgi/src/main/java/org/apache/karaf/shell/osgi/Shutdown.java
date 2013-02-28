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
package org.apache.karaf.shell.osgi;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.apache.felix.gogo.commands.Command;
import org.osgi.framework.Bundle;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Command to shut down Karaf
 */
@Command(scope = "osgi", name = "shutdown", description = "Shutdown the framework down.")
public class Shutdown extends OsgiCommandSupport {

    @Option(name = "-f", aliases = "--force", description = "Force the shutdown without confirmation message.", required = false, multiValued = false)
    boolean force = false;

    @Argument(name = "time", index = 0, description = "Shutdown after a specified delay. The time argument can have different" +
            " formats. First, it can be an abolute time in the format hh:mm, in which hh is the hour (1 or 2 digits) and mm" +
            " is the minute of the hour (in two digits). Second, it can be in the format +m, in which m is the number of minutes" +
            " to wait. The word now is an alias for +0.", required = false, multiValued = false)
    String time;

    protected Object doExecute() throws Exception {

        long sleep = 0;
        if (time != null) {
            if (!time.equals("now")) {
                if (time.startsWith("+")) {
                    // delay in number of minutes provided
                    time = time.substring(1);
                    try {
                        sleep = Long.parseLong(time) * 60 * 1000;
                    } catch (Exception e) {
                        System.err.println("Invalid time argument.");
                        return null;
                    }
                } else {
                    // try to parse the date in hh:mm
                    String[] strings = time.split(":");
                    if (strings.length != 2) {
                        System.err.println("Invalid time argument.");
                        return null;
                    }
                    GregorianCalendar currentDate = new GregorianCalendar();
                    GregorianCalendar shutdownDate = new GregorianCalendar(currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE), Integer.parseInt(strings[0]), Integer.parseInt(strings[1]));
                    if (shutdownDate.before(currentDate)) {
                        shutdownDate.set(Calendar.DATE, shutdownDate.get(Calendar.DATE) + 1);
                    }
                    sleep = shutdownDate.getTimeInMillis() - currentDate.getTimeInMillis();
                }
            }
        }

        if (force) {
            this.shutdown(sleep);
            return null;
        }

        for (; ; ) {
            StringBuffer sb = new StringBuffer();
            String karafName = System.getProperty("karaf.name");
            System.err.println(String.format("Confirm: shutdown instance %s (yes/no): ", karafName));

            System.err.flush();
            for (; ; ) {
                int c = session.getKeyboard().read();
                if (c < 0) {
                    return null;
                }
                System.err.print((char) c);
                System.err.flush();
                if (c == '\r' || c == '\n') {
                    break;
                }
                sb.append((char) c);
            }
            String str = sb.toString();
            if (str.equals("yes")) {
                this.shutdown(sleep);
            }
            return null;
        }
    }

    private void shutdown(final long sleep) {
        new Thread() {
            public void run() {
                try {
                    if (sleep > 0) {
                        System.err.println("Shutdown in " + sleep/1000/60 + " minute(s)");
                    }
                    Thread.sleep(sleep);
                    getBundleContext().getBundle(0).stop();
                } catch (Exception e) {
                    log.error("Error when shutting down", e);
                }
            }
        }.start();
    }

}
