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
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

public class Logger implements ServiceListener{
	
	public static final int ERROR = 1;
    public static final int WARNING = 2;
    public static final int INFO = 3;
    public static final int DEBUG = 4;
    
    private BundleContext m_context;
    private ServiceReference m_ref;
    private LogService m_log;
    
    private String m_name;
    private int m_level;
    
    public Logger(BundleContext bc, String name, int level) { 
    	m_name = name;
    	m_level = level;
    	m_context = bc;
    	
    	m_ref = m_context.getServiceReference(LogService.class.getName());
    	if(m_ref != null) { m_log = (LogService) m_context.getService(m_ref); }
    	
    	try {
			m_context.addServiceListener(this, "(objectClass="+LogService.class.getName() + ")");
		} catch (InvalidSyntaxException e) { e.printStackTrace(); }
    }
    
    public void log(int level, String msg) {
    	if(m_level >= level) {
    		synchronized(this) { _log(level, msg, null); }
    	}
    }
    
    public void log(int level, String msg, Throwable ex) {
    	if(m_level >= level) { 
    		synchronized(this) {_log(level, msg, ex); }
    	}
    }
    
    private void _log(int level, String msg, Throwable ex)
    {
        String s = msg;
        s = (ex == null) ? s : s + " (" + ex.getMessage() + ")";
        String message;
        switch (level)
        {
            case DEBUG:
            	message = "[" + m_name + "] DEBUG: " + s;
            	if(m_log != null) { m_log.log(LogService.LOG_DEBUG, message); }
                System.err.println(message);
                break;
            case ERROR:
            	message = "[" + m_name + "] ERROR: " + s;
            	if(m_log != null) { m_log.log(LogService.LOG_ERROR, message); }
                System.err.println(message);
                break;
            case INFO:
                message = "[" + m_name + "] INFO: " + s;
                if(m_log != null) { m_log.log(LogService.LOG_INFO, message); }
            	System.err.println(message);
                break;
            case WARNING:
            	message = "[" + m_name + "] WARNING: " + s;
            	if(m_log != null) { m_log.log(LogService.LOG_WARNING, message); }
                System.err.println(message);
                break;
            default:
                System.err.println("[" + m_name + "] UNKNOWN[" + level + "]: " + s);
            	break;
        }
    }

	public void serviceChanged(ServiceEvent ev) {
		if(ev.getType() == ServiceEvent.REGISTERED && m_ref == null) {
			m_ref = ev.getServiceReference();
			m_log = (LogService) m_context.getService(m_ref);
		}
		if(ev.getType() == ServiceEvent.UNREGISTERING && m_ref == ev.getServiceReference()) {
			m_context.ungetService(m_ref);
			m_log = null;
			m_ref = m_context.getServiceReference(LogService.class.getName());
	    	if(m_ref != null) { m_log = (LogService) m_context.getService(m_ref); }
		}
		
	}
}
