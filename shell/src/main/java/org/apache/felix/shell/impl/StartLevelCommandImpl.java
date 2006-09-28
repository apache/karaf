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
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.startlevel.StartLevel;

public class StartLevelCommandImpl implements Command
{
    private BundleContext m_context = null;

    public StartLevelCommandImpl(BundleContext context)
    {
        m_context = context;
    }

    public String getName()
    {
        return "startlevel";
    }

    public String getUsage()
    {
        return "startlevel [<level>]";
    }

    public String getShortDescription()
    {
        return "get or set framework start level.";
    }

    public void execute(String s, PrintStream out, PrintStream err)
    {
        // Get start level service.
        ServiceReference ref = m_context.getServiceReference(
            org.osgi.service.startlevel.StartLevel.class.getName());
        if (ref == null)
        {
            out.println("StartLevel service is unavailable.");
            return;
        }

        StartLevel sl = (StartLevel) m_context.getService(ref);
        if (sl == null)
        {
            out.println("StartLevel service is unavailable.");
            return;
        }

        // Parse command line.
        StringTokenizer st = new StringTokenizer(s, " ");

        // Ignore the command name.
        st.nextToken();

        if (st.countTokens() == 0)
        {
            out.println("Level " + sl.getStartLevel());
        }
        else if (st.countTokens() >= 1)
        {
            String levelStr = st.nextToken().trim();

            try {
                int level = Integer.parseInt(levelStr);
                sl.setStartLevel(level);
            } catch (NumberFormatException ex) {
                err.println("Unable to parse integer '" + levelStr + "'.");
            } catch (Exception ex) {
                err.println(ex.toString());
            }
        }
    }
}