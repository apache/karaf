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
 * iPOJO Logger.
 * This class is an helper class implementing a simple log system. 
 * This logger sends log messages to a log service if available.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Logger {
    
    /**
     * The iPOJO default log level property.
     */
    public static final String IPOJO_LOG_LEVEL = "ipojo.log.level";

    /**
     * The Log Level ERROR.
     */
    public static final int ERROR = 1;

    /**
     * The Log Level WARNING.
     */
    public static final int WARNING = 2;

    /**
     * The Log Level INFO.
     */
    public static final int INFO = 3;

    /**
     * The Log Level DEBUG.
     */
    public static final int DEBUG = 4;

    /**
     * The Bundle Context used to get the
     * log service.
     */
    private BundleContext m_context;

    /**
     * The name of the logger.
     */
    private String m_name;

    /**
     * The trace level of this logger.
     */
    private int m_level;

    /**
     * Creates a logger.
     * @param context the bundle context
     * @param name the name of the logger
     * @param level the trace level
     */
    public Logger(BundleContext context, String name, int level) {
        m_name = name;
        m_level = level;
        m_context = context;
    }
    
    /**
     * Create a logger.
     * Uses the default logger level.
     * @param context the bundle context
     * @param name the name of the logger
     */
    public Logger(BundleContext context, String name) {
        this(context, name, getDefaultLevel(context));
    }

    /**
     * Logs a message.
     * @param level the level of the message
     * @param msg the the message to log
     */
    public void log(int level, String msg) {
        if (m_level >= level) {
            dispatch(level, msg);
        }
    }

    /**
     * Logs a message with an exception.
     * @param level the level of the message
     * @param msg the message to log
     * @param exception the exception attached to the message
     */
    public void log(int level, String msg, Throwable exception) {
        if (m_level >= level) {
            dispatch(level, msg, exception);
        }
    }
    
    /**
     * Internal log method. 
     * @param level the level of the message.
     * @param msg the message to log
     */
    private void dispatch(int level, String msg) {
        LogService log = null;
        ServiceReference ref = null;
        try {
            ref = m_context.getServiceReference(LogService.class.getName());
            if (ref != null) {
                log = (LogService) m_context.getService(ref);
            }
        } catch (IllegalStateException e) {
            // Handle the case where the iPOJO bundle is stopping
        }

        String message = null;
        switch (level) {
            case DEBUG:
                message = "[DEBUG] " + m_name + " : " + msg;
                if (log != null) {
                    log.log(LogService.LOG_DEBUG, message);
                } else {
                    System.err.println(message);
                }
                break;
            case ERROR:
                message = "[ERROR] " + m_name + " : " + msg;
                if (log != null) {
                    log.log(LogService.LOG_ERROR, message);
                } else {
                    System.err.println(message);
                }
                break;
            case INFO:
                message = "[INFO] " + m_name + " : " + msg;
                if (log != null) {
                    log.log(LogService.LOG_INFO, message);
                } else {
                    System.err.println(message);
                }
                break;
            case WARNING:
                message = "[WARNING] " + m_name + " : " + msg;
                if (log != null) {
                    log.log(LogService.LOG_WARNING, message);
                } else {
                    System.err.println(message);
                }
                break;
            default:
                message = "[UNKNOWN] " + m_name + " : " + msg;
                System.err.println(message);
                break;
        }
        
        if (log != null) {
            m_context.ungetService(ref);
        }
    }

    /**
     * Internal log method.
     * @param level the level of the message.
     * @param msg the message to log
     * @param exception the exception attached to the message
     */
    private void dispatch(int level, String msg, Throwable exception) {
        LogService log = null;
        ServiceReference ref = null;
        try {
            ref = m_context.getServiceReference(LogService.class.getName());
            if (ref != null) {
                log = (LogService) m_context.getService(ref);
            }
        } catch (IllegalStateException e) {
            // Handle the case where the iPOJO bundle is stopping
        }
        
        String message = null;
        switch (level) {
            case DEBUG:
                message = "[DEBUG] " + m_name + " : " + msg;
                if (log != null) {
                    log.log(LogService.LOG_DEBUG, message, exception);
                } else {
                    System.err.println(message);
                    exception.printStackTrace();
                }
                break;
            case ERROR:
                message = "[ERROR] " + m_name + " : " + msg;
                if (log != null) {
                    log.log(LogService.LOG_ERROR, message, exception);
                } else {
                    System.err.println(message);
                    exception.printStackTrace();
                }
                break;
            case INFO:
                message = "[INFO] " + m_name + " : " + msg;
                if (log != null) {
                    log.log(LogService.LOG_INFO, message, exception);
                } else {
                    System.err.println(message);
                    exception.printStackTrace();
                }
                break;
            case WARNING:
                message = "[WARNING] " + m_name + " : " + msg;
                if (log != null) {
                    log.log(LogService.LOG_WARNING, message, exception);
                } else {
                    System.err.println(message);
                    exception.printStackTrace();
                }
                break;
            default:
                message = "[UNKNOWN] " + m_name + " : " + msg;
                System.err.println(message);
                exception.printStackTrace();
                break;
        }
        
        if (log != null) {
            m_context.ungetService(ref);
        }
    }
    
    /**
     * Gets the default logger level.
     * The property is searched inside the framework properties, 
     * the system properties, and in the manifest from the given 
     * bundle context. By default, set the level to {@link Logger#WARNING}. 
     * @param context the bundle context.
     * @return the default log level.
     */
    private static int getDefaultLevel(BundleContext context) {
        // First check in the framework and in the system properties
        String level = context.getProperty(IPOJO_LOG_LEVEL);
        
        // If null, look in bundle manifest
        if (level == null) {
            String key = IPOJO_LOG_LEVEL.replace('.', '-');
            level = (String) context.getBundle().getHeaders().get(key);
        }
        
        if (level != null) {
            if (level.equalsIgnoreCase("info")) {
                return INFO;
            } else if (level.equalsIgnoreCase("debug")) {
                return DEBUG;
            } else if (level.equalsIgnoreCase("warning")) {
                return WARNING;
            } else if (level.equalsIgnoreCase("error")) {
                return ERROR;
            }
        }
        
        // Either l is null, either the specified log level was unknown
        // Set the default to WARNING
        return WARNING;
        
    }
}
