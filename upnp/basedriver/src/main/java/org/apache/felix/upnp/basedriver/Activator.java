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
import org.osgi.framework.ServiceRegistration;

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
import org.apache.felix.upnp.basedriver.tool.Util;
import org.apache.felix.upnp.extra.controller.DevicesInfo;
import org.apache.felix.upnp.extra.controller.DriverController;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class Activator implements BundleActivator {
	
	private final static String BASEDRIVER_LOG_PROP = "felix.upnpbase.log";
	private final static String EXPORTER_ENABLED_PROP = "felix.upnpbase.exporter.enabled";
	private final static String IMPORTER_ENABLED_PROP = "felix.upnpbase.importer.enabled";
	private final static String NET_ONLY_IPV4_PROP = "felix.upnpbase.cyberdomo.net.onlyIPV4";
	private final static String NET_ONLY_IPV6_PROP = "felix.upnpbase.cyberdomo.net.onlyIPV6";
	private final static String NET_USE_LOOPBACK_PROP = "felix.upnpbase.cyberdomo.net.loopback";
	private final static String CYBERDOMO_LOG_PROP = "felix.upnpbase.cyberdomo.log";
	
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
	
	/**
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		//Setting basic variabile used by everyone
        
 		Activator.bc = context;				
		
 		//
 		// Debugger configuration
 		//
	    String levelStr = Util.getPropertyDefault(context,BASEDRIVER_LOG_PROP,"2");	    
		Activator.logger = new Logger(levelStr);
		
	    String cyberLog = Util.getPropertyDefault(context,CYBERDOMO_LOG_PROP,"false");
	    Activator.logger.setCyberDebug(cyberLog);
	    
	    
 		//
	    // NET configuration
	   	//
	    String useOnlyIPV4 = Util.getPropertyDefault(context,NET_ONLY_IPV4_PROP,"true");
    	if (useOnlyIPV4.equalsIgnoreCase("true"))
            UPnP.setEnable(UPnP.USE_ONLY_IPV4_ADDR);
    	else
    		UPnP.setDisable(UPnP.USE_ONLY_IPV4_ADDR);
    	
       	String useOnlyIPV6 = Util.getPropertyDefault(context,NET_ONLY_IPV6_PROP,"false");
    	if (useOnlyIPV6.equalsIgnoreCase("true"))
            UPnP.setEnable(UPnP.USE_ONLY_IPV6_ADDR);
    	else
    		UPnP.setDisable(UPnP.USE_ONLY_IPV6_ADDR);

       	String useLoopback = Util.getPropertyDefault(context,NET_USE_LOOPBACK_PROP,"false");
    	if (useLoopback.equalsIgnoreCase("true"))
            UPnP.setEnable(UPnP.USE_LOOPBACK_ADDR);
    	else
    		UPnP.setDisable(UPnP.USE_LOOPBACK_ADDR);

    	//
    	// Exporter configuration		
       	//
    	String useExporter = Util.getPropertyDefault(context,EXPORTER_ENABLED_PROP,"true");
       	if (useExporter.equalsIgnoreCase("true")){
			//Setting up Base Driver Exporter
			this.queue = new RootDeviceExportingQueue();
			this.producerDeviceToExport = new RootDeviceListener(queue);
			producerDeviceToExport.activate();
			consumerDeviceToExport = new ThreadExporter(queue);
			new Thread(consumerDeviceToExport, "upnp.basedriver.Exporter").start();
       	}

    	//
       	// Importer configuration		
      	//
       	String useImporter = Util.getPropertyDefault(context,IMPORTER_ENABLED_PROP,"true");
       	if (useImporter.equalsIgnoreCase("true")){
			//Setting up Base Driver Importer
			this.notifierQueue = new NotifierQueue();
			this.subQueue = new SubscriptionQueue();
			ctrl = new MyCtrlPoint(context, subQueue, notifierQueue);
			
			//Enable CyberLink re-new for Event
			ctrl.setNMPRMode(true);
				
			this.monitor=new Monitor();
			this.notifier = new Notifier(notifierQueue,monitor);
			this.subScriber = new SubScriber(ctrl, subQueue,monitor);
			
			ctrl.start();
			subScriber.start();
			notifier.start();
       	}
        
        doControllerRegistration();
        
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
}
