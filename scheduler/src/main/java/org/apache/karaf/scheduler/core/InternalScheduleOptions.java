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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.apache.karaf.scheduler.ScheduleOptions;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

/**
 * Scheduler options provide an extensible way of defining how to schedule a job.
 * @since 2.3
 */
public class InternalScheduleOptions implements ScheduleOptions {

    public String name;

    public boolean canRunConcurrently = false;

    public Map<String, Serializable> configuration;

    public final String schedule;

    public final TriggerBuilder<? extends Trigger> trigger;

    public final IllegalArgumentException argumentException;

    public InternalScheduleOptions(Date date) {
        if ( date == null ) {
            this.trigger = null;
            this.argumentException = new IllegalArgumentException("Date can't be null");
        } else {
            this.trigger = TriggerBuilder.newTrigger().startAt(date);
            this.argumentException = null;
        }
        this.schedule = "at(" + formatDate(date) + ")";
    }

    public InternalScheduleOptions(Date date, int times, long period) {
        TriggerBuilder<? extends Trigger> trigger = null;
        IllegalArgumentException argumentException = null;
        try {
            if (date == null) {
                throw new IllegalArgumentException("Date can't be null");
            }
            if (times < 2 && times != -1) {
                throw new IllegalArgumentException("Times argument must be higher than 1 or -1");
            }
            if (period < 1) {
                throw new IllegalArgumentException("Period argument must be higher than 0");
            }
            final SimpleScheduleBuilder sb;
            if (times == -1) {
                sb = SimpleScheduleBuilder.simpleSchedule().repeatForever();
            } else {
                sb = SimpleScheduleBuilder.simpleSchedule().withRepeatCount(times - 1);
            }
            trigger = TriggerBuilder.newTrigger()
                        .startAt(date)
                        .withSchedule(sb.withIntervalInMilliseconds(period * 1000));
        } catch (IllegalArgumentException e) {
            argumentException = e;
        }
        this.trigger = trigger;
        this.argumentException = argumentException;
        this.schedule = "at(" + formatDate(date) + ", " + times + ", " + period + ")";
    }

    public InternalScheduleOptions(String expression) {
        TriggerBuilder<? extends Trigger> trigger = null;
        IllegalArgumentException argumentException = null;
        try {
            if ( expression == null ) {
                throw new IllegalArgumentException("Expression can't be null");
            }
            if ( !CronExpression.isValidExpression(expression) ) {
                throw new IllegalArgumentException("Expression is invalid : " + expression);
            }
            trigger = TriggerBuilder.newTrigger()
                            .withSchedule(CronScheduleBuilder.cronSchedule(expression));
        } catch (IllegalArgumentException e) {
            argumentException = e;
        }
        this.trigger = trigger;
        this.argumentException = argumentException;
        this.schedule = "cron(" + expression + ")";
    }

    /**
     * @see org.apache.karaf.scheduler.ScheduleOptions#config(java.util.Map)
     */
    public ScheduleOptions config(final  Map<String, Serializable> config) {
        this.configuration = config;
        return this;
    }

    /**
     * @see org.apache.karaf.scheduler.ScheduleOptions#name(java.lang.String)
     */
    public ScheduleOptions name(final String name) {
        this.name = name;
        return this;
    }

    /**
     * @see org.apache.karaf.scheduler.ScheduleOptions#canRunConcurrently(boolean)
     */
    public ScheduleOptions canRunConcurrently(final boolean flag) {
        this.canRunConcurrently = flag;
        return this;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public boolean canRunConcurrently() {
        return this.canRunConcurrently;
    }

    @Override
    public String schedule() {
        return schedule;
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "null";
        }
        Calendar c = GregorianCalendar.getInstance();
        c.setTime(date);
        return DatatypeConverter.printDateTime(c);
    }
}
