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
package org.apache.felix.ipojo.junit4osgi.impl;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * Log Service default implementation.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class LogServiceImpl implements LogService {
    
    /**
     * Creates a message.
     * @param level the log level
     * @param msg the message
     * @param exception the thrown exception
     * @return the computed message
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
            message = message + " : " + exception.getMessage();
        }
        
        return message;
    }

    /**
     * Logs a message.
     * @param arg0 the log level
     * @param arg1 the message
     * @see org.osgi.service.log.LogService#log(int, java.lang.String)
     */
    public void log(int arg0, String arg1) {
        System.err.println(computeLogMessage(arg0, arg1, null));
    }

    /**
     * Logs a message.
     * @param arg0 the log level
     * @param arg1 the message
     * @param arg2 the thrown exception
     * @see org.osgi.service.log.LogService#log(int, java.lang.String)
     */
    public void log(int arg0, String arg1, Throwable arg2) {
        System.err.println(computeLogMessage(arg0, arg1, arg2));
    }

    /**
     * Logs a message.
     * @param arg0 the service reference
     * @param arg1 the log level
     * @param arg2 the message
     * @see org.osgi.service.log.LogService#log(ServiceReference, int, String)
     */
    public void log(ServiceReference arg0, int arg1, String arg2) {
        System.err.println(computeLogMessage(arg1, arg2, null));
    }

    /**
     * Logs a message.
     * @param arg0 the service reference
     * @param arg1 the log level
     * @param arg2 the message
     * @param arg3 the thrown exception
     * @see org.osgi.service.log.LogService#log(ServiceReference, int, String, Throwable)
     */
    public void log(ServiceReference arg0, int arg1, String arg2, Throwable arg3) {
        System.err.println(computeLogMessage(arg1, arg2, arg3));
    }

}
