/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.main.internal;


import java.util.concurrent.TimeUnit;


public class Systemd {
    public static final String ENV_WATCHDOG_USEC = "WATCHDOG_USEC";
    public static final String ENV_MAIN_PID = "SYSTEMD_MAIN_PID";

    private final String mainPid;

    public Systemd() {
        this.mainPid = System.getProperty("karaf.systemd.main.pid", System.getenv(ENV_MAIN_PID));
    }

    public int notifyWatchdog() {
        int rc = -1;

        // WATCHDOG : tells the service manager to update the watchdog timestamp.
        //            This is the keep-alive ping that services need to issue in
        //            regular intervals if WatchdogSec= is enabled for it.
        // MAINPID  : the main process ID (PID) of the service, in case the service
        //            manager did not fork off the process itself. Example: "MAINPID=4711"
        //            This does not seem to work reliably so used only if system
        //            property karaf.systemd.main.pid or env variable SYSTEMD_MAIN_PID
        //            are set (system property takes the precedence)
        if(SystemdDaemon.INSTANCE != null) {
            rc = SystemdDaemon.INSTANCE.sd_notify(
                0,
                (mainPid == null) ? "WATCHDOG=1" : ("MAINPID=" + mainPid + "\nWATCHDOG=1"));
        }

        return rc;
    }

    public long getWatchdogTimeout(TimeUnit timeUnit) {
        String timeouts = System.getenv(ENV_WATCHDOG_USEC);
        if(timeouts != null) {
            long micros = Long.parseLong(timeouts);
            return timeUnit.convert(micros, TimeUnit.MICROSECONDS);
        }

        return -1;
    }
}
