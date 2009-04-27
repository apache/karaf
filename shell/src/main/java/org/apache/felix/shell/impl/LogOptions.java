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

import java.util.StringTokenizer;

/**
 * Parse and encapsulate command line options
 *
 */
public class LogOptions
{
    private int minLevel = 4;
    private int maxNumberOfLogs = -1;

    public LogOptions(String commandLine)
    {
        StringTokenizer st = new StringTokenizer(commandLine);
        readOptions(st);
    }

    private void readOptions(StringTokenizer st)
    {
        if (st.countTokens() > 1)
        {
            st.nextToken();
            String firstOption = st.nextToken();
            checkOption(firstOption);

            if (st.hasMoreTokens())
            {
                checkOption(st.nextToken());
            }
        }
    }

    private void checkOption(String opt)
    {
        try
        {
            maxNumberOfLogs = Integer.parseInt(opt);
        }
        catch (NumberFormatException nfe)
        {
            //do nothing, it's not a number
        }
        if ("info".equalsIgnoreCase(opt))
        {
            minLevel = 3;
        }
        else if ("warn".equalsIgnoreCase(opt))
        {
            minLevel = 2;
        }
        else if ("error".equalsIgnoreCase(opt))
        {
            minLevel = 1;
        }
    }

    public int getMinLevel()
    {
        return minLevel;
    }

    public int getMaxNumberOfLogs()
    {
        return maxNumberOfLogs;
    }
}