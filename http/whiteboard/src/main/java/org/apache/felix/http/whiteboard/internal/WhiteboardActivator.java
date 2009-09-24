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
package org.apache.felix.http.whiteboard.internal;

import org.osgi.util.tracker.ServiceTracker;
import org.apache.felix.http.whiteboard.internal.tracker.FilterTracker;
import org.apache.felix.http.whiteboard.internal.tracker.HttpContextTracker;
import org.apache.felix.http.whiteboard.internal.tracker.ServletTracker;
import org.apache.felix.http.whiteboard.internal.tracker.HttpServiceTracker;
import org.apache.felix.http.whiteboard.internal.manager.ExtenderManagerImpl;
import org.apache.felix.http.whiteboard.internal.manager.ExtenderManager;
import org.apache.felix.http.base.internal.AbstractActivator;
import org.apache.felix.http.base.internal.logger.SystemLogger;

import java.util.ArrayList;

public final class WhiteboardActivator
    extends AbstractActivator
{
    private final ArrayList<ServiceTracker> trackers;
    private ExtenderManager manager;

    public WhiteboardActivator()
    {
        this.trackers = new ArrayList<ServiceTracker>();
    }

    protected void doStart()
        throws Exception
    {
        this.manager = new ExtenderManagerImpl();
        addTracker(new HttpContextTracker(getBundleContext(), this.manager));
        addTracker(new FilterTracker(getBundleContext(), this.manager));
        addTracker(new ServletTracker(getBundleContext(), this.manager));
        addTracker(new HttpServiceTracker(getBundleContext(), this.manager));
        SystemLogger.info("Http service whiteboard started");
    }

    private void addTracker(ServiceTracker tracker)
    {
        this.trackers.add(tracker);
        tracker.open();
    }

    protected void doStop()
        throws Exception
    {
        for (ServiceTracker tracker : this.trackers) {
            tracker.close();
        }

        this.trackers.clear();
        this.manager.unregisterAll();
    }
}
