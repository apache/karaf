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
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.startlevel.StartLevel;

/**
 * Shell command to display a list of bundles whose
 * Bundle-Name or Bundle-Symbolic-Name contains
 * a specified string
 *
 */
public class FindCommandImpl extends PsCommandImpl
{
    public FindCommandImpl(BundleContext context)
    {
        super(context);
    }

    public void execute(String line, PrintStream out, PrintStream err)
    {
        StringTokenizer st = new StringTokenizer(line);
        if (st.countTokens() < 2)
        {
            out.println("Please specify a bundle name");
            return;
        }

        // Get start level service.
        ServiceReference ref = m_context.getServiceReference(
            org.osgi.service.startlevel.StartLevel.class.getName());
        StartLevel sl = null;
        if (ref != null)
        {
            sl = (StartLevel) m_context.getService(ref);
        }

        if (sl == null)
        {
            out.println("StartLevel service is unavailable.");
        }

        st.nextToken();
        String pattern = st.nextToken();

        Bundle[] bundles = m_context.getBundles();

        List found = new ArrayList();

        for (int i = 0; i < bundles.length; i++)
        {
            Bundle bundle = bundles[i];
            String name = (String) bundle.getHeaders().get(Constants.BUNDLE_NAME);
            if (match(bundle.getSymbolicName(), pattern) || match(name, pattern))
            {
                found.add(bundle);
            }
        }

        if (found.size() > 0)
        {
            printBundleList((Bundle[]) found.toArray(new Bundle[found.size()]), sl, out, false, false, false);
        }
        else
        {
            out.println("No matching bundles found");
        }

    }

    private boolean match(String name, String pattern)
    {
        return name != null && name.toLowerCase().contains(pattern.toLowerCase());
    }

    public String getName()
    {
        return "find";
    }

    public String getShortDescription()
    {
        return "find bundles by name.";
    }

    public String getUsage()
    {
        return "find <bundle name>";
    }
}