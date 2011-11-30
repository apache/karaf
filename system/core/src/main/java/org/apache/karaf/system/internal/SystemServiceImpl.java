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
package org.apache.karaf.system.internal;

import org.apache.karaf.system.SystemService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Implementation of the system service.
 */
public class SystemServiceImpl implements SystemService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemServiceImpl.class);

    private BundleContext bundleContext;

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    public void shutdown() throws Exception {
        shutdown(null);
    }

    public void shutdown(String time) throws Exception {
        long sleep = 0;
        if (time != null) {
            if (!time.equals("now")) {
                if (time.startsWith("+")) {
                    // delay in number of minutes provided
                    time = time.substring(1);
                    try {
                        sleep = Long.parseLong(time) * 60 * 1000;
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Time " + time + " is not valid");
                    }
                } else {
                    // try to parse the date in hh:mm
                    String[] strings = time.split(":");
                    if (strings.length != 2) {
                        throw new IllegalArgumentException("Time " + time + " is not valid");
                    }
                    GregorianCalendar currentDate = new GregorianCalendar();
                    GregorianCalendar shutdownDate = new GregorianCalendar(currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE), Integer.parseInt(strings[0]), Integer.parseInt(strings[1]));
                    if (shutdownDate.before(currentDate)) {
                        shutdownDate.set(Calendar.DATE, shutdownDate.get(Calendar.DATE) + 1);
                    }
                    sleep = shutdownDate.getTimeInMillis() - currentDate.getTimeInMillis();
                }
            }
        }
        shutdown(sleep);
    }

    private void shutdown(final long sleep) {
        new Thread() {
            public void run() {
                try {
                    if (sleep > 0) {
                        LOGGER.info("Shutdown in " + sleep / 1000 / 60 + " minute(s)");
                    }
                    Thread.sleep(sleep);
                    Bundle bundle = getBundleContext().getBundle(0);
                    bundle.stop();
                } catch (Exception e) {
                    LOGGER.error("Error when shutting down", e);
                }
            }
        }.start();
    }

    public void setStartLevel(int startLevel) throws Exception {
        // get start level service
        ServiceReference ref = bundleContext.getServiceReference(org.osgi.service.startlevel.StartLevel.class.getName());
        if (ref == null) {
            throw new IllegalStateException("StartLevel service is unavailable");
        }
        try {
            org.osgi.service.startlevel.StartLevel startLevelService = (org.osgi.service.startlevel.StartLevel) bundleContext.getService(ref);
            if (startLevelService == null) {
                throw new IllegalStateException("StartLevel service is unavailable");
            }
            startLevelService.setStartLevel(startLevel);
        } finally {
            bundleContext.ungetService(ref);
        }
    }

    public int getStartLevel() throws Exception {
        // get start level service
        ServiceReference ref = bundleContext.getServiceReference(org.osgi.service.startlevel.StartLevel.class.getName());
        if (ref == null) {
            throw new IllegalStateException("StartLevel service is unavailable");
        }
        try {
            org.osgi.service.startlevel.StartLevel startLevelService = (org.osgi.service.startlevel.StartLevel) bundleContext.getService(ref);
            if (startLevelService == null) {
                throw new IllegalStateException("StartLevel service is unavailable");
            }
            return startLevelService.getStartLevel();
        } finally {
            bundleContext.ungetService(ref);
        }
    }

}
