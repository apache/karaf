/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.jetty.internal;

import org.mortbay.log.Logger;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import java.util.Map;
import java.util.HashMap;

public final class JettyLogger
    implements Logger
{
    private final static Map<String, Logger> LOGGERS =
        new HashMap<String, Logger>();

    private final String name;
    private boolean debugEnabled;

    public JettyLogger()
    {
        this("org.mortbay.log");
    }

    public JettyLogger(String name)
    {
        this.name = name;
    }

    public org.mortbay.log.Logger getLogger(String name)
    {
        Logger logger = LOGGERS.get(name);
        if (logger == null) {
            logger = new JettyLogger(name);
            logger.setDebugEnabled(isDebugEnabled());
            LOGGERS.put(name, logger);
        }

        return logger;
    }

    public boolean isDebugEnabled()
    {
        return this.debugEnabled;
    }

    public void setDebugEnabled(boolean enabled)
    {
        this.debugEnabled = enabled;
    }

    public void debug(String msg, Throwable cause)
    {
        SystemLogger.debug(msg);
    }

    public void debug(String msg, Object arg0, Object arg1)
    {
        SystemLogger.debug(format(msg, arg0, arg1));
    }

    public void info(String msg, Object arg0, Object arg1)
    {
        SystemLogger.info(format(msg, arg0, arg1));
    }

    public void warn(String msg, Throwable cause)
    {
        SystemLogger.warning(msg, cause);
    }

    public void warn( String msg, Object arg0, Object arg1 )
    {
        SystemLogger.warning(format(msg, arg0, arg1), null);
    }

    public String toString()
    {
        return this.name;
    }

    private String format(String msg, Object arg0, Object arg1)
    {
        int i0 = msg.indexOf("{}");
        int i1 = i0 < 0 ? -1 : msg.indexOf("{}", i0 + 2);

        if (arg1 != null && i1 >= 0) {
            msg = msg.substring(0, i1) + arg1 + msg.substring(i1 + 2);
        }

        if (arg0 != null && i0 >= 0) {
            msg = msg.substring(0, i0) + arg0 + msg.substring(i0 + 2);
        }

        return msg;
    }
}
