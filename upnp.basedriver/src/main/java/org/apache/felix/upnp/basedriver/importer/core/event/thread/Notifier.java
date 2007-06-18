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

import org.apache.felix.upnp.basedriver.importer.core.event.message.StateChanged;
import org.apache.felix.upnp.basedriver.importer.core.event.structs.Monitor;
import org.apache.felix.upnp.basedriver.importer.core.event.structs.NotifierQueue;
import org.apache.felix.upnp.basedriver.importer.core.event.structs.StateVarsToNotify;


/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/

public class Notifier extends Thread {

	private NotifierQueue notifierQueue;
	private Monitor monitor;
	private boolean running = true;

    public Notifier(NotifierQueue notifierQueue,Monitor monitor) {
		super("upnp.basedriver.Notifier");
		this.notifierQueue = notifierQueue;
		this.monitor = monitor;
		
	}

	public void run() {
		while (running) {
            StateChanged msg = (StateChanged) notifierQueue.dequeue();
            if (running) {
				StateVarsToNotify vars = null;
				if (msg.getSeq() == 0) {
					vars = new StateVarsToNotify(msg);
					monitor.putStateVars(msg.getSid(),vars);
				} else {
					monitor.updateStateVars(msg.getSid(),msg.getDictionary());
				}
            }
		}
	}

	public void close() {
		running  = false;
		notifierQueue.close();
	}
}
