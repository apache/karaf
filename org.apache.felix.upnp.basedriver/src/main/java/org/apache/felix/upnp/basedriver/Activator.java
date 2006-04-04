/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.felix.upnp.basedriver;

import org.apache.felix.upnp.extra.controller.DevicesInfo;
import org.apache.felix.upnp.extra.controller.DriverController;

import org.cybergarage.upnp.UPnP;
import org.cybergarage.xml.parser.JaxpParser;

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

/**
 * @author Stefano "Kismet" Lenzi, 
 * @author Francesco Furfari
 * 
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
	
	/**
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		//Setting basic variabile used by everyone
        
 		Activator.bc = context;				
		
	    String levelStr = (String) Util.getPropertyDefault(context,"domoware.upnpbase.log","2");	    
		Activator.logger = new Logger(levelStr);
	    String cyberLog = (String) Util.getPropertyDefault(context,"domoware.upnpbase.cyberlink.log","false");
	    Activator.logger.setCyberDebug(cyberLog);

        UPnP.setXMLParser(new JaxpParser());
		
		//Setting up Base Driver Exporter
		this.queue = new RootDeviceExportingQueue();
		this.producerDeviceToExport = new RootDeviceListener(queue);
		producerDeviceToExport.activate();
		consumerDeviceToExport = new ThreadExporter(queue);
		new Thread(consumerDeviceToExport, "Exporter").start();

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
        
		//Setting up Base Driver Exporter
		consumerDeviceToExport.end();
		consumerDeviceToExport.cleanUp();
		producerDeviceToExport.deactive();

		//Setting up Base Driver Importer
		ctrl.stop();
		Activator.logger.close();
		Activator.logger=null;
		Activator.bc = null;
	}
}