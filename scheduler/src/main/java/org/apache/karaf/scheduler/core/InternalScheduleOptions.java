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

    private static final long serialVersionUID = -2632689849349264449L;

    public String name;

    public boolean canRunConcurrently = false;

    public Map<String, Serializable> configuration;

    public String schedule;

    private Date date;
    private int times;
    private long period;
    private String expression;

    public InternalScheduleOptions(Date date) {
        this.date = date;
        this.times = 0;
        this.period = 0;
        this.schedule =  null;
        this.expression = null;
    }

    public InternalScheduleOptions(Date date, int times, long period) {
        this.date = date;
        this.times = times;
        this.period = period;
        this.schedule =  null;
        this.expression = null;
    }

    public InternalScheduleOptions(String expression) {
        this.date = null;
        this.times = 0;
        this.period = 0;
        this.schedule =  null;
        this.expression = expression;
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

    public TriggerBuilder<? extends Trigger> compile() {
        TriggerBuilder<? extends Trigger> result = null;
        try {
            if (null == expression) {
                if (date == null) {
                    throw new IllegalArgumentException("Date can't be null");
                } else {
                    boolean dateOnly = false;
                    if (times < 2 && times != -1) {
                        dateOnly = true;
                    }
                    if (period < 1) {
                        dateOnly = true;
                    }
                    if (dateOnly) {
                        result = TriggerBuilder.newTrigger().startAt(date);
                        this.schedule = "at(" + formatDate(date) + ")";
                    } else {
                        final SimpleScheduleBuilder sb;
                        if (times == -1) {
                            sb = SimpleScheduleBuilder.simpleSchedule().repeatForever();
                        } else {
                            sb = SimpleScheduleBuilder.simpleSchedule().withRepeatCount(times - 1);
                        }
                        result = TriggerBuilder.newTrigger()
                                .startAt(date)
                                .withSchedule(sb.withIntervalInMilliseconds(period * 1000));
                        this.schedule = "at(" + formatDate(date) + ", " + times + ", " + period + ")";
                    }
                }
            } else {
                TriggerBuilder<? extends Trigger> trigger = null;
                if (!CronExpression.isValidExpression(expression)) {
                    throw new IllegalArgumentException("Expression is invalid : " + expression);
                }
                trigger = TriggerBuilder.newTrigger()
                        .withSchedule(CronScheduleBuilder.cronSchedule(expression));
                result = trigger;
                this.schedule = "cron(" + expression + ")";
            }
        } catch (IllegalArgumentException e) {
            throw e;
        }

        return result;
    }
}
