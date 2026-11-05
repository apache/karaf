/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.services.logging;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.osgi.service.log.LoggerFactory;

public class Activator implements BundleActivator, ManagedService {

    public static final String CONFIGURATION_PID = "org.apache.karaf.logging";

    private final JulConfig julConfig = new JulConfig();
    private LogReaderServiceImpl logReaderService;
    private ServiceRegistration<?> logServiceReg;
    private ServiceRegistration<?> logReaderServiceReg;
    private ServiceRegistration<?> managedServiceReg;
    private FrameworkListener frameworkListener;
    private BundleListener bundleListener;

    @Override
    public void start(BundleContext context) throws Exception {
        logReaderService = new LogReaderServiceImpl();

        // Initialize JUL configuration
        julConfig.init(logReaderService);

        // Register LogService and LoggerFactory as ServiceFactory so each bundle gets its own context
        ServiceFactory<LogService> logServiceFactory = new ServiceFactory<LogService>() {
            @Override
            public LogService getService(Bundle bundle, ServiceRegistration<LogService> registration) {
                return new LogServiceImpl(bundle);
            }

            @Override
            public void ungetService(Bundle bundle, ServiceRegistration<LogService> registration, LogService service) {
            }
        };

        Hashtable<String, Object> props = new Hashtable<>();
        props.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        props.put(Constants.SERVICE_DESCRIPTION, "Apache Karaf Simple Logging Service");

        logServiceReg = context.registerService(
                new String[]{LogService.class.getName(), LoggerFactory.class.getName()},
                logServiceFactory,
                props
        );

        logReaderServiceReg = context.registerService(
                LogReaderService.class.getName(),
                logReaderService,
                props
        );

        Hashtable<String, Object> cmProps = new Hashtable<>();
        cmProps.put(Constants.SERVICE_PID, CONFIGURATION_PID);
        managedServiceReg = context.registerService(
                ManagedService.class.getName(),
                this,
                cmProps
        );

        // Track framework events
        frameworkListener = event -> {
            LogService logService = new LogServiceImpl(event.getBundle());
            switch (event.getType()) {
                case FrameworkEvent.ERROR:
                    logService.log(LogService.LOG_ERROR, "Framework error", event.getThrowable());
                    break;
                case FrameworkEvent.WARNING:
                    logService.log(LogService.LOG_WARNING, "Framework warning", event.getThrowable());
                    break;
                case FrameworkEvent.INFO:
                    logService.log(LogService.LOG_INFO, "Framework info: " + event);
                    break;
                default:
                    logService.log(LogService.LOG_DEBUG, "Framework event: " + event.getType());
                    break;
            }
        };
        context.addFrameworkListener(frameworkListener);

        // Track bundle lifecycle
        bundleListener = event -> {
            LogService logService = new LogServiceImpl(event.getBundle());
            if (event.getType() == BundleEvent.STARTED) {
                logService.log(LogService.LOG_INFO, "Bundle started: " + event.getBundle().getSymbolicName() + " [" + event.getBundle().getBundleId() + "]");
            } else if (event.getType() == BundleEvent.STOPPED) {
                logService.log(LogService.LOG_INFO, "Bundle stopped: " + event.getBundle().getSymbolicName() + " [" + event.getBundle().getBundleId() + "]");
            }
        };
        context.addBundleListener(bundleListener);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (bundleListener != null) {
            context.removeBundleListener(bundleListener);
            bundleListener = null;
        }
        if (frameworkListener != null) {
            context.removeFrameworkListener(frameworkListener);
            frameworkListener = null;
        }
        if (managedServiceReg != null) {
            managedServiceReg.unregister();
            managedServiceReg = null;
        }
        if (logReaderServiceReg != null) {
            logReaderServiceReg.unregister();
            logReaderServiceReg = null;
        }
        if (logServiceReg != null) {
            logServiceReg.unregister();
            logServiceReg = null;
        }
        julConfig.close();
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        julConfig.update(properties);
    }
}
