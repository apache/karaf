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
package org.apache.karaf.shell.shell;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

@Command(scope = "shell", name = "sleep", description = "Sleeps for a bit then wakes up.")
public class SleepAction extends AbstractAction {

    @Argument(index = 0, name = "duration", description = "The amount of time to sleep. The default time unit is millisecond, use -s option to use second instead.", required = true, multiValued = false)
    private long time = -1;
    
    @Option(name = "-s", aliases = { "--second" }, description = "Use a duration time in seconds instead of milliseconds.", required = false, multiValued = false)
    private boolean second = false;

    protected Object doExecute() throws Exception {
        if (second) {
            log.info("Sleeping for {} second(s)", time);
            time = time * 1000;
        } else {
            log.info("Sleeping for {} millisecond(s)", time);
        }

        try {
            Thread.sleep(time);
        }
        catch (InterruptedException ignore) {
            log.debug("Sleep was interrupted... :-(");
        }

        log.info("Awake now");
        return null;
    }
}
