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
import java.util.*;

import org.apache.felix.shell.Command;
import org.osgi.framework.*;

public class Activator implements BundleActivator
{
    private transient BundleContext m_context = null;
    private transient ShellServiceImpl m_shell = null;

    public void start(BundleContext context)
    {
        m_context = context;

        // Register impl service implementation.
        String[] classes = {
            org.apache.felix.shell.ShellService.class.getName(),
            org.ungoverned.osgi.service.shell.ShellService.class.getName()
        };
        context.registerService(classes, m_shell = new ShellServiceImpl(), null);

        // Listen for registering/unregistering of impl command
        // services so that we can automatically add/remove them
        // from our list of available commands.
        ServiceListener sl = new ServiceListener() {
            public void serviceChanged(ServiceEvent event)
            {
                if (event.getType() == ServiceEvent.REGISTERED)
                {
                    m_shell.addCommand(event.getServiceReference());
                }
                else if (event.getType() == ServiceEvent.UNREGISTERING)
                {
                    m_shell.removeCommand(event.getServiceReference());
                }
                else
                {
                }
            }
        };

        try
        {
            m_context.addServiceListener(sl,
                "(|(objectClass="
                + org.apache.felix.shell.Command.class.getName()
                + ")(objectClass="
                + org.ungoverned.osgi.service.shell.Command.class.getName()
                + "))");
        }
        catch (InvalidSyntaxException ex)
        {
            System.err.println("Activator: Cannot register service listener.");
            System.err.println("Activator: " + ex);
        }

        // Now manually try to find any commands that have already
        // been registered (i.e., we didn't see their service events).
        initializeCommands();

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
        m_shell.clearCommands();
    }

    private void initializeCommands()
    {
        synchronized (m_shell)
        {
            try
            {
                ServiceReference[] refs = m_context.getServiceReferences(
                    org.apache.felix.shell.Command.class.getName(), null);
                if (refs != null)
                {
                    for (int i = 0; i < refs.length; i++)
                    {
                        m_shell.addCommand(refs[i]);
                    }
                }
            }
            catch (Exception ex)
            {
                System.err.println("Activator: " + ex);
            }
        }
    }

    private class ShellServiceImpl implements
        org.apache.felix.shell.ShellService,
        org.ungoverned.osgi.service.shell.ShellService
    {
        private HashMap m_commandRefMap = new HashMap();
        private TreeMap m_commandNameMap = new TreeMap();

        public synchronized String[] getCommands()
        {
            Set ks = m_commandNameMap.keySet();
            String[] cmds = (ks == null)
                ? new String[0] : (String[]) ks.toArray(new String[ks.size()]);
            return cmds;
        }

        public synchronized String getCommandUsage(String name)
        {
            Command command = (Command) m_commandNameMap.get(name);
            return (command == null) ? null : command.getUsage();
        }

        public synchronized String getCommandDescription(String name)
        {
            Command command = (Command) m_commandNameMap.get(name);
            return (command == null) ? null : command.getShortDescription();
        }

        public synchronized ServiceReference getCommandReference(String name)
        {
            ServiceReference ref = null;
            Iterator itr = m_commandRefMap.entrySet().iterator();
            while (itr.hasNext())
            {
                Map.Entry entry = (Map.Entry) itr.next();
                if (((Command) entry.getValue()).getName().equals(name))
                {
                    ref = (ServiceReference) entry.getKey();
                    break;
                }
            }
            return ref;
        }

        public synchronized void removeCommand(ServiceReference ref)
        {
            Command command = (Command) m_commandRefMap.remove(ref);
            if (command != null)
            {
                m_commandNameMap.remove(command.getName());
            }
        }

        public synchronized void executeCommand(
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

        protected synchronized Command getCommand(String name)
        {
            Command command = (Command) m_commandNameMap.get(name);
            return (command == null) ? null : command;
        }

        protected synchronized void addCommand(ServiceReference ref)
        {
            Object cmdObj = m_context.getService(ref);
            Command command =
                (cmdObj instanceof org.ungoverned.osgi.service.shell.Command)
                ? new OldCommandWrapper((org.ungoverned.osgi.service.shell.Command) cmdObj)
                : (Command) cmdObj;
            m_commandRefMap.put(ref, command);
            m_commandNameMap.put(command.getName(), command);
        }

        protected synchronized void clearCommands()
        {
            m_commandRefMap.clear();
            m_commandNameMap.clear();
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