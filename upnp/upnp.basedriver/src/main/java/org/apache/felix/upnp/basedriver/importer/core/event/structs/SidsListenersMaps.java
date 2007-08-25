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

import java.util.Dictionary;
import java.util.Vector;

import org.osgi.service.upnp.UPnPEventListener;

/**
 * This class contain two table:
 *  - sid2listener: have SID of Suscribed Service as key and a Vector of UPnPEventListener as value
 *  - listener2sids: have an UPnPEventListener as kay and a Vector of SID of Scriscrobed Service 
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class SidsListenersMaps {
	private Sid2Listeners sid2Listeners;
	private Listener2Sids listeners2Sids;
	
	public SidsListenersMaps(){
		sid2Listeners=new Sid2Listeners();
		listeners2Sids=new Listener2Sids();
	}
    
    
	/////////////////// Sid to Listeners //////////////////////////////////
	public final void putSid2Listeners(String sid, UPnPEventListener listener) {
		sid2Listeners.put(sid,listener);
	}
    public final Vector getListenersFromSid(String sid) {
        return sid2Listeners.get(sid);
    }
    public final void removeSidKey(String sid) {
        sid2Listeners.remove(sid);
    }
	public final boolean updateListeners(String sid, String deviceID, String serviceID,Dictionary dictionary){
		return sid2Listeners.updateListeners(sid,deviceID,serviceID,dictionary);
	}
	public final boolean getAlreadyFirst(String sid) {
		return sid2Listeners.getAlreadyFirst(sid);
	}
	public final void setAlreadyFirst(String sid, boolean bool) {
		sid2Listeners.setAlreadyFirst(sid,bool);
	}
    
    
    
    /////////////////// Listener to Sids //////////////////////////////////
	public final void putListener2Sids(UPnPEventListener listener,String sid) {
		listeners2Sids.put(listener,sid);
	}
	public final Vector getSidsFromListener(UPnPEventListener listener){
		return listeners2Sids.get(listener);
	}
	public final void removeListenerKey(UPnPEventListener listener){
		listeners2Sids.remove(listener);
	}
}
