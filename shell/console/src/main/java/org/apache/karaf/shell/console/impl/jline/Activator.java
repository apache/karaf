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
package org.apache.karaf.shell.console.impl.jline;

import java.io.InputStream;
import java.io.PrintStream;

import org.apache.felix.gogo.runtime.CommandProcessorImpl;
import org.apache.felix.gogo.runtime.CommandSessionImpl;
import org.apache.felix.gogo.runtime.activator.EventAdminListener;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.threadio.ThreadIO;
import org.fusesource.jansi.AnsiConsole;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    private org.apache.felix.gogo.runtime.activator.Activator activator = new WrappedActivator();

    public void start(BundleContext context) throws Exception {
        AnsiConsole.systemInstall();
        activator.start(context);
    }

    public void stop(BundleContext context) throws Exception {
        activator.stop(context);
        AnsiConsole.systemUninstall();
    }

    protected static class WrappedActivator extends org.apache.felix.gogo.runtime.activator.Activator {

        @Override
        protected ServiceRegistration newProcessor(ThreadIO tio, BundleContext context) {
            processor = new WrappedCommandProcessor(tio);
            try
            {
                processor.addListener(new EventAdminListener(context));
            }
            catch (NoClassDefFoundError error)
            {
                // Ignore the listener if EventAdmin package isn't present
            }

            // Setup the variables and commands exposed in an OSGi environment.
            processor.addConstant(CONTEXT, context);
            processor.addCommand("bundles", processor, "addCommand");
            processor.addCommand("bundles", processor, "removeCommand");
            processor.addCommand("bundles", processor, "eval");

            return context.registerService(CommandProcessor.class.getName(), processor, null);
        }

    }

    protected static class WrappedCommandProcessor extends CommandProcessorImpl {

        public WrappedCommandProcessor(ThreadIO tio) {
            super(tio);
        }

        @Override
        public CommandSession createSession(InputStream in, PrintStream out, PrintStream err) {
            CommandSessionImpl session = new WrappedCommandSession(this, in, out, err);
            sessions.put(session, null);
            return session;
        }
    }

    protected static class WrappedCommandSession extends CommandSessionImpl {

        public WrappedCommandSession(CommandProcessorImpl shell, InputStream in, PrintStream out, PrintStream err) {
            super(shell, in, out, err);
        }
        public Object get(String name) {
            Object val = super.get(name);
            if (val == null) {
                val = System.getProperty(name);
            }
            return val;
        }

   }

}
