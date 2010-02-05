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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;

import org.apache.felix.shell.Command;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;

/**
 * Apache Felix Shell command to display recent log entries
 */
public class LogCommandImpl implements Command
{
    private final BundleContext m_bundleContext;

    public LogCommandImpl(BundleContext context)
    {
        m_bundleContext = context;
    }

    public void execute(String line, PrintStream out, PrintStream err)
    {
        LogOptions options = new LogOptions(line);

        ServiceReference ref =
            m_bundleContext.getServiceReference(LogReaderService.class.getName());
        if (ref != null)
        {
            LogReaderService service = (LogReaderService) m_bundleContext.getService(ref);
            Enumeration entries = service.getLog();

            int index = 0;
            while (entries.hasMoreElements()
                && (options.getMaxNumberOfLogs() < 0 | index < options.getMaxNumberOfLogs()))
            {
                LogEntry entry = (LogEntry) entries.nextElement();
                if (entry.getLevel() <= options.getMinLevel())
                {
                    display(entry, out);
                    index++;
                }
            }
        }
        else
        {
            out.println("No LogReaderService available");
        }
    }

    private void display(LogEntry entry, PrintStream out)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

        StringBuffer buffer = new StringBuffer();
        buffer.append(sdf.format(new Date(entry.getTime()))).append(" ");
        buffer.append(levelAsAString(entry.getLevel())).append(" - ");
        buffer.append("Bundle: ").append(entry.getBundle().getSymbolicName());
        if (entry.getServiceReference() != null)
        {
            buffer.append(" - ");
            buffer.append(entry.getServiceReference().toString());
        }
        buffer.append(" - ").append(entry.getMessage());
        if (entry.getException() != null)
        {
            buffer.append(" - ");
            StringWriter writer = new StringWriter();
            PrintWriter  pw = new PrintWriter(writer);
            entry.getException().printStackTrace(pw);
            buffer.append(writer.toString());
        }

        out.println(buffer.toString());
    }

    private String levelAsAString(int level)
    {
        switch (level)
        {
            case 1:
                return "ERROR";
            case 2:
                return "WARNING";
            case 3:
                return "INFO";
            default:
                return "DEBUG";
        }
    }

    public String getName()
    {
        return "log";
    }

    public String getShortDescription()
    {
        return "list the most recent log entries.";
    }

    public String getUsage()
    {
        return "log [<max log entries>] [error | warn | info | debug]";
    }
}