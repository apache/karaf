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
package org.apache.felix.framework;

import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

/**
 * <p>
 * This class mimics the standard OSGi <tt>LogService</tt> interface. An
 * instance of this class will be used by the framework for all logging. Currently,
 * the implementation of this class just sends log messages to standard output,
 * but in the future it will be modified to use a log service if one is
 * installed in the framework. To do so, it will need to use reflection to
 * call the log service methods, since it will not have access to the
 * <tt>LogService</tt> class.
 * </p>
**/
// TODO: Modify LogWrapper to get LogService service object and invoke with reflection.
public class Logger
{
    public static final int LOG_ERROR = 1;
    public static final int LOG_WARNING = 2;
    public static final int LOG_INFO = 3;
    public static final int LOG_DEBUG = 4;

    private Object m_logObj = null;

    public Logger()
    {
    }

    public void log(int level, String msg)
    {
        synchronized (this)
        {
            if (m_logObj != null)
            {
// Will use reflection.
//                m_logObj.log(level, msg);
            }
            else
            {
                _log(null, level, msg, null);
            }
        }
    }

    public void log(int level, String msg, Throwable ex)
    {
        synchronized (this)
        {
            if (m_logObj != null)
            {
// Will use reflection.
//                m_logObj.log(level, msg);
            }
            else
            {
                _log(null, level, msg, ex);
            }
        }
    }

    public void log(ServiceReference sr, int level, String msg)
    {
        synchronized (this)
        {
            if (m_logObj != null)
            {
// Will use reflection.
//                m_logObj.log(level, msg);
            }
            else
            {
                _log(sr, level, msg, null);
            }
        }
    }

    public void log(ServiceReference sr, int level, String msg, Throwable ex)
    {
        synchronized (this)
        {
            if (m_logObj != null)
            {
// Will use reflection.
//                m_logObj.log(level, msg);
            }
            else
            {
                _log(sr, level, msg, ex);
            }
        }
    }
    
    private void _log(ServiceReference sr, int level, String msg, Throwable ex)
    {
        String s = (sr == null) ? null : "SvcRef " + sr;
        s = (s == null) ? msg : s + " " + msg;
        s = (ex == null) ? s : s + " (" + ex + ")";
        switch (level)
        {
            case LOG_DEBUG:
                System.out.println("DEBUG: " + s);
                break;
            case LOG_ERROR:
                System.out.println("ERROR: " + s);
                if (ex != null)
                {
                    if ((ex instanceof BundleException) &&
                        (((BundleException) ex).getNestedException() != null))
                    {
                        ex = ((BundleException) ex).getNestedException();
                    }
                    ex.printStackTrace();
                }
                break;
            case LOG_INFO:
                System.out.println("INFO: " + s);
                break;
            case LOG_WARNING:
                System.out.println("WARNING: " + s);
                break;
            default:
                System.out.println("UNKNOWN[" + level + "]: " + s);
        }
    }
}