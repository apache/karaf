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
package org.apache.karaf.log.core.internal.osgi;

import java.util.Hashtable;

import org.apache.karaf.log.core.LogEventFormatter;
import org.apache.karaf.log.core.LogService;
import org.apache.karaf.log.core.internal.LogEventFormatterImpl;
import org.apache.karaf.log.core.internal.LogMBeanImpl;
import org.apache.karaf.log.core.internal.LogServiceImpl;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.Managed;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.ops4j.pax.logging.spi.PaxAppender;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogLevel;

@Services(
        requires = @RequireService(ConfigurationAdmin.class),
        provides = @ProvideService(LogService.class)
)
@Managed("org.apache.karaf.log")
public class Activator extends BaseActivator implements ManagedService {

    protected void doStart() throws Exception {
        ConfigurationAdmin configurationAdmin = getTrackedService(ConfigurationAdmin.class);
        if (configurationAdmin == null) {
            return;
        }

        int size = getInt("size", 500);
        String pattern = getString("pattern", "%d{ABSOLUTE} | %-5.5p | %-16.16t | %-32.32c{1} | %-32.32C %4L | %m%n");
        String errorColor = getString("errorColor", "31");
        String warnColor = getString("warnColor", "35");
        String infoColor = getString("infoColor", "36");
        String debugColor = getString("debugColor", "39");
        String traceColor = getString("traceColor", "39");

        LogEventFormatterImpl formatter = new LogEventFormatterImpl();
        formatter.setPattern(pattern);
        formatter.setColor(LogLevel.ERROR, errorColor);
        formatter.setColor(LogLevel.WARN, warnColor);
        formatter.setColor(LogLevel.INFO, infoColor);
        formatter.setColor(LogLevel.DEBUG, debugColor);
        formatter.setColor(LogLevel.TRACE, traceColor);
        formatter.setColor(LogLevel.AUDIT, traceColor);
        register(LogEventFormatter.class, formatter);

        LogServiceImpl logService = new LogServiceImpl(configurationAdmin, size);
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("org.ops4j.pax.logging.appender.name", "VmLogAppender");
        register(PaxAppender.class, logService, props);
        register(LogService.class, logService);

        LogMBeanImpl securityMBean = new LogMBeanImpl(logService);
        registerMBean(securityMBean, "type=log");
    }

}
