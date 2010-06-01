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
package org.apache.felix.shell.impl;

import java.io.PrintStream;
import java.security.*;

import org.apache.felix.shell.Command;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator
{
    private BundleContext m_context = null;
    private ShellServiceImpl m_shell = null;
    private volatile ServiceTracker m_tracker = null;

    public void start(BundleContext context)
    {
        m_context = context;

        // Register impl service implementation.
        String[] classes = {
            org.apache.felix.shell.ShellService.class.getName(),
            org.ungoverned.osgi.service.shell.ShellService.class.getName()
        };
        context.registerService(classes, m_shell = new ShellServiceImpl(), null);

        // Create a service tracker to listen for command services.
        String filter = "(|(objectClass="
                + org.apache.felix.shell.Command.class.getName()
                + ")(objectClass="
                + org.ungoverned.osgi.service.shell.Command.class.getName()
                + "))";
        try
        {
            m_tracker = new ServiceTracker(
                context, FrameworkUtil.createFilter(filter), m_tracker);
        }
        catch (InvalidSyntaxException ex)
        {
            // This should never happen.
        }

        m_tracker.open();

        // Register "bundlelevel" command service.
        context.registerService(
            org.apache.felix.shell.Command.class.getName(),
            new BundleLevelCommandImpl(m_context), null);

        // Register "cd" command service.
        classes = new String[2];
        classes[0] = org.apache.felix.shell.Command.class.getName();
        classes[1] = org.apache.felix.shell.CdCommand.class.getName();
        context.registerService(
            classes, new CdCommandImpl(m_context), null);

        // Register "find" command service.
        context.registerService(
            org.apache.felix.shell.Command.class.getName(),
            new FindCommandImpl(m_context), null);

        // Register "headers" command service.
        context.registerService(
            org.apache.felix.shell.Command.class.getName(),
            new HeadersCommandImpl(m_context), null);

        // Register "help" command service.
        context.registerService(
            org.apache.felix.shell.Command.class.getName(),
            new HelpCommandImpl(m_context), null);

        // Register "inspect" command service.
        context.registerService(
            org.apache.felix.shell.Command.class.getName(),
            new InspectCommandImpl(m_context), null);

        // Register "install" command service.
        context.registerService(
            org.apache.felix.shell.Command.class.getName(),
            new InstallCommandImpl(m_context), null);

        // Register "log" command service.
        context.registerService(
            org.apache.felix.shell.Command.class.getName(),
            new LogCommandImpl(m_context), null);

        // Register "ps" command service.
        context.registerService(
            org.apache.felix.shell.Command.class.getName(),
            new PsCommandImpl(m_context), null);

        // Register "refresh" command service.
        context.registerService(
            org.apache.felix.shell.Command.class.getName(),
            new RefreshCommandImpl(m_context), null);

        // Register "resolve" command service.
        context.registerService(
            org.apache.felix.shell.Command.class.getName(),
            new ResolveCommandImpl(m_context), null);

        // Register "startlevel" command service.
        context.registerService(
            org.apache.felix.shell.Command.class.getName(),
            new StartLevelCommandImpl(m_context), null);

        // Register "shutdown" command service.
        context.registerService(
            org.apache.felix.shell.Command.class.getName(),
            new ShutdownCommandImpl(m_context), null);

        // Register "start" command service.
        context.registerService(
            org.apache.felix.shell.Command.class.getName(),
            new StartCommandImpl(m_context), null);

        // Register "stop" command service.
        context.registerService(
            org.apache.felix.shell.Command.class.getName(),
            new StopCommandImpl(m_context), null);
        
        // Register "sysprop" command service.
        context.registerService(
            org.apache.felix.shell.Command.class.getName(),
            new SystemPropertiesCommandImpl(), null);

        // Register "uninstall" command service.
        context.registerService(
            org.apache.felix.shell.Command.class.getName(),
            new UninstallCommandImpl(m_context), null);

        // Register "update" command service.
        context.registerService(
            org.apache.felix.shell.Command.class.getName(),
            new UpdateCommandImpl(m_context), null);

        // Register "version" command service.
        context.registerService(
            org.apache.felix.shell.Command.class.getName(),
            new VersionCommandImpl(m_context), null);
    }

    public void stop(BundleContext context)
    {
        m_tracker.close();
    }

    private class ShellServiceImpl implements
        org.apache.felix.shell.ShellService,
        org.ungoverned.osgi.service.shell.ShellService
    {
        public String[] getCommands()
        {
            Object[] commands = m_tracker.getServices();
            String[] names = (commands == null)
                ? new String[0] : new String[commands.length];
            for (int i = 0; i < names.length; i++)
            {
                names[i] = ((Command) commands[i]).getName();
            }
            return names;
        }

        public String getCommandUsage(String name)
        {
            Object[] commands = m_tracker.getServices();
            for (int i = 0; (commands != null) && (i < commands.length); i++)
            {
                Command command = (Command) commands[i];
                if (command.getName().equals(name))
                {
                    return command.getUsage();
                }
            }
            return null;
        }

        public String getCommandDescription(String name)
        {
            Object[] commands = m_tracker.getServices();
            for (int i = 0; (commands != null) && (i < commands.length); i++)
            {
                Command command = (Command) commands[i];
                if (command.getName().equals(name))
                {
                    return command.getShortDescription();
                }
            }
            return null;
        }

        public ServiceReference getCommandReference(String name)
        {
            ServiceReference[] refs = m_tracker.getServiceReferences();
            for (int i = 0; (refs != null) && (i < refs.length); i++)
            {
                Command command = (Command) m_tracker.getService(refs[i]);
                if ((command != null) && command.getName().equals(name))
                {
                    return refs[i];
                }
            }
            return null;
        }

        public void executeCommand(
            String commandLine, PrintStream out, PrintStream err) throws Exception
        {
            commandLine = commandLine.trim();
            String commandName = (commandLine.indexOf(' ') >= 0)
                ? commandLine.substring(0, commandLine.indexOf(' ')) : commandLine;
            Command command = getCommand(commandName);
            if (command != null)
            {
                if (System.getSecurityManager() != null)
                {
                    try
                    {
                        AccessController.doPrivileged(
                            new ExecutePrivileged(command, commandLine, out, err));
                    }
                    catch (PrivilegedActionException ex)
                    {
                        throw ex.getException();
                    }
                }
                else
                {
                    try
                    {
                        command.execute(commandLine, out, err);
                    }
                    catch (Throwable ex)
                    {
                        err.println("Unable to execute command: " + ex);
                        ex.printStackTrace(err);
                    }
                }
            }
            else
            {
                err.println("Command not found.");
            }
        }

        Command getCommand(String name)
        {
            Object[] commands = m_tracker.getServices();
            for (int i = 0; (commands != null) && (i < commands.length); i++)
            {
                Command command = (Command) commands[i];
                if (command.getName().equals(name))
                {
                    return command;
                }
            }
            return null;
        }
    }

    private static class OldCommandWrapper implements Command
    {
        private org.ungoverned.osgi.service.shell.Command m_oldCommand = null;

        public OldCommandWrapper(org.ungoverned.osgi.service.shell.Command oldCommand)
        {
            m_oldCommand = oldCommand;
        }

        public String getName()
        {
            return m_oldCommand.getName();
        }

        public String getUsage()
        {
            return m_oldCommand.getUsage();
        }

        public String getShortDescription()
        {
            return m_oldCommand.getShortDescription();
        }

        public void execute(String line, PrintStream out, PrintStream err)
        {
            m_oldCommand.execute(line, out, err);
        }
    }

    public static class ExecutePrivileged implements PrivilegedExceptionAction
    {
        private Command m_command = null;
        private String m_commandLine = null;
        private PrintStream m_out = null;
        private PrintStream m_err = null;

        public ExecutePrivileged(
            Command command, String commandLine,
            PrintStream out, PrintStream err)
            throws Exception
        {
            m_command = command;
            m_commandLine = commandLine;
            m_out = out;
            m_err = err;
        }

        public Object run() throws Exception
        {
            m_command.execute(m_commandLine, m_out, m_err);
            return null;
        }
    }
}