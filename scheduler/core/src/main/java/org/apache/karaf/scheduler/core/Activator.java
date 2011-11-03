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

import org.osgi.framework.*;

import java.util.Dictionary;
import java.util.Properties;

public class Activator implements BundleActivator {

    private TaskScheduler scheduler = new TaskScheduler();
    private RunnableServiceListener listener;

    private static final String filter = "(&(objectclass=%s)(&(%s >= 0)(%s >= 0)))";

    @Override
    public void start(BundleContext context) throws Exception {
        listener = new RunnableServiceListener(context, scheduler);

        //register scheduler service
        context.registerService(scheduler.getClass().getName(), scheduler, (Dictionary) new Properties());

        //register service listener
        context.addServiceListener(listener, String.format(filter, Runnable.class.getName(), KarafTimerTask.ID_PROPERTY, KarafTimerTask.PERIOD_PROPERTY));

    }

    @Override
    public void stop(BundleContext context) throws Exception {

    }

}
