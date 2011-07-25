/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.karaf.scheduler.core;

import java.util.TimerTask;

public class KarafTimerTask<R extends Runnable> extends TimerTask {

    public static final String ID_PROPERTY = "org.apache.karaf.scheduler.task.id";
    public static final String PERIOD_PROPERTY = "org.apache.karaf.scheduler.task.period";

    protected String id;
    protected R task;
    protected Long schedulePeriod = 0L;

    /**
     * Constructor
     * @param id  the id of the task. Used for reference.
     * @param task the task to be scheduled
     */
    public KarafTimerTask(String id, R task) {
        this.id = id;
        this.task = task;
    }

    /**
     * Constructor
     * @param id  the id of the task. Used for reference.
     * @param task the task to be scheduled
     * @param schedulePeriod the schedule period.
     */
    public KarafTimerTask(String id, R task, Long schedulePeriod) {
        this.id = id;
        this.task = task;
        this.schedulePeriod = schedulePeriod;
    }

    @Override
    public void run() {
        task.run();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public R getTask() {
        return task;
    }

    public void setTask(R task) {
        this.task = task;
    }

    public Long getSchedulePeriod() {
        return schedulePeriod;
    }

    public void setSchedulePeriod(Long schedulePeriod) {
        this.schedulePeriod = schedulePeriod;
    }

}
