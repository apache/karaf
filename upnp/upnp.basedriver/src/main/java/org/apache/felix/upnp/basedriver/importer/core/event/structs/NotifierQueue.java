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

package org.apache.felix.upnp.basedriver.importer.core.event.structs;

import java.util.Vector;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class NotifierQueue {
	private Vector queue;
	private boolean running = true;
	
	public NotifierQueue(){
		queue=new Vector();
	}
	
	public synchronized void enqueue(Object obj){
		queue.add(obj);
		if(queue.size() == 1){
			notify();
		}
		
	}
	
	public synchronized Object dequeue(){
		while(queue.size()==0 && running){
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (running)
			return queue.remove(0);
		else
			return null;
	}

	public synchronized void close() {
		running  = false;
		notify();		
	}

}
