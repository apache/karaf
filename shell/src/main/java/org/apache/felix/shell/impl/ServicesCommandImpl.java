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
import java.util.*;

import org.apache.felix.shell.Command;
import org.osgi.framework.*;

public class ServicesCommandImpl implements Command
{
    private static final String IN_USE_SWITCH = "-u";
    private static final String SHOW_ALL_SWITCH = "-a";

    private BundleContext m_context = null;

    public ServicesCommandImpl(BundleContext context)
    {
        m_context = context;
    }

    public String getName()
    {
        return "services";
    }

    public String getUsage()
    {
        return "services [-u] [-a] [<id> ...]";
    }

    public String getShortDescription()
    {
        return "list registered or used services.";
    }

    public void execute(String s, PrintStream out, PrintStream err)
    {
        StringTokenizer st = new StringTokenizer(s, " ");

        // Ignore the command name.
        st.nextToken();

        // Put the remaining tokens into a list.
        List tokens = new ArrayList();
        for (int i = 0; st.hasMoreTokens(); i++)
        {
            tokens.add(st.nextToken());
        }

        // Default switch values.
        boolean inUse = false;
        boolean showAll = false;

        // Check for "in use" switch.
        if (tokens.contains(IN_USE_SWITCH))
        {
            // Remove the switch and set boolean flag.
            tokens.remove(IN_USE_SWITCH);
            inUse = true;
        }

        // Check for "show all" switch.
        if (tokens.contains(SHOW_ALL_SWITCH))
        {
            // Remove the switch and set boolean flag.
            tokens.remove(SHOW_ALL_SWITCH);
            showAll = true;
        }

        // If there are bundle IDs specified then print their
        // services and associated service properties, otherwise
        // list all bundles and services.
        if (tokens.size() >= 1)
        {
            while (tokens.size() > 0)
            {
                String id = (String) tokens.remove(0);

                boolean headerPrinted = false;
                boolean needSeparator = false;

                try
                {
                    long l = Long.parseLong(id);
                    Bundle bundle = m_context.getBundle(l);
                    if (bundle != null)
                    {
                        ServiceReference[] refs = null;
                        
                        // Get registered or in-use services.
                        if (inUse)
                        {
                            refs = bundle.getServicesInUse();
                        }
                        else
                        {
                            refs = bundle.getRegisteredServices();
                        }

                        // Print properties for each service.
                        for (int refIdx = 0;
                            (refs != null) && (refIdx < refs.length);
                            refIdx++)
                        {
                            String[] objectClass = (String[])
                                refs[refIdx].getProperty("objectClass");

                            // Determine if we need to print the service, depending
                            // on whether it is a command service or not.
                            boolean print = true;
                            for (int ocIdx = 0;
                                !showAll && (ocIdx < objectClass.length);
                                ocIdx++)
                            {
                                if (objectClass[ocIdx].equals(
                                    org.apache.felix.shell.Command.class.getName()))
                                {
                                    print = false;
                                }
                            }

                            // Print header if we have not already done so.
                            if (!headerPrinted)
                            {
                                headerPrinted = true;
                                String title = Util.getBundleName(bundle);
                                title = (inUse)
                                    ? title + " uses:"
                                    : title + " provides:";
                                out.println("");
                                out.println(title);
                                out.println(Util.getUnderlineString(title));
                            }

                            if (showAll || print)
                            {
                                // Print service separator if necessary.
                                if (needSeparator)
                                {
                                    out.println("----");
                                }

                                // Print service properties.
                                String[] keys = refs[refIdx].getPropertyKeys();
                                for (int keyIdx = 0;
                                    (keys != null) && (keyIdx < keys.length);
                                    keyIdx++)
                                {
                                    Object v = refs[refIdx].getProperty(keys[keyIdx]);
                                    out.println(
                                        keys[keyIdx] + " = " + Util.getValueString(v));
                                }
                                
                                needSeparator = true;
                            }
                        }
                    }
                    else
                    {
                        err.println("Bundle ID " + id + " is invalid.");
                    }
                }
                catch (NumberFormatException ex)
                {
                    err.println("Unable to parse id '" + id + "'.");
                }
                catch (Exception ex)
                {
                    err.println(ex.toString());
                }
            }
        }
        else
        {
            Bundle[] bundles = m_context.getBundles();
            if (bundles != null)
            {
                // TODO: Sort list.
                for (int bundleIdx = 0; bundleIdx < bundles.length; bundleIdx++)
                {
                    boolean headerPrinted = false;
                    ServiceReference[] refs = null;

                    // Get registered or in-use services.
                    if (inUse)
                    {
                        refs = bundles[bundleIdx].getServicesInUse();
                    }
                    else
                    {
                        refs = bundles[bundleIdx].getRegisteredServices();
                    }

                    for (int refIdx = 0; (refs != null) && (refIdx < refs.length); refIdx++)
                    { 
                        String[] objectClass = (String[])
                            refs[refIdx].getProperty("objectClass");

                        // Determine if we need to print the service, depending
                        // on whether it is a command service or not.
                        boolean print = true;
                        for (int ocIdx = 0;
                            !showAll && (ocIdx < objectClass.length);
                            ocIdx++)
                        {
                            if (objectClass[ocIdx].equals(
                                org.apache.felix.shell.Command.class.getName()))
                            {
                                print = false;
                            }
                        }

                        // Print the service if necessary.
                        if (showAll || print)
                        {
                            if (!headerPrinted)
                            {
                                headerPrinted = true;
                                String title = Util.getBundleName(bundles[bundleIdx]);
                                title = (inUse)
                                    ? title + " uses:"
                                    : title + " provides:";
                                out.println("\n" + title);
                                out.println(Util.getUnderlineString(title));
                            }
                            out.println(Util.getValueString(objectClass));
                        }
                    }
                }
            }
            else
            {
                out.println("There are no registered services.");
            }
        }
    }
}
