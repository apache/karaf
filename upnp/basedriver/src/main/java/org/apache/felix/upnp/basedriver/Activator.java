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
import org.apache.felix.upnp.basedriver.util.Converter;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class Activator implements BundleActivator {
	
	public final static String BASEDRIVER_LOG_PROP = "felix.upnpbase.log";
	public final static String CYBERDOMO_LOG_PROP = "felix.upnpbase.cyberdomo.log";
	public final static String EXPORTER_ENABLED_PROP = "felix.upnpbase.exporter.enabled";
	public final static String IMPORTER_ENABLED_PROP = "felix.upnpbase.importer.enabled";
	public final static String NET_ONLY_IPV4_PROP = "felix.upnpbase.cyberdomo.net.onlyIPV4";
	public final static String NET_ONLY_IPV6_PROP = "felix.upnpbase.cyberdomo.net.onlyIPV6";
	public final static String NET_USE_LOOPBACK_PROP = "felix.upnpbase.cyberdomo.net.loopback";
	
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
    
	private boolean useExporter = true;
	private boolean useImporter = true;
	
	private boolean useOnlyIPV4 = true;
	private boolean useOnlyIPV6 = false;
	private boolean useLoopback = false;	
	
	/**
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		//Setting basic variabile used by everyone
        
 		Activator.bc = context;				
		
 		doInitProperties();
 		
 		doInitUPnPStack();
 		
 		doInitExporter();
 		
 		doInitImporter();
 		
        doControllerRegistration();
        
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
	
	public final String getPropertyDefault(BundleContext bc, String propertyName, String defaultValue ){
		String value = bc.getProperty(propertyName);
		if(value == null)
			return defaultValue;
		return value;
	}


	
	/**
	 * Method used for initilizing the Import component of the UPnP Base Driver
	 * 
	 * @since 0.3
	 */
	private void doInitImporter() {
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

	/**
	/**
	 * Method used for initilizing the Exporter component of the UPnP Base Driver
	 * @throws InvalidSyntaxException 
	 * 
	 * @since 0.3
	 * @throws InvalidSyntaxException
	 */
	private void doInitExporter() throws InvalidSyntaxException {		
       	if (!useExporter) return;
   		
       	
		//Setting up Base Driver Exporter
		this.queue = new RootDeviceExportingQueue();
		this.producerDeviceToExport = new RootDeviceListener(queue);
		producerDeviceToExport.activate();
		consumerDeviceToExport = new ThreadExporter(queue);
		new Thread(consumerDeviceToExport, "upnp.basedriver.Exporter").start();
       	
	}

	/**
	 * Method used for initilizing the UPnP SDK component used by the UPnP Base Driver
	 * 
	 * @since 0.3
	 */
	private void doInitUPnPStack() {
		
	    if (useOnlyIPV4) UPnP.setEnable(UPnP.USE_ONLY_IPV4_ADDR);
    	else UPnP.setDisable(UPnP.USE_ONLY_IPV4_ADDR);

	    if (useOnlyIPV6) UPnP.setEnable(UPnP.USE_ONLY_IPV6_ADDR);
    	else UPnP.setDisable(UPnP.USE_ONLY_IPV6_ADDR);

    	if (useLoopback) UPnP.setEnable(UPnP.USE_LOOPBACK_ADDR);
    	else UPnP.setDisable(UPnP.USE_LOOPBACK_ADDR);
    	
	}

	/**
	 * Method used for initilizing the general properties of the UPnP Base Driver
	 * 
	 * @since 0.3
	 */
	private void doInitProperties() {
		
 		//
 		// Debugger configuration
 		//
	    String levelStr = getPropertyDefault(Activator.bc,BASEDRIVER_LOG_PROP,"2");	    
		Activator.logger = new Logger(levelStr);
		
	    String cyberLog = getPropertyDefault(Activator.bc,CYBERDOMO_LOG_PROP,"false");
	    Activator.logger.setCyberDebug(cyberLog);	    

 		//
	    // NET configuration
	   	//
	    try {
			useOnlyIPV4 = ((Boolean) Converter.parseString(
					getPropertyDefault(Activator.bc,NET_ONLY_IPV4_PROP,"true"),"boolean"
			)).booleanValue();
		} catch (Exception e) {
			logger.WARNING(NET_ONLY_IPV4_PROP+" initialized with wrong value, using default "+useOnlyIPV4);
		}
    	
       	try {
			useOnlyIPV6 = ((Boolean) Converter.parseString(
					getPropertyDefault(Activator.bc,NET_ONLY_IPV6_PROP,"false"),"boolean"
			)).booleanValue();
		} catch (Exception e) {
			logger.WARNING(NET_ONLY_IPV6_PROP+" initialized with wrong value, using default "+useOnlyIPV6);
		}
       	
       	try {
			useLoopback = ((Boolean) Converter.parseString(
					getPropertyDefault(Activator.bc,NET_USE_LOOPBACK_PROP,"false"),"boolean"
			)).booleanValue();
		} catch (Exception e) {
			logger.WARNING(NET_USE_LOOPBACK_PROP+" initialized with wrong value, using default "+useLoopback);
		}
    	
    	//
    	// Exporter configuration		
       	//
    	try {
			useExporter = ((Boolean) Converter.parseString(
					getPropertyDefault(Activator.bc,EXPORTER_ENABLED_PROP,"true"),"boolean"    	
			)).booleanValue();
		} catch (Exception e) {
			logger.WARNING(EXPORTER_ENABLED_PROP+" initialized with wrong value, using default "+useExporter);
		}
    	
    	//
       	// Importer configuration		
      	//
       	try {
			useImporter = ((Boolean) Converter.parseString(
					getPropertyDefault(Activator.bc,IMPORTER_ENABLED_PROP,"true"),"boolean"
					
			)).booleanValue();
		} catch (Exception e) {
			logger.WARNING(IMPORTER_ENABLED_PROP+" initialized with wrong value, using default "+useImporter);
		}
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
