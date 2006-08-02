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

package org.apache.felix.upnp.basedriver.export;



import java.util.Dictionary;
import java.util.Enumeration;

import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.Service;
import org.cybergarage.upnp.StateVariable;

import org.osgi.service.upnp.UPnPEventListener;

import org.apache.felix.upnp.extra.util.Converter;

/* 
* @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
*/
public class ExporterUPnPEventListener implements UPnPEventListener {

	private Device d;
	
	public ExporterUPnPEventListener(Device d){
		this.d=d;
	}
		
	/**
	 * @see org.osgi.service.upnp.UPnPEventListener#notifyUPnPEvent(java.lang.String, java.lang.String, java.util.Dictionary)
	 */
	public void notifyUPnPEvent(String deviceId, String serviceId,Dictionary events) {
		Device dAux = null;
		if(d.getUDN().equals(deviceId)){
			dAux=d;
		}else{
			dAux= d.getDevice(deviceId);
		}
		Service s = dAux.getService(serviceId);
		// fix 2/9/2004 francesco 
		Enumeration e = events.keys();
		StateVariable sv;
		while (e.hasMoreElements()) {
			String name = (String) e.nextElement();
			sv=s.getStateVariable(name);
			//sv.setValue((String) events.get(name));
			try {
				sv.setValue(Converter.toString(events.get(name),sv.getDataType()));
			} catch (Exception ignored) {
			}
		}
	}
}
