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

package org.apache.felix.upnp.basedriver.export;

import java.util.Vector;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class RootDeviceExportingQueue {
	
    private Vector v;
    private boolean waiting;
    
    public RootDeviceExportingQueue(){
        v = new Vector();
        waiting=false;
    }
    
    public synchronized void addRootDevice(DeviceNode dev){
        v.addElement(dev);
        if(waiting) notify();
    }
    
    public synchronized DeviceNode getRootDevice(){
        while(v.isEmpty()){
            waiting=true;
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        DeviceNode dev = (DeviceNode) v.firstElement();
        v.remove(0);
        return dev;
    }
    /**
     * 
     * @param udn <code>String</code> rappresentaing the UDN of the device to remove 
     * @return a root <code>DeviceNode</code> that have UDN equals to the passed
     * 		 udn <code>String</code> or contain a device with the spcified UDN
     */
    public synchronized DeviceNode removeRootDevice(String udn){
        for (int i = 0; i < v.size(); i++) {
        	DeviceNode aux=(DeviceNode) v.elementAt(i);
            if(aux.contains(udn)){
                v.remove(i);
                return aux;
            }
        }
        return null;
    }
}
