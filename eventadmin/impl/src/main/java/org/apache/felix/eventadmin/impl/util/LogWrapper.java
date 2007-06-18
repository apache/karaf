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
package org.apache.felix.eventadmin.impl.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * This class mimics the standard OSGi <tt>LogService</tt> interface. An
 * instance of this class will be used by the EventAdmin for all logging. The 
 * implementation of this class sends log messages to standard output, if no
 * <tt>LogService</tt> is present; it uses a log service if one is
 * installed in the framework. To do that without creating a hard dependency on the 
 * package it uses fully qualified class names and registers a listener with the
 * framework hence, it does not need access to the <tt>LogService</tt> class but will 
 * use it if the listener is informed about an available service. By using a 
 * DynamicImport-Package dependency we don't need the package but
 * use it if present. Additionally, all log methods prefix the log message with
 * <tt>EventAdmin: </tt>.
 * 
 * @see org.osgi.service.log.LogService
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
**/
// TODO: At the moment we log a message to all currently available LogServices. 
//       Maybe, we should only log to the one with the highest ranking instead?
//       What is the best practice in this case? 
public class LogWrapper
{
    /**
     * ERROR LEVEL
     * 
     * @see org.osgi.service.log.LogService#LOG_ERROR 
     */
    public static final int LOG_ERROR = 1;

    /**
     * WARNING LEVEL
     * 
     * @see org.osgi.service.log.LogService#LOG_WARNING
     */
    public static final int LOG_WARNING = 2;

    /**
     * INFO LEVEL
     * 
     * @see org.osgi.service.log.LogService#LOG_INFO
     */
    public static final int LOG_INFO = 3;

    /**
     * DEBUG LEVEL
     * 
     * @see org.osgi.service.log.LogService#LOG_DEBUG
     */
    public static final int LOG_DEBUG = 4;

    // A set containing the currently available LogServices. Furthermore used as lock
    private final Set m_loggerRefs = new HashSet();

    // Only null while not set and m_loggerRefs is empty hence, only needs to be 
    // checked in case m_loggerRefs is empty otherwise it will not be null.
    private BundleContext m_context;

    /*
     * A thread save variant of the double checked locking singleton.
     */
    private static class LogWrapperLoader
    {
        static final LogWrapper m_singleton = new LogWrapper();
    }
    
    /**
     * Returns the singleton instance of this LogWrapper that can be used to send
     * log messages to all currently available LogServices or to standard output,
     * respectively.
     * 
     * @return the singleton instance of this LogWrapper.
     */
    public static LogWrapper getLogger()
    {
        return LogWrapperLoader.m_singleton;
    }

    /**
     * Set the <tt>BundleContext</tt> of the bundle. This method registers a service
     * listener for LogServices with the framework that are subsequently used to 
     * log messages.
     * 
     *  @param context The context of the bundle.
     */
    public static void setContext(final BundleContext context)
    {
        LogWrapperLoader.m_singleton.setBundleContext(context);

        try
        {
            context.addServiceListener(new ServiceListener()
            {
                // Add a newly available LogService reference to the singleton.
                public void serviceChanged(final ServiceEvent event)
                {
                    if (ServiceEvent.REGISTERED == event.getType())
                    {
                        LogWrapperLoader.m_singleton.addLoggerRef(
                            event.getServiceReference());
                    }
                    // unregistered services are handled in the next log operation.
                }

            }, "(" + Constants.OBJECTCLASS
                + "=org.osgi.service.log.LogService)");

            // Add all available LogService references to the singleton.
            final ServiceReference[] refs = context.getServiceReferences(
                "org.osgi.service.log.LogService", null);

            if (null != refs)
            {
                for (int i = 0; i < refs.length; i++)
                {
                    LogWrapperLoader.m_singleton.addLoggerRef(refs[i]);
                }
            }
        } catch (InvalidSyntaxException e)
        {
            // this never happens
        }
    } 

    /*
     * The private singleton constructor.
     */
    LogWrapper()
    {
        // Singleton
    }

    /*
     * Add a reference to a newly available LogService
     */
    void addLoggerRef(final ServiceReference ref)
    {
        synchronized (m_loggerRefs)
        {
            m_loggerRefs.add(ref);
        }
    }

    /*
     * Set the context of the bundle in the singleton implementation.
     */
    private void setBundleContext(final BundleContext context)
    {
        synchronized(m_loggerRefs)
        {
            m_context = context;
        }
    }

    /**
     * Log a message with the given log level. Note that this will prefix the message
     * with <tt>EventAdmin: </tt>.
     * 
     * @param level The log level with which to log the msg.
     * @param msg The message to log.
     */
    public void log(final int level, final String msg)
    {
        // The method will remove any unregistered service reference as well.
        synchronized(m_loggerRefs)
        {
            final String logMsg = "EventAdmin: " + msg;
            
            if (!m_loggerRefs.isEmpty())
            {
                // There is at least one LogService available hence, we can use the
                // class as well.
                for (Iterator iter = m_loggerRefs.iterator(); iter.hasNext();)
                {
                    final ServiceReference next = (ServiceReference) iter.next();
                    
                    org.osgi.service.log.LogService logger = 
                        (org.osgi.service.log.LogService) m_context.getService(next);
    
                    if (null != logger)
                    {
                        logger.log(level, logMsg);
                        
                        m_context.ungetService(next);
                    }
                    else
                    {
                        // The context returned null for the reference - it follows 
                        // that the service is unregistered and we can remove it
                        iter.remove();
                    }
                }
            }
            else
            {
                _log(null, level, logMsg, null);
            }
        }
    }

    /**
     * Log a message with the given log level and the associated exception. Note that 
     * this will prefix the message with <tt>EventAdmin: </tt>.
     * 
     * @param level The log level with which to log the msg.
     * @param msg The message to log.
     * @param ex The exception associated with the message.
     */
    public void log(final int level, final String msg, final Throwable ex)
    {
        // The method will remove any unregistered service reference as well.
        synchronized(m_loggerRefs)
        {
            final String logMsg = "EventAdmin: " + msg;
            
            if (!m_loggerRefs.isEmpty())
            {
                // There is at least one LogService available hence, we can use the
                // class as well.
                for (Iterator iter = m_loggerRefs.iterator(); iter.hasNext();)
                {
                    final ServiceReference next = (ServiceReference) iter.next();
                    
                    org.osgi.service.log.LogService logger = 
                        (org.osgi.service.log.LogService) m_context.getService(next);
    
                    if (null != logger)
                    {
                        logger.log(level, logMsg, ex);
                        
                        m_context.ungetService(next);
                    }
                    else
                    {
                        // The context returned null for the reference - it follows 
                        // that the service is unregistered and we can remove it
                        iter.remove();
                    }
                }
            }
            else
            {
                _log(null, level, logMsg, ex);
            }
        }
    }

    /**
     * Log a message with the given log level together with the associated service
     * reference. Note that this will prefix the message with <tt>EventAdmin: </tt>.
     * 
     * @param sr The reference of the service associated with this message.
     * @param level The log level with which to log the msg.
     * @param msg The message to log.
     */
    public void log(final ServiceReference sr, final int level, final String msg)
    {
        // The method will remove any unregistered service reference as well.
        synchronized(m_loggerRefs)
        {
            final String logMsg = "EventAdmin: " + msg;
            
            if (!m_loggerRefs.isEmpty())
            {
                // There is at least one LogService available hence, we can use the
                // class as well.
                for (Iterator iter = m_loggerRefs.iterator(); iter.hasNext();)
                {
                    final ServiceReference next = (ServiceReference) iter.next();
                       
                    org.osgi.service.log.LogService logger = 
                        (org.osgi.service.log.LogService) m_context.getService(next);
    
                    if (null != logger)
                    {
                        logger.log(sr, level, logMsg);
                        
                        m_context.ungetService(next);
                    }
                    else
                    {
                        // The context returned null for the reference - it follows 
                        // that the service is unregistered and we can remove it
                        iter.remove();
                    }
                }
            }
            else
            {
                _log(sr, level, logMsg, null);
            }
        }
    }

    /**
     * Log a message with the given log level, the associated service reference and
     * exception. Note that this will prefix the message with <tt>EventAdmin: </tt>.
     * 
     * @param sr The reference of the service associated with this message.
     * @param level The log level with which to log the msg.
     * @param msg The message to log.
     * @param ex The exception associated with the message.
     */
    public void log(final ServiceReference sr, final int level, final String msg, 
        final Throwable ex)
    {
        // The method will remove any unregistered service reference as well.
        synchronized(m_loggerRefs)
        {
            final String logMsg = "EventAdmin: " + msg;
            
            if (!m_loggerRefs.isEmpty())
            {
                // There is at least one LogService available hence, we can use the
                // class as well.
                for (Iterator iter = m_loggerRefs.iterator(); iter.hasNext();)
                {
                       final ServiceReference next = (ServiceReference) iter.next();
                       
                    org.osgi.service.log.LogService logger = 
                        (org.osgi.service.log.LogService) m_context.getService(next);
    
                    if (null != logger)
                    {
                        logger.log(sr, level, logMsg, ex);
                        
                        m_context.ungetService(next);
                    }
                    else
                    {
                        // The context returned null for the reference - it follows 
                        // that the service is unregistered and we can remove it
                        iter.remove();
                    }
                }
            }
            else
            {
                _log(sr, level, logMsg, ex);
            }
        }
    }

    /*
     * Log the message to standard output. This appends the level to the message.
     * null values are handled appropriate. 
     */
    private void _log(final ServiceReference sr, final int level, final String msg, 
        Throwable ex)
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
                    if ((ex instanceof BundleException)
                        && (((BundleException) ex).getNestedException() != null))
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
