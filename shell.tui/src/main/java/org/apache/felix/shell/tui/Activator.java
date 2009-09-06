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
package org.apache.felix.shell.tui;

import java.io.*;

import org.apache.felix.shell.ShellService;
import org.osgi.framework.*;

public class Activator implements BundleActivator
{
    private static final String CHECK_INPUT_PROP = "shell.tui.checkinput";

    private BundleContext m_context = null;
    private volatile ShellTuiRunnable m_runnable = null;
    private volatile Thread m_thread = null;
    private ServiceReference m_shellRef = null;
    private ShellService m_shell = null;
    private volatile boolean m_checkInput = false;

    public void start(BundleContext context)
    {
        m_context = context;

        // Check for configuration property.
        String s = context.getProperty(CHECK_INPUT_PROP);
        m_checkInput = (s == null) ? false : Boolean.valueOf(s).booleanValue();

        // Listen for registering/unregistering impl service.
        ServiceListener sl = new ServiceListener() {
            public void serviceChanged(ServiceEvent event)
            {
                synchronized (Activator.this)
                {
                    // Initialize the service if we don't have one.
                    if ((event.getType() == ServiceEvent.REGISTERED)
                        && (m_shellRef == null))
                    {
                        initializeService();
                    }
                    // Unget the service if it is unregistering.
                    else if ((event.getType() == ServiceEvent.UNREGISTERING)
                        && event.getServiceReference().equals(m_shellRef))
                    {
                        m_context.ungetService(m_shellRef);
                        m_shellRef = null;
                        m_shell = null;
                        // Try to get another service.
                        initializeService();
                    }
                }
            }
        };
        try
        {
            m_context.addServiceListener(sl,
                "(objectClass="
                + org.apache.felix.shell.ShellService.class.getName()
                + ")");
        }
        catch (InvalidSyntaxException ex)
        {
            System.err.println("ShellTui: Cannot add service listener.");
            System.err.println("ShellTui: " + ex);
        }

        // Now try to manually initialize the impl service
        // since one might already be available.
        initializeService();

        // Start impl thread.
        m_thread = new Thread(
            m_runnable = new ShellTuiRunnable(),
            "Felix Shell TUI");
        m_thread.start();
    }

    private synchronized void initializeService()
    {
        if (m_shell == null)
        {
            m_shellRef = m_context.getServiceReference(
                org.apache.felix.shell.ShellService.class.getName());
            if (m_shellRef != null)
            {
                m_shell = (ShellService) m_context.getService(m_shellRef);
            }
        }
    }

    public void stop(BundleContext context)
    {
        if (m_runnable != null)
        {
            m_runnable.stop();
        }
    }

    private class ShellTuiRunnable implements Runnable
    {
        private volatile boolean m_stop = false;

        public void stop()
        {
            m_stop = true;
        }

        public void run()
        {
            String line = null;
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

            try
            {
                boolean needPrompt = true;
                int available;
                while (!m_stop)
                {
                    if (needPrompt)
                    {
                        System.out.print("-> ");
                        needPrompt = false;
                    }

                    if (m_checkInput)
                    {
                        available = System.in.available();

                        if (available == 0)
                        {
                            try
                            {
                                Thread.sleep(200);
                            }
                            catch (InterruptedException ex)
                            {
                                // No one should be interrupting this thread, so
                                // ignore it.
                            }
                            continue;
                        }
                    }

                    line = in.readLine();
                    if (line == null)
                    {
                        System.err.println("ShellTUI: No standard input...exiting.");
                        break;
                    }
                    needPrompt = true;

                    line = line.trim();
                    if (line.length() == 0)
                    {
                        continue;
                    }

                    synchronized (Activator.this)
                    {
                        if (m_shell == null)
                        {
                            System.out.println("No impl service available.");
                            continue;
                        }

                        try
                        {
                            m_shell.executeCommand(line, System.out, System.err);
                        }
                        catch (Exception ex)
                        {
                            System.err.println("ShellTUI: " + ex);
                            ex.printStackTrace();
                        }
                    }
                }
            }
            catch (IOException ex)
            {
                // Any IO error causes the thread to exit.
                System.err.println("ShellTUI: Unable to read from stdin...exiting.");
            }
        }
    }
}