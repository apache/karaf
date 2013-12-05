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
package org.apache.karaf.system.commands;

import jline.console.ConsoleReader;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.system.SystemService;

/**
 * Command to shut down Karaf container.
 */
@Command(scope = "system", name = "shutdown", description = "Shutdown Karaf.")
public class Shutdown extends AbstractSystemAction {

    @Option(name = "-f", aliases = "--force", description = "Force the shutdown without confirmation message.", required = false, multiValued = false)
    boolean force = false;

    @Option(name = "-r", aliases = "--reboot", description = "Reboot the Karaf container.", required = false, multiValued = false)
    boolean reboot = false;

    @Option(name = "-h", aliases = "--halt", description = "Halt the Karaf container.", required = false, multiValued = false)
    boolean halt = false;

    @Option(name = "-c", aliases = {"--clean", "--clean-all", "-ca"}, description = "Force a clean restart by deleting the data directory")
    private boolean cleanAll;

    @Option(name = "-cc", aliases = {"--clean-cache", "-cc"}, description = "Force a clean restart by deleting the cache directory")
    private boolean cleanCache;


    @Argument(name = "time", index = 0, description = "Shutdown after a specified delay. The time argument can have different" +
            " formats. First, it can be an absolute time in the format hh:mm, in which hh is the hour (1 or 2 digits) and mm" +
            " is the minute of the hour (in two digits). Second, it can be in the format +m, in which m is the number of minutes" +
            " to wait. The word now is an alias for +0.", required = false, multiValued = false)
    String time;

    protected Object doExecute() throws Exception {

        if (force) {
            if (reboot) {
                systemService.reboot(time, determineSwipeType());
            } else {
                systemService.halt(time);
            }
            return null;
        }

        for (; ; ) {
            String karafName = System.getProperty("karaf.name");
            String msg;
            if (reboot) {
                msg = String.format("Confirm: reboot instance %s (yes/no): ", karafName);
            } else {
                msg = String.format("Confirm: halt instance %s (yes/no): ", karafName);
            }
            ConsoleReader reader = (ConsoleReader) session.get(".jline.reader");
            String str = reader.readLine(msg);
            if (str.equalsIgnoreCase("yes")) {
                if (reboot) {
                    systemService.reboot(time, determineSwipeType());
                } else {
                    systemService.halt(time);
                }
            }
            return null;
        }
    }

    private SystemService.Swipe determineSwipeType() {
        if (cleanAll) {
            return SystemService.Swipe.ALL;
        } else if (cleanCache) {
            return SystemService.Swipe.CACHE;
        }
        return SystemService.Swipe.NONE;
    }

}
