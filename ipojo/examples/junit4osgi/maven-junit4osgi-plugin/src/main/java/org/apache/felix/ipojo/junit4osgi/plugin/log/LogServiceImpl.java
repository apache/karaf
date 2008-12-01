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
package org.apache.felix.ipojo.junit4osgi.plugin.log;

import org.apache.felix.ipojo.junit4osgi.plugin.StringOutputStream;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * An implementation of the log service to collect logged messages.
 * This service implementation is also {@link BundleActivator} and is
 * activated when the embedded OSGi platform starts.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class LogServiceImpl implements LogService, BundleActivator {
    
    /**
     * Default output stream (not collected).
     */
    private StringOutputStream m_defaultStream;
  
    /**
     * Collected output stream.
     */
    private StringOutputStream m_outputStream; 
    
    /**
     * Creates the log service object.
     */
    public LogServiceImpl() {
        m_defaultStream = new StringOutputStream();
    }
   
    /**
     * Enables the log messages collection.
     */
    public void enableOutputStream() {
        m_outputStream = new StringOutputStream();
    }
    
    /**
     * Get collected log messages.
     * @return the String containing the logged messages.
     */
    public String getLoggedMessages() {
        return m_outputStream.toString();
    }
    
    /**
     * Re-initializes the collected message list.
     */
    public void reset() {
        m_outputStream = null;
    }

    /**
     * Logs a message.
     * @param arg0 the log level
     * @param arg1 the message
     * @see org.osgi.service.log.LogService#log(int, java.lang.String)
     */
    public void log(int arg0, String arg1) {
        write(computeLogMessage(arg0, arg1, null));
    }

    /**
     * Logs a message with an attached exception.
     * @param arg0 the log level
     * @param arg1 the message
     * @param arg2 the associated exception
     * @see org.osgi.service.log.LogService#log(int, java.lang.String, java.lang.Throwable)
     */
    public void log(int arg0, String arg1, Throwable arg2) {
        write(computeLogMessage(arg0, arg1, arg2));
    }

    /**
     * Logs a message raised by the given service reference.
     * @param arg0 the service reference
     * @param arg1 the log level
     * @param arg2 the message
     * @see org.osgi.service.log.LogService#log(org.osgi.framework.ServiceReference, int, java.lang.String)
     */
    public void log(ServiceReference arg0, int arg1, String arg2) {
        write(computeLogMessage(arg1, arg2, null));
    }

    /**
     * Logs a message raised by the given service reference
     * associated with an exception.
     * @param arg0 the service reference
     * @param arg1 the log level
     * @param arg2 the message
     * @param arg3 the exception
     * @see org.osgi.service.log.LogService#log(org.osgi.framework.ServiceReference, int, java.lang.String)
     */
    public void log(ServiceReference arg0, int arg1, String arg2, Throwable arg3) {
        write(computeLogMessage(arg1, arg2, arg3));
    }
    
    /**
     * Computes the string from the message.
     * @param level the log level
     * @param msg the message
     * @param exception the exception (can be <code>null</code>
     * @return the resulting String
     */
    private String computeLogMessage(int level, String msg, Throwable exception) {
        String message = null;
        switch (level) {
            case LogService.LOG_DEBUG:
                message = "[DEBUG] " + msg;
                break;
            case LogService.LOG_ERROR:
                message = "[ERROR] " + msg;
                break;
            case LogService.LOG_INFO:
                message = "[INFO] " + msg;
                break;
            case LogService.LOG_WARNING:
                message = "[WARNING] " + msg;
                break;
            default:
                break;
        }
        
        if (exception != null) {
            message = message + " : " + exception.getMessage() + "\n";
        }
        
        return message;
    }
    
    /**
     * Writes the given message in the adequate output stream. 
     * @param log the message
     */
    public void write(String log) {
        if (m_outputStream != null) {
            m_outputStream.write(log);
        } else {
            m_defaultStream.write(log);
        }
    }

    /**
     * Stars the log service implementation:
     * Registers the service.
     * @param bc the bundle context.
     * @throws Exception should not happen.
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext bc) throws Exception {
        bc.registerService(LogService.class.getName(), this, null);
    }

    /**
     * Stops the log service implementation.
     * Does nothing.
     * @param arg0 the bundle context
     * @throws Exception should not happen.
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext arg0) throws Exception {
        // Nothing to do.
        
    }

}
