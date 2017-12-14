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
package org.apache.karaf.scheduler.command;

import org.apache.karaf.scheduler.ScheduleOptions;
import org.apache.karaf.scheduler.Scheduler;
import org.apache.karaf.scheduler.command.completers.JobNameCompleter;
import org.apache.karaf.shell.api.action.*;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import javax.xml.bind.DatatypeConverter;
import java.util.Date;

@Command(scope = "scheduler", name = "reschedule", description = "Update scheduling of an existing job")
@Service
public class Reschedule implements Action {

    @Argument(name = "name", required = true, description = "The job name")
    @Completion(JobNameCompleter.class)
    String name;

    @Option(name = "--concurrent", description = "Should jobs run concurrently or not (defaults to false)")
    boolean concurrent;

    @Option(name = "--cron", description = "The cron expression")
    String cron;

    @Option(name = "--at", description = "Absolute date in ISO format (ex: 2014-05-13T13:56:45)")
    String at;

    @Option(name = "--times", description = "Number of times this job should be executed")
    int times = -1;

    @Option(name = "--period", description = "Time during executions (in seconds)")
    long period;

    @Reference
    Scheduler scheduler;

    @Override
    public Object execute() throws Exception {
        if (cron != null && (at != null || times != -1 || period != 0)) {
            throw new IllegalArgumentException("Both cron expression and explicit execution time can not be specified");
        }
        ScheduleOptions options;
        if (cron != null) {
            options = scheduler.EXPR(cron);
        } else {
            Date date;
            if (at != null) {
                date = DatatypeConverter.parseDateTime(at).getTime();
            } else {
                date = new Date();
            }
            if (period > 0) {
                options = scheduler.AT(date, times, period);
            } else {
                options = scheduler.AT(date);
            }
        }
        options.name(name);
        if (concurrent) {
            options.canRunConcurrently(concurrent);
        }
        scheduler.reschedule(name, options);
        return null;
    }

}
