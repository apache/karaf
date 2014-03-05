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

import org.apache.felix.gogo.runtime.threadio.ThreadIOImpl;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.shell.impl.action.osgi.CommandExtender;
import org.apache.karaf.shell.impl.action.command.ManagerImpl;
import org.apache.karaf.shell.impl.console.SessionFactoryImpl;
import org.apache.karaf.shell.impl.console.TerminalFactory;
import org.apache.karaf.shell.impl.console.osgi.secured.SecuredSessionFactoryImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    private static final String START_CONSOLE = "karaf.startLocalConsole";

    private ThreadIOImpl threadIO;

    private SessionFactoryImpl sessionFactory;
    private ServiceRegistration sessionFactoryRegistration;

    private CommandExtender actionExtender;

    private TerminalFactory terminalFactory;
    private LocalConsoleManager localConsoleManager;

    @Override
    public void start(BundleContext context) throws Exception {
        threadIO = new ThreadIOImpl();
        threadIO.start();

        sessionFactory = new SecuredSessionFactoryImpl(context, threadIO);
        sessionFactory.getCommandProcessor().addConverter(new Converters(context));
        sessionFactory.getCommandProcessor().addConstant(".context", context.getBundle(0).getBundleContext());

        sessionFactory.register(new ManagerImpl(sessionFactory, sessionFactory));

        sessionFactoryRegistration = context.registerService(SessionFactory.class.getName(), sessionFactory, null);

        actionExtender = new CommandExtender(sessionFactory);
        actionExtender.start(context);

        if (Boolean.parseBoolean(context.getProperty(START_CONSOLE))) {
            terminalFactory = new TerminalFactory();
            localConsoleManager = new LocalConsoleManager(context, terminalFactory, sessionFactory);
            localConsoleManager.start();
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        sessionFactoryRegistration.unregister();
        sessionFactory.stop();
        localConsoleManager.stop();
        actionExtender.stop(context);
        threadIO.stop();
        terminalFactory.destroy();
    }
}
