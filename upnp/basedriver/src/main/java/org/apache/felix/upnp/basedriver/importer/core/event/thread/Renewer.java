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

import org.apache.felix.upnp.basedriver.importer.core.MyCtrlPoint;
import org.apache.felix.upnp.basedriver.importer.core.event.message.SidExipired;
import org.apache.felix.upnp.basedriver.importer.core.event.structs.SubscriptionQueue;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class Renewer extends Thread {
	private long timeout;
	private String sid;
	private Service service;
	private MyCtrlPoint ctrl;
	private boolean bool;
	private SubscriptionQueue subqueue;
	private final long time_before_renew=60000;
	/**
	 * *
	 * 
	 * @param timeout
	 * @param sid
	 * @param service
	 */
    public Renewer(long timeout, String sid, Service service,
            MyCtrlPoint ctrl,SubscriptionQueue subqueue) {
		super("ReNewal" + sid);
		if (timeout - time_before_renew > 0) {
			this.timeout = timeout - time_before_renew;
		} else {
			this.timeout = timeout;
		}
		this.sid = sid;
		this.service = service;
		this.ctrl = ctrl;
		bool = true;
		this.subqueue=subqueue;
	}
	public void run() {
		while (bool) {
			try {
				sleep(timeout);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			boolean ok = ctrl.subscribe(service, sid, 180000);
			if (ok) {//renew ok
				if (service.getTimeout() - time_before_renew > 0) {
					timeout = service.getTimeout() - time_before_renew;
				} else {
					timeout = service.getTimeout();
				}
			} else {//renew not ok
				bool=false;
				subqueue.enqueue(new SidExipired(sid,service));
			}
		}
	}

}
