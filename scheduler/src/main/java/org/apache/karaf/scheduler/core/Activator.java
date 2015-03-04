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

import org.apache.karaf.scheduler.Scheduler;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.ThreadPool;

@Services(provides = @ProvideService(Scheduler.class))
public class Activator extends BaseActivator {

    private ThreadPool threadPool;
    private QuartzScheduler scheduler;
    private WhiteboardHandler whiteboardHandler;

    @Override
    protected void doStart() throws Exception {
        threadPool = new SimpleThreadPool(4, Thread.NORM_PRIORITY);
        scheduler = new QuartzScheduler(threadPool);
        whiteboardHandler = new WhiteboardHandler(bundleContext, scheduler);
        register(Scheduler.class, scheduler);
    }

    @Override
    protected void doStop() {
        if (whiteboardHandler != null) {
            whiteboardHandler.deactivate();
            whiteboardHandler = null;
        }
        if (scheduler != null) {
            scheduler.deactivate();
            scheduler = null;
        }
        super.doStop();
    }

}
