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
package org.apache.karaf.scheduler.core;

import java.io.Serializable;
import java.util.Map;

import org.apache.karaf.scheduler.JobContext;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;

/**
 * This component is responsible to launch a {@link org.apache.karaf.scheduler.Job}
 * or {@link Runnable} in a Quartz Scheduler.
 *
 */
public class QuartzJobExecutor implements Job {

    /**
     * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
     */
    public void execute(final JobExecutionContext context) throws JobExecutionException {

        final JobDataMap data = context.getJobDetail().getJobDataMap();
        final Object job = data.get(QuartzScheduler.DATA_MAP_OBJECT);
        final Logger logger = (Logger)data.get(QuartzScheduler.DATA_MAP_LOGGER);

        try {
            logger.debug("Executing job {} with name {}", job, data.get(QuartzScheduler.DATA_MAP_NAME));
            if (job instanceof org.apache.karaf.scheduler.Job) {
                final InternalScheduleOptions options = (InternalScheduleOptions) data.get(QuartzScheduler.DATA_MAP_OPTIONS);
                final String name = (String) data.get(QuartzScheduler.DATA_MAP_NAME);

                final JobContext jobCtx = new JobContextImpl(name, options.configuration);
                ((org.apache.karaf.scheduler.Job) job).execute(jobCtx);
            } else if (job instanceof Runnable) {
                ((Runnable) job).run();
            } else {
                logger.error("Scheduled job {} is neither a job nor a runnable.", job);
            }
        } catch (final Throwable t) {
            // there is nothing we can do here, so we just log
            logger.error("Exception during job execution of " + job + " : " + t.getMessage(), t);
        }
    }

    public static final class JobContextImpl implements JobContext {

        protected final Map<String, Serializable> configuration;
        protected final String name;

        public JobContextImpl(String name, Map<String, Serializable> config) {
            this.name = name;
            this.configuration = config;
        }

        /**
         * @see org.apache.karaf.scheduler.JobContext#getConfiguration()
         */
        public Map<String, Serializable> getConfiguration() {
            return this.configuration;
        }

        /**
         * @see org.apache.karaf.scheduler.JobContext#getName()
         */
        public String getName() {
            return this.name;
        }
    }
}
