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

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class RunnableServiceListener implements ServiceListener {

    private final BundleContext bundleContext;
    private final TaskScheduler scheduler;

    /**
     * Constructor
     *
     * @param bundleContext
     * @param scheduler
     */
    public RunnableServiceListener(BundleContext bundleContext, TaskScheduler scheduler) {
        this.bundleContext = bundleContext;
        this.scheduler = scheduler;
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        switch (event.getType()) {
            case ServiceEvent.REGISTERED:
                scheduleRunnableService(event);
                break;
            case ServiceEvent.UNREGISTERING: {
                unscheduleRunnableService(event);
            }
            break;
            default:
                break;
        }
    }

    /**
     * Schedules the execution of the Runnable Service of the {@link ServiceEvent}.
     *
     * @param event
     */
    protected void scheduleRunnableService(ServiceEvent event) {
        ServiceReference reference = event.getServiceReference();
        Runnable service = (Runnable) bundleContext.getService(reference);
        String id = (String) reference.getProperty(KarafTimerTask.ID_PROPERTY);
        String periodValue = (String) reference.getProperty(KarafTimerTask.PERIOD_PROPERTY);

        if (periodValue != null) {
            Long period = Long.parseLong(periodValue);
            KarafTimerTask task = new KarafTimerTask(id, service, period);
            scheduler.schedule(task);
        }
    }

    /**
     * Unschedules the execution of the Runnable Service of the {@link ServiceEvent}.
     *
     * @param event
     */
    protected void unscheduleRunnableService(ServiceEvent event) {
        ServiceReference reference = event.getServiceReference();
        String id = (String) reference.getProperty(KarafTimerTask.ID_PROPERTY);
        scheduler.unschedule(id);
    }

}
