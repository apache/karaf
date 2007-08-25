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

import java.util.Hashtable;
import java.util.Vector;

import org.osgi.service.upnp.UPnPEventListener;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class Listener2Sids {
	private Hashtable listenerSid;
    
	public Listener2Sids() {
		this.listenerSid = new Hashtable();
	}
    
	public void put(UPnPEventListener listener, String sid) {
		if (!listenerSid.containsKey(listener)) {
			Vector vec = new Vector();
			vec.add(sid);
			listenerSid.put(listener, vec);
		} else {
			Vector vec = (Vector) listenerSid.get(listener);
			if (!vec.contains(sid)) {
				vec.add(sid);
			}
		}
	}
    
	public final void remove(UPnPEventListener listener) {
		listenerSid.remove(listener);
	}

	public final Vector get(UPnPEventListener listener) {
		return ((Vector) listenerSid.get(listener));
	}
}

