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

package org.apache.felix.upnp.basedriver.importer.core.event.thread;


import org.cybergarage.upnp.Service;

import org.osgi.service.log.LogService;

import org.apache.felix.upnp.basedriver.Activator;
import org.apache.felix.upnp.basedriver.importer.core.MyCtrlPoint;
import org.apache.felix.upnp.basedriver.importer.core.event.message.FirstMessage;
import org.apache.felix.upnp.basedriver.importer.core.event.message.ListenerModified;
import org.apache.felix.upnp.basedriver.importer.core.event.message.ListenerUnRegistration;
import org.apache.felix.upnp.basedriver.importer.core.event.message.SidExipired;
import org.apache.felix.upnp.basedriver.importer.core.event.structs.Monitor;
import org.apache.felix.upnp.basedriver.importer.core.event.structs.SubscriptionQueue;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class SubScriber extends Thread {

	private MyCtrlPoint ctrl;
	private SubscriptionQueue subQueue;
    private boolean running = true;
	private Monitor monitor;

    
    public SubScriber(MyCtrlPoint ctrl, SubscriptionQueue subQueue, Monitor monitor) {
		super("upnp.basedriver.Subscriber");
		this.ctrl = ctrl;
		this.subQueue = subQueue;
		this.monitor=monitor;
		
	}

	public void run() {
		while (running) {
			Object msg = subQueue.dequeue();
			if (running) {
				if (msg instanceof FirstMessage) {
					FirstMessage firstmsg = (FirstMessage) msg;
					Service service = firstmsg.getService();
					if (!service.isSubscribed()) {//is not subscribe
						boolean ok = ctrl.subscribe(service,120000);
						String sid = "";
						if (ok) {//subcribe ok	                        
							sid = service.getSID();
							firstmsg.setSid(sid);                        
							monitor.addListener(sid,firstmsg.getListener());						
						} else {//subscribe not ok
							Activator.logger.log(LogService.LOG_ERROR,"Sucribe failed");
						}
					} else {// already subscribe
						monitor.addListener(service.getSID(),firstmsg.getListener());
					}
				} else if (msg instanceof ListenerModified) {
					monitor.updateListener((ListenerModified)msg,subQueue,ctrl);
				} else if (msg instanceof ListenerUnRegistration) {
					ListenerUnRegistration unreg=(ListenerUnRegistration)msg;
					monitor.delListener(unreg.getListener(),ctrl);
				} else if(msg instanceof SidExipired){
				    Activator.logger.WARNING("[Importer] Please report the bug. Used code - should be checked and removed");
				}
			}
		}
	}

	public void close() {
		running  = false;	
		subQueue.close();
	}

}
