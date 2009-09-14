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
package org.apache.felix.http.whiteboard.internal.util;

import org.osgi.util.tracker.ServiceTracker;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public final class SystemLogger
{
    private final static SystemLogger INSTANCE =
        new SystemLogger();
    
    private ServiceTracker tracker;

    private SystemLogger()
    {
    }

    public void open(BundleContext context)
    {
        this.tracker = new ServiceTracker(context, LogService.class.getName(), null);
        this.tracker.open();
    }

    public void close()
    {
        this.tracker.close();
    }

    public void debug(String message)
    {
        log(LogService.LOG_DEBUG, message, null);
    }

    public void info(String message)
    {
        log(LogService.LOG_INFO, message, null);
    }

    public void warning(String message, Throwable cause)
    {
        log(LogService.LOG_WARNING, message, cause);
    }

    public void error(String message, Throwable cause)
    {
        log(LogService.LOG_ERROR, message, cause);
    }

    private void log(int level, String message, Throwable cause)
    {
        LogService log = (LogService)this.tracker.getService();
        if (log != null) {
            log.log(level, message, cause);
        } else {
            System.out.println(message);
            if (cause != null) {
                cause.printStackTrace(System.out);
            }
        }
    }

    public static SystemLogger get()
    {
        return INSTANCE;
    }
}