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
import java.util.Iterator;
import java.util.Vector;

import org.cybergarage.upnp.Service;

import org.osgi.service.upnp.UPnPEventListener;

import org.apache.felix.upnp.basedriver.importer.core.MyCtrlPoint;
import org.apache.felix.upnp.basedriver.importer.core.event.message.FirstMessage;
import org.apache.felix.upnp.basedriver.importer.core.event.message.ListenerModified;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/

public class Monitor {
	private Hashtable sidStateVars;
	private SidsListenersMaps sidListSid;

	public Monitor() {
		this.sidListSid = new SidsListenersMaps();
		this.sidStateVars = new Hashtable();
	}

	public synchronized void putStateVars(String sid, StateVarsToNotify vars) {
		sidStateVars.put(sid, vars);
		Vector listeners = (Vector) sidListSid.getListenersFromSid(sid);
		if (listeners != null) {
			sidListSid.updateListeners(sid, vars.getDeviceID(), vars.getServiceID(), vars.getDictionary());
			sidListSid.setAlreadyFirst(sid,true);
		} else {
			sidListSid.setAlreadyFirst(sid, false);
		}
	}
	public synchronized void updateStateVars(String sid, Dictionary dic) {
		StateVarsToNotify vars = (StateVarsToNotify) sidStateVars.get(sid);
		if (vars != null) {
			vars.updateDic(dic);
			if (sidListSid.getAlreadyFirst(sid)) {
			    /*
			     * Sends only the changed StateVariable
			     */
				sidListSid.updateListeners(sid, 
				        vars.getDeviceID(), vars.getServiceID(), 
				        dic);
			} else {
			    /*
			     * Sends the sholw StateVariable for the service 
			     */
				boolean bool = sidListSid.updateListeners(sid, 
				        vars.getDeviceID(), vars.getServiceID(), 
				        vars.getDictionary());
				if (bool) {
					sidListSid.setAlreadyFirst(sid,true);
				}
			}
		}
	}
    
/*    
    public synchronized StateVarsToNotify getStateVars(String sid) {
        return (StateVarsToNotify) sidStateVars.get(sid);
    }
    public synchronized void removeStateVars(String sid) {
        sidStateVars.remove(sid);
    }
*/
	public synchronized void addListener(String sid, UPnPEventListener listener) {
		StateVarsToNotify vars = (StateVarsToNotify) sidStateVars.get(sid);
		if (vars != null) {
		    /*
		     * Notify the listener whit the whole StateVariables and then
		     * the next time you send only the changed StateVariables
		     */
            listener.notifyUPnPEvent(vars.getDeviceID(), 
                    vars.getServiceID(),vars.getDictionary());
        }
		sidListSid.putSid2Listeners(sid, listener);
		sidListSid.putListener2Sids(listener, sid);
	}

	/**
	 * Delete the reference to the listener from the stuctures sid2Listeners and listener2Sids.
	 * Also if no more listner are listening for a UPnP Service that UPnP Service is unscribed.
	 * 
	 * @param listener The listener to delete
	 * @param ctrl Needed for reference
	 */
	public synchronized void delListener(UPnPEventListener listener,
			MyCtrlPoint ctrl/*##renew, SidRenewer sidRenewer*/) {
        
        //francesco-renew
        // State variable clean up -- todo?
        
		Vector sids = sidListSid.getSidsFromListener(listener);
		if (sids != null) {
		    Iterator i = sids.iterator();
			while(i.hasNext()){
			    String sid = (String) i.next();
				Vector listeners = 
                    sidListSid.getListenersFromSid(sid);
				listeners.remove(listener);
				if (listeners.size() == 0) {
					Service service = 
                        ctrl.serviceFromSid(sid);
					//##renew  Renewer renewer = sidRenewer.get((String) sids.elementAt(i));
					//##renew  renewer.stop();
					if (service != null) {
						boolean ok = ctrl.unsubscribe(service);
						if (!ok) {
						    //TODO Log?s
							service.clearSID();
						}
					}
					sidListSid.setAlreadyFirst(sid,false);
					sidStateVars.remove(sid);
					i.remove();
				}
			}
			sidListSid.removeListenerKey(listener);			
		}
	}

	public synchronized void updateListener(ListenerModified msg,
			SubscriptionQueue subqueue, MyCtrlPoint ctrl/*##renew, SidRenewer sidRenewer*/) {
		UPnPEventListener listener = msg.getListener();
		Vector newServices = msg.getNewServices();
		Vector subscribed = new Vector();
		Vector notSubscribed = new Vector();
		
		for (int i = 0; i < newServices.size(); i++) {
			Service ser = (Service) newServices.elementAt(i);
			if (ser.isSubscribed()) {
				subscribed.add(ser);
			} else {
				notSubscribed.add(ser);
			}
		}
		

		Vector oldSids = sidListSid.getSidsFromListener(listener);
        // francesco-renew
        // check subscribed services
		if(oldSids==null) return;
		
		for (int i = 0; i < notSubscribed.size(); i++) {
			Service ser = (Service) notSubscribed.elementAt(i);
			subqueue.enqueue(new FirstMessage(ser, listener));
		}
		
		for (int i = 0; i < oldSids.size(); i++) {
			String oldSid = (String) oldSids.elementAt(i);
			if (!subscribed.contains(oldSid)) {
                // francesco-renew
                // to check -- Listner removal from sid2Listener
                				
				unsubscribeListenerForSid(oldSid,listener,ctrl);
			}
		}

	}
    
	/**
	 * Unregister the listener as UPnPListener for the UPnPService with
	 * the spicfied SID
	 * 
     * @param sid
     * @param listener
     * @param ctrl
     */
    private void unsubscribeListenerForSid(String sid, UPnPEventListener listener, MyCtrlPoint ctrl) {
        Vector listeners = sidListSid.getListenersFromSid(sid);
        listeners.remove(listener);
        if(listeners.size()==0){
			Service service = ctrl.serviceFromSid(sid);
			if (service != null) {
				boolean ok = ctrl.unsubscribe(service);
				if (!ok) {
				    //TODO Log?
					service.clearSID();
				}
			}
			sidListSid.setAlreadyFirst(sid,false);
			sidStateVars.remove(sid);
		}
        Vector sids = sidListSid.getSidsFromListener(listener);
        sids.remove(sid);
        if(sids.size()==0){
            sidListSid.removeListenerKey(listener);
        }
    }

    public synchronized void delSid(String sid) {
		Vector listeners = sidListSid.getListenersFromSid(sid);
		if(listeners==null)return;
		for (int i = 0; i < listeners.size(); i++) {
			Vector sids = sidListSid.getSidsFromListener((UPnPEventListener) listeners
					.elementAt(i));
			sids.remove(sid);
		}
		sidListSid.removeSidKey(sid);
	}

	public synchronized void clearAll(String sid, Service service) {
		service.clearSID();
		delSid(sid);
		sidStateVars.remove(sid);
	}

}
