/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.impl.console.osgi;

import java.io.Closeable;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.felix.gogo.runtime.threadio.ThreadIOImpl;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.karaf.shell.api.console.CommandLoggingFilter;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.shell.impl.action.command.ManagerImpl;
import org.apache.karaf.shell.impl.action.osgi.CommandExtender;
import org.apache.karaf.shell.impl.console.SessionFactoryImpl;
import org.apache.karaf.shell.impl.console.osgi.secured.SecuredSessionFactoryImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

    private static final String START_CONSOLE = "karaf.startLocalConsole";
    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private ThreadIOImpl threadIO;

    private SessionFactoryImpl sessionFactory;
    private ServiceRegistration<SessionFactory> sessionFactoryRegistration;
    private ServiceRegistration<CommandProcessor> commandProcessorRegistration;

    private CommandExtender actionExtender;

    private Closeable eventAdminListener;

    private LocalConsoleManager localConsoleManager;

    ServiceTracker<CommandLoggingFilter, CommandLoggingFilter> filterTracker;

    CommandTracker commandTracker;

    ConverterTracker converterTracker;

    ListenerTracker listenerTracker;

    @Override
    public void start(final BundleContext context) throws Exception {
        threadIO = new ThreadIOImpl();
        threadIO.start();

        sessionFactory = new SecuredSessionFactoryImpl(context, threadIO);
        sessionFactory.getCommandProcessor().addConverter(new Converters(context));
        sessionFactory.getCommandProcessor().addConstant(".context", context.getBundle(0).getBundleContext());

        final CopyOnWriteArraySet<CommandLoggingFilter> listeners = new CopyOnWriteArraySet<>();
        filterTracker = new ServiceTracker<>(
                context, CommandLoggingFilter.class, new ServiceTrackerCustomizer<CommandLoggingFilter, CommandLoggingFilter>() {
            @Override
            public CommandLoggingFilter addingService(ServiceReference<CommandLoggingFilter> reference) {
                CommandLoggingFilter service = context.getService(reference);
                listeners.add(service);
                return service;
            }

            @Override
            public void modifiedService(ServiceReference<CommandLoggingFilter> reference, CommandLoggingFilter service) {
            }

            @Override
            public void removedService(ServiceReference<CommandLoggingFilter> reference, CommandLoggingFilter service) {
                listeners.remove(service);
                context.ungetService(reference);
            }
        });
        filterTracker.open();
        LoggingCommandSessionListener loggingCommandSessionListener = new LoggingCommandSessionListener();
        loggingCommandSessionListener.setFilters(listeners);
        sessionFactory.getCommandProcessor().addListener(loggingCommandSessionListener);

        try {
            EventAdminListener listener = new EventAdminListener(context);
            sessionFactory.getCommandProcessor().addListener(listener);
            eventAdminListener = listener;
        } catch (NoClassDefFoundError error) {
            // Ignore the listener if EventAdmin package isn't present
        }

        sessionFactory.register(new ManagerImpl(sessionFactory, sessionFactory));

        sessionFactoryRegistration = context.registerService(SessionFactory.class, sessionFactory, null);
        commandProcessorRegistration = context.registerService(CommandProcessor.class, sessionFactory.getCommandProcessor(), null);

        actionExtender = new CommandExtender(sessionFactory);
        actionExtender.start(context);

        commandTracker = new CommandTracker(sessionFactory, context);
        commandTracker.open();

        converterTracker = new ConverterTracker(sessionFactory, context);
        converterTracker.open();

        listenerTracker = new ListenerTracker(sessionFactory, context);
        listenerTracker.open();

        if (Boolean.parseBoolean(context.getProperty(START_CONSOLE))) {
            localConsoleManager = new LocalConsoleManager(context, sessionFactory);
            localConsoleManager.start();
        } else {
            LOGGER.info("Not starting local console. To activate set " + START_CONSOLE + "=true");
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        filterTracker.close();
        commandProcessorRegistration.unregister();
        sessionFactoryRegistration.unregister();
        if (localConsoleManager != null) {
            localConsoleManager.stop();
        }
        sessionFactory.stop();
        actionExtender.stop(context);
        commandTracker.close();
        converterTracker.close();
        listenerTracker.close();
        threadIO.stop();
        if (eventAdminListener != null) {
            eventAdminListener.close();
        }
    }

}
