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

import java.util.StringTokenizer;
import org.apache.felix.shell.Command;
import org.apache.felix.shell.ShellService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class HelpCommandImpl implements Command
{
    private BundleContext m_context = null;

    public HelpCommandImpl(BundleContext context)
    {
        m_context = context;
    }

    public String getName()
    {
        return "help";
    }

    public String getUsage()
    {
        return "help [<command> ...]";
    }

    public String getShortDescription()
    {
        return "display available command usage and description.";
    }

    public void execute(String s, PrintStream out, PrintStream err)
    {
        try {
            // Get a reference to the impl service.
            ServiceReference ref = m_context.getServiceReference(
                org.apache.felix.shell.ShellService.class.getName());

            if (ref != null)
            {
                ShellService ss = (ShellService) m_context.getService(ref);

                // Parse command line.
                StringTokenizer st = new StringTokenizer(s, " ");

                // Ignore the command name.
                st.nextToken();

                if (!st.hasMoreTokens())
                {
                    String[] cmds = ss.getCommands();
                    for (int i = 0; i < cmds.length; i++)
                    {
                        out.println(cmds[i]);
                    }
                    out.println("\nUse 'help <command-name>' for more information.");
                }
                else
                {
                    String[] cmds = ss.getCommands();
                    String[] targets = new String[st.countTokens()];
                    for (int i = 0; i < targets.length; i++)
                    {
                        targets[i] = st.nextToken().trim();
                    }
                    boolean found = false;
                    for (int cmdIdx = 0; (cmdIdx < cmds.length); cmdIdx++)
                    {
                        for (int targetIdx = 0; targetIdx < targets.length; targetIdx++)
                        {
                            if (cmds[cmdIdx].equals(targets[targetIdx]))
                            {
                                if (found)
                                {
                                    out.println("---");
                                }
                                found = true;
                                out.println("Command     : "
                                    + cmds[cmdIdx]);
                                out.println("Usage       : "
                                    + ss.getCommandUsage(cmds[cmdIdx]));
                                out.println("Description : "
                                    + ss.getCommandDescription(cmds[cmdIdx]));
                            }
                        }
                    }
                }
            }
            else
            {
                err.println("No ShellService is unavailable.");
            }
        } catch (Exception ex) {
            err.println(ex.toString());
        }
    }
}