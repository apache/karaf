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
package org.apache.felix.ipojo.util;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * iPOJO Logger. This logger send log message to a log service if presents.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Logger {

    /**
     * Log Level ERROR.
     */
    public static final int ERROR = 1;

    /**
     * Log Level WARNING.
     */
    public static final int WARNING = 2;

    /**
     * Log Level INFO.
     */
    public static final int INFO = 3;

    /**
     * Log Level DEBUG.
     */
    public static final int DEBUG = 4;

    /**
     * Bundle Context.
     */
    private BundleContext m_context;

    /**
     * Name of the logger.
     */
    private String m_name;

    /**
     * trace level of this logger.
     */
    private int m_level;

    /**
     * Constructor.
     * 
     * @param bc : bundle context
     * @param name : name of the logger
     * @param level : trace level
     */
    public Logger(BundleContext bc, String name, int level) {
        m_name = name;
        m_level = level;
        m_context = bc;

    }

    /**
     * Log a message.
     * 
     * @param level : level of the message
     * @param msg : the message to log
     */
    public void log(int level, String msg) {
        if (m_level >= level) {
            synchronized (this) {
                dispatch(level, msg, null);
            }
        }
    }

    /**
     * Log a message with an exception.
     * 
     * @param level : level of the message
     * @param msg : message to log
     * @param ex : exception attached to the message
     */
    public void log(int level, String msg, Throwable ex) {
        if (m_level >= level) {
            synchronized (this) {
                dispatch(level, msg, ex);
            }
        }
    }

    /**
     * Internal log method.
     * 
     * @param level : level of the message.
     * @param msg : message to log
     * @param ex : exception attached to the message
     */
    private void dispatch(int level, String msg, Throwable ex) {
        String s = msg;
        if (ex != null) {
            s += " (" + ex.getMessage() + ")";
        }
        
        ServiceReference ref = m_context.getServiceReference(LogService.class.getName());
        LogService log = null;
        if (ref != null) {
            log = (LogService) m_context.getService(ref);
        }
        
        String message = null;
        switch (level) {
            case DEBUG:
                message = "[" + m_name + "] DEBUG: " + s;
                if (log != null) {
                    log.log(LogService.LOG_DEBUG, message);
                }
                System.err.println(message);
                break;
            case ERROR:
                message = "[" + m_name + "] ERROR: " + s;
                if (log != null) {
                    log.log(LogService.LOG_ERROR, message);
                }
                System.err.println(message);
                break;
            case INFO:
                message = "[" + m_name + "] INFO: " + s;
                if (log != null) {
                    log.log(LogService.LOG_INFO, message);
                }
                System.err.println(message);
                break;
            case WARNING:
                message = "[" + m_name + "] WARNING: " + s;
                if (log != null) {
                    log.log(LogService.LOG_WARNING, message);
                }
                System.err.println(message);
                break;
            default:
                System.err.println("[" + m_name + "] UNKNOWN[" + level + "]: " + s);
                break;
        }
        
        if (log != null) {
            m_context.ungetService(ref);
        }
    }
}
