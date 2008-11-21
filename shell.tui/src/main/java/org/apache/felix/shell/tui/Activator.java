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
    private BundleContext m_context = null;
    private volatile ShellTuiRunnable m_runnable = null;
    private volatile Thread m_thread = null;
    private ServiceReference m_shellRef = null;
    private ShellService m_shell = null;

    public void start(BundleContext context)
    {
        m_context = context;

        // Listen for registering/unregistering impl service.
        ServiceListener sl = new ServiceListener() {
            public void serviceChanged(ServiceEvent event)
            {
                synchronized (Activator.this)
                {
                    // Ignore additional services if we already have one.
                    if ((event.getType() == ServiceEvent.REGISTERED)
                        && (m_shellRef != null))
                    {
                        return;
                    }
                    // Initialize the service if we don't have one.
                    else if ((event.getType() == ServiceEvent.REGISTERED)
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
        if (m_shell != null)
        {
            return;
        }
        m_shellRef = m_context.getServiceReference(
            org.apache.felix.shell.ShellService.class.getName());
        if (m_shellRef == null)
        {
            return;
        }
        m_shell = (ShellService) m_context.getService(m_shellRef);
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

            // Check to see if we have stdin.
            try
            {
                System.in.available();
            }
            catch (IOException ex)
            {
                m_stop = true;
            }

            while (!m_stop)
            {
                System.out.print("-> ");

                try
                {
                    line = in.readLine();
                }
                catch (IOException ex)
                {
                    System.err.println("ShellTUI: Error reading from stdin...exiting.");
                    break;
                }

                synchronized (Activator.this)
                {
                    if (line == null)
                    {
                        System.err.println("ShellTUI: No standard input...exiting.");
                        break;
                    }

                    if (m_shell == null)
                    {
                        System.out.println("No impl service available.");
                        continue;
                    }

                    line = line.trim();

                    if (line.length() == 0)
                    {
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
    }
}
