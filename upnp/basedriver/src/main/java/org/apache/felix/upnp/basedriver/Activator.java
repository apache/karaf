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

package org.apache.felix.upnp.basedriver;

import java.util.Enumeration;
import java.util.Properties;

import org.cybergarage.upnp.UPnP;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;

import org.apache.felix.upnp.basedriver.controller.DevicesInfo;
import org.apache.felix.upnp.basedriver.controller.DriverController;
import org.apache.felix.upnp.basedriver.controller.impl.DriverControllerImpl;
import org.apache.felix.upnp.basedriver.export.RootDeviceExportingQueue;
import org.apache.felix.upnp.basedriver.export.RootDeviceListener;
import org.apache.felix.upnp.basedriver.export.ThreadExporter;
import org.apache.felix.upnp.basedriver.importer.core.MyCtrlPoint;
import org.apache.felix.upnp.basedriver.importer.core.event.structs.Monitor;
import org.apache.felix.upnp.basedriver.importer.core.event.structs.NotifierQueue;
import org.apache.felix.upnp.basedriver.importer.core.event.structs.SubscriptionQueue;
import org.apache.felix.upnp.basedriver.importer.core.event.thread.Notifier;
import org.apache.felix.upnp.basedriver.importer.core.event.thread.SubScriber;
import org.apache.felix.upnp.basedriver.tool.Logger;
import org.apache.felix.upnp.basedriver.util.Constants;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class Activator implements BundleActivator {
	
	
	public static BundleContext bc;
    public static Logger logger;        
	private RootDeviceExportingQueue queue;
	private RootDeviceListener producerDeviceToExport;
	private ThreadExporter consumerDeviceToExport;

	private MyCtrlPoint ctrl;
	private SubScriber subScriber;
	private Notifier notifier;
	private NotifierQueue notifierQueue;
	private SubscriptionQueue subQueue;
	private Monitor monitor;
    private DriverControllerImpl drvController;
    private ServiceRegistration drvControllerRegistrar;
    
    private Properties configuration;
    
	
	/**
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		//Setting basic variabile used by everyone
        
 		Activator.bc = context;				
		
 		doInitProperties();
 		
 		doInitLogger();
 		
 		doInitUPnPStack();
 		
 		doInitExporter();
 		
 		doInitImporter();
 		
        doControllerRegistration();
        
        Activator.logger.DEBUG( "org.apache.felix.upnp.basedriver Configuration:\n"+
        		configuration.toString()
        );
        
	}


    private void doInitProperties() {
    	/*
    	 * Loading default properties value from the embeeded properties file
    	 */
    	configuration = new Properties();    
    	try {    		
    		configuration.load(Activator.class.getResourceAsStream("resources/upnp.properties"));
		} catch (Exception ignored) {
		}
		
		/*
		 * Overiding default value with the properties defined in the Bundle/System
		 */
		Enumeration e = configuration.propertyNames();
		while (e.hasMoreElements()) {
			String name = (String) e.nextElement();
			String systemValue = Activator.bc.getProperty(name);
			if(systemValue!=null){
				configuration.setProperty(name, systemValue);
			}
		}
		
	}


	/**
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
        
        drvControllerRegistrar.unregister();
        
		//Base Driver Exporter
        if (consumerDeviceToExport != null) {
			consumerDeviceToExport.end();
			consumerDeviceToExport.cleanUp();
			producerDeviceToExport.deactive();
        }

		//Base Driver Importer
        if (ctrl != null){
			ctrl.stop();
			subScriber.close();
			notifier.close();
        }
        
		Activator.logger.close();
		Activator.logger=null;
		Activator.bc = null;
	}

	/**
	 * Method used for initilizing the general properties of the UPnP Base Driver
	 * 
	 * @since 0.3
	 */
	private void doInitLogger() {
		
 	    String levelStr = configuration.getProperty(Constants.BASEDRIVER_LOG_PROP,"2");	    
		Activator.logger = new Logger(levelStr);
		
	    String cyberLog = configuration.getProperty(Constants.CYBERDOMO_LOG_PROP,"false");
	    Activator.logger.setCyberDebug(cyberLog);	    

	}

	/**
	 * Method used for initilizing the UPnP SDK component used by the UPnP Base Driver
	 * 
	 * @since 0.3
	 */
	private void doInitUPnPStack() {
		boolean useOnlyIPV4 = Boolean.valueOf(configuration.getProperty(Constants.NET_ONLY_IPV4_PROP,"true")).booleanValue();
	    if (useOnlyIPV4) UPnP.setEnable(UPnP.USE_ONLY_IPV4_ADDR);
    	else UPnP.setDisable(UPnP.USE_ONLY_IPV4_ADDR);
	   
	    String javaVersion = Activator.bc.getProperty("java.version");
	    if(!(javaVersion == null 
	    		|| javaVersion.startsWith("1.0") || javaVersion.startsWith("1.1")
	    		|| javaVersion.startsWith("1.2") || javaVersion.startsWith("1.3")  
	    ))	    	
	    {
	    	boolean useOnlyIPV6 = Boolean.valueOf(configuration.getProperty(Constants.NET_ONLY_IPV6_PROP,"false")).booleanValue();
		
		/*
		 * Defining an alias for UPnP.USE_ONLY_IPV6_ADDR in order to allow compilation of the code either with upnp-stack and upnp-stack-jdk13
		 */
		final int ALIAS_USE_ONLY_IPV6_ADDR=1;
		
	    	if (useOnlyIPV6) UPnP.setEnable(ALIAS_USE_ONLY_IPV6_ADDR);
	    	else UPnP.setDisable(ALIAS_USE_ONLY_IPV6_ADDR);
	    }
	    
		boolean useLoopback = Boolean.valueOf(configuration.getProperty(Constants.NET_USE_LOOPBACK_PROP,"false")).booleanValue();
    	if (useLoopback) UPnP.setEnable(UPnP.USE_LOOPBACK_ADDR);
    	else UPnP.setDisable(UPnP.USE_LOOPBACK_ADDR);
    	
	}

	/**
	/**
	 * Method used for initilizing the Exporter component of the UPnP Base Driver
	 * @throws InvalidSyntaxException 
	 * 
	 * @since 0.3
	 * @throws InvalidSyntaxException
	 */
	private void doInitExporter() throws InvalidSyntaxException {		
		boolean useExporter = Boolean.valueOf(configuration.getProperty(Constants.EXPORTER_ENABLED_PROP,"true")).booleanValue();
      	if (!useExporter) return;
   		      	
		this.queue = new RootDeviceExportingQueue();
		this.producerDeviceToExport = new RootDeviceListener(queue);
		producerDeviceToExport.activate();
		consumerDeviceToExport = new ThreadExporter(queue);
		new Thread(consumerDeviceToExport, "upnp.basedriver.Exporter").start();
       	
	}
	
	/**
	 * Method used for initilizing the Import component of the UPnP Base Driver
	 * 
	 * @since 0.3
	 */
	private void doInitImporter() {
		boolean useImporter = Boolean.valueOf(configuration.getProperty(Constants.IMPORTER_ENABLED_PROP,"true")).booleanValue();
      	if (!useImporter) return;
   		
   		
		//Setting up Base Driver Importer
		this.notifierQueue = new NotifierQueue();
		this.subQueue = new SubscriptionQueue();
		ctrl = new MyCtrlPoint(Activator.bc, subQueue, notifierQueue);
		
		//Enable CyberLink re-new for Event
		ctrl.setNMPRMode(true);
			
		this.monitor=new Monitor();
		this.notifier = new Notifier(notifierQueue,monitor);
		this.subScriber = new SubScriber(ctrl, subQueue,monitor);
		
		ctrl.start();
		subScriber.start();
		notifier.start();
	}



	private void doControllerRegistration() {
        drvController = new DriverControllerImpl(ctrl);
        drvControllerRegistrar = bc.registerService(
            new String[]{
            		DriverController.class.getName(),
            		DevicesInfo.class.getName()},
            drvController,
            null
        );       
    }
	
	
}
