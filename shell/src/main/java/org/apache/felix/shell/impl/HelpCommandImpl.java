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
        return "help";
    }

    public String getShortDescription()
    {
        return "display impl commands.";
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
                String[] cmds = ss.getCommands();
                String[] usage = new String[cmds.length];
                String[] desc = new String[cmds.length];
                int maxUsage = 0;
                for (int i = 0; i < cmds.length; i++)
                {
                    usage[i] = ss.getCommandUsage(cmds[i]);
                    desc[i] = ss.getCommandDescription(cmds[i]);
                    // Just in case the command has gone away.
                    if ((usage[i] != null) && (desc[i] != null))
                    {
                        maxUsage = Math.max(maxUsage, usage[i].length());
                    }
                }
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < cmds.length; i++)
                {
                    // Just in case the command has gone away.
                    if ((usage[i] != null) && (desc[i] != null))
                    {
                        sb.delete(0, sb.length());
                        for (int j = 0; j < (maxUsage - usage[i].length()); j++)
                        {
                            sb.append(' ');
                        }
                        out.println(usage[i] + sb + " - " + desc[i]);
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
