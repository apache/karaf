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
import java.util.Hashtable;
import java.util.Vector;

import org.osgi.service.upnp.UPnPEventListener;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class Sid2Listeners {
	private Hashtable sidListener;
	private Hashtable alreadyfirst;
    
	public Sid2Listeners() {
		this.sidListener = new Hashtable();
		this.alreadyfirst = new Hashtable();
	}
    
	public void put(String sid, UPnPEventListener listener) {
		if (!sidListener.containsKey(sid)) {
			Vector vec = new Vector();
			vec.add(listener);
			sidListener.put(sid, vec);
		} else {
			Vector vec = (Vector) sidListener.get(sid);
			if (!vec.contains(listener)) {
				vec.add(listener);
			}
		}
	}

	public final void remove(String sid) {
		sidListener.remove(sid);
	}

	public final Vector get(String sid) {
		return ((Vector) sidListener.get(sid));
	}

    /**
	 * @param sid
	 * @param dictionary
	 */
	public boolean updateListeners(String sid, String deviceID,String serviceID, Dictionary dictionary) {
        
		Vector listeners = (Vector) sidListener.get(sid);
		if (listeners != null) {
			for (int i = 0; i < listeners.size(); i++) {
				UPnPEventListener listener = (UPnPEventListener) listeners.elementAt(i);
				listener.notifyUPnPEvent(deviceID, serviceID, dictionary);
			}
			return true;
		}
		return false;
	}
    
	public boolean getAlreadyFirst(String sid) {
		return ((Boolean) alreadyfirst.get(sid)).booleanValue();
	}

    public void setAlreadyFirst(String sid, boolean bool) {
		alreadyfirst.put(sid, new Boolean(bool));
	}
}
