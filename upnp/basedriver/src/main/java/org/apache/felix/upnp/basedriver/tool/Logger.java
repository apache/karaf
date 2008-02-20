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

package org.apache.felix.upnp.basedriver.tool;

import java.io.PrintStream;

import org.apache.felix.upnp.basedriver.Activator;
import org.cybergarage.util.Debug;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class Logger implements ServiceListener {
	
	private ServiceReference rls;
	private LogService osgiLogService;
    private int level;
	private PrintStream out;
	public  final static String NEWLINE = System.getProperty("line.separator");
    public static final String ROW ="\n================REQUEST=====================\n";
    public static final String END_ROW ="--------------------------------------------";
    
	private final static String LEVEL_TO_STRING[] = new String[]{
		"",
		"ERROR [ " + Activator.bc.getBundle().getBundleId() + " ] ",
		"WARNING [ " + Activator.bc.getBundle().getBundleId() + " ] ",
		"INFO [ " + Activator.bc.getBundle().getBundleId() + " ] ",
		"DEBUG [ " + Activator.bc.getBundle().getBundleId() + " ] ",
	};
	
	
	/**
	 * Create a Logger with <code>System.out</code> as <tt>PrintStream</tt> and without 
	 * 		reporting message on both <tt>PrintStream</tt> and <tt>LogService</tt>
	 * 
	 * @param log <tt>ServiceReference</tt> to the <tt>LogService</tt> to use, 
	 * 		or null to avoid the use of this service
	 * 
	 * @see #Logger(LogService, PrintStream, boolean)
	 */
	public Logger(String levelStr){
	    this.out = System.out;
		try {
	        this.level = Integer.parseInt(levelStr);
	    } catch (Exception ex){
	    	out.println("WARNING [UPnPBaseDriver Log]: " + levelStr+" is not a valid value!");
	    	this.level=2;
	    }
 	    findService();
	}

    public void setCyberDebug(String value){
        try {
            if (Boolean.valueOf(value).booleanValue()){
                Debug.on();
                out.println("INFO [UPnPBaseDriver] Started CyberLink Debug");
            }
       } catch (Exception ex){
            out.println("WARNING [UPnPBaseDriver CyberLog]: " + value +" is not a valid value!");
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////// programmatic interface ////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    
    public void setLogLevel(int level){
        if (level < 0 || level >4 ) throw new IllegalArgumentException("Log Level must be [0-4]");
        this.level = level;
    }
    
    public int getLogLevel(){
        return this.level;
    }
    
    
    public void setCyberDebug(boolean value){
        if (value) Debug.on();
        else Debug.off();
    }
    
    public boolean getCyberDebug(){
        return Debug.isOn();
    }
    //////////////////////////// end programmatic interface ////////////////////////////
   
    
	public final void ERROR(String message) {
	    log(1,message);
	}
	
	public final void WARNING(String message) {
	    log(2,message);
	}
	
	public final void INFO(String message) {
	    log(3,message);
	}
	
    public final void DEBUG(String message) {
        log(4,message);
    }
    
    public final void PACKET(String message) {
        log(4, new StringBuffer(ROW).append(message).append(END_ROW).toString());
    }

    /**
     * Logs a message.
     *
     * <p>The <tt>ServiceReference</tt> field and the <tt>Throwable</tt>
     * field of the <tt>LogEntry</tt> object will be set to <tt>null</tt>.
     * @param msglevel The severity of the message.
     * This should be one of the defined log levels
     * but may be any integer that is interpreted in a user defined way.
     * @param message Human readable string describing the condition or <tt>null</tt>.
     * @see #LOG_ERROR
     * @see #LOG_WARNING
     * @see #LOG_INFO
     * @see #LOG_DEBUG
     */
	public void log(int msglevel, String message) {
		synchronized (this) {
            if (msglevel <= this.level){
    			if (this.osgiLogService != null ){
    				    osgiLogService.log(msglevel, message);
    			}
    			else {
    				StringBuffer sb = new StringBuffer(Logger.LEVEL_TO_STRING[msglevel]);
    			    this.out.println(sb.append(message));
    			}
            }
		}
		
	}

	 /**
     * Logs a message with an exception.
     *
     * <p>The <tt>ServiceReference</tt> field of the <tt>LogEntry</tt> object will be set to <tt>null</tt>.
     * @param msglevel The severity of the message.
     * This should be one of the defined log levels
     * but may be any integer that is interpreted in a user defined way.
     * @param message The human readable string describing the condition or <tt>null</tt>.
     * @param exception The exception that reflects the condition or <tt>null</tt>.
     * @see #LOG_ERROR
     * @see #LOG_WARNING
     * @see #LOG_INFO
     * @see #LOG_DEBUG
     */
	public void log(int msglevel, String message, Throwable exception) {
		synchronized (this) {
            if (msglevel <= this.level){ 
    			if(this.osgiLogService != null){
    				    osgiLogService.log(msglevel, message, exception);
    			}
    			else {
    				StringBuffer sb = new StringBuffer(Logger.LEVEL_TO_STRING[msglevel]);
    			    this.out.println(sb.append(message).append(NEWLINE).append(exception));
    				exception.printStackTrace(this.out);
    			}
            }
		}
	}

	private synchronized void setLogService(ServiceReference reference){
		this.rls = reference;
		this.osgiLogService = (LogService) Activator.bc.getService(rls);
	}	
	/**
	 * This look for a <tt>LogService</tt> if it founds no <tt>LogService</tt> will register a new
	 * Listener of LogService 
	 *
	 */
	private synchronized void findService() {
		//PRE:Actually no LogService are setted and we are registered as ServiceListener 
		//		for LogService (unregisterin event)
		this.rls = Activator.bc.getServiceReference(LogService.class.getName());
		if (this.rls != null){
			this.osgiLogService = (LogService) Activator.bc.getService(rls);
		}
		try {
			Activator.bc.addServiceListener(this, 
					"(" + Constants.OBJECTCLASS	+ "=" + LogService.class.getName() + ")"
			);
		} catch (InvalidSyntaxException ignore) {}				
		//POST: We are connected to a LogService or we are registered as ServiceListener 
		//		for LogService(registering event)
	}
	
	private synchronized void releaseLogService() {
        if( osgiLogService != null)
            Activator.bc.ungetService(this.rls);
		this.rls = null;
		this.osgiLogService = null;
	}
	
	/**
	 * Used to keep track the existence of a <tt>LogService</tt>
	 * 
	 * @see ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
	 */
	public void serviceChanged(ServiceEvent e) {
		switch (e.getType()) {
			case ServiceEvent.REGISTERED: {
			    // put here code check for serviceid
				setLogService(e.getServiceReference());
			}break;
	
			case ServiceEvent.MODIFIED: 
			break;
	
			case ServiceEvent.UNREGISTERING: {				
			    // put here code check for serviceid
				releaseLogService();
			}break;
		}		
	}

	/**
	 * Stop using the <tt>LogService</tt> and listening for those service event
	 * 
	 * NOTE: All the message will be reported to <tt>PrintStream</tt>
	 *
	 */
	public void close(){	
		Activator.bc.removeServiceListener(this);
		releaseLogService();
	}
}
