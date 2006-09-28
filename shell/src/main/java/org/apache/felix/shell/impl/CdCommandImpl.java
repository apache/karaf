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

import org.apache.felix.shell.CdCommand;
import org.osgi.framework.BundleContext;

public class CdCommandImpl implements CdCommand
{
    private BundleContext m_context = null;
    private String m_baseURL = "";

    public CdCommandImpl(BundleContext context)
    {
        m_context = context;

        // See if the initial base URL is specified.
        String baseURL = m_context.getProperty(BASE_URL_PROPERTY);
        setBaseURL(baseURL);
    }

    public String getName()
    {
        return "cd";
    }

    public String getUsage()
    {
        return "cd [<base-URL>]";
    }

    public String getShortDescription()
    {
        return "change or display base URL.";
    }

    public void execute(String s, PrintStream out, PrintStream err)
    {
        StringTokenizer st = new StringTokenizer(s, " ");

        // Ignore the command name.
        st.nextToken();

        // No more tokens means to display the base URL,
        // otherwise set the base URL.
        if (st.countTokens() == 0)
        {
            out.println(m_baseURL);
        }
        else if (st.countTokens() == 1)
        {
            setBaseURL(st.nextToken());
        }
        else
        {
            err.println("Incorrect number of arguments");
        }
    }

    public String getBaseURL()
    {
        return m_baseURL;
    }

    public void setBaseURL(String s)
    {
        m_baseURL = (s == null) ? "" : s;
    }
}