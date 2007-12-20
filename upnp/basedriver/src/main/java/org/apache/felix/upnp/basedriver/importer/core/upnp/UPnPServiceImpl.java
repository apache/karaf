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

package org.apache.felix.upnp.basedriver.importer.core.upnp;


import java.util.Enumeration;
import java.util.Hashtable;

import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.ActionList;
import org.cybergarage.upnp.Service;
import org.cybergarage.upnp.ServiceStateTable;
import org.cybergarage.upnp.StateVariable;

import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPService;
import org.osgi.service.upnp.UPnPStateVariable;

/** 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class UPnPServiceImpl implements UPnPService {
	private Service service;
	private Hashtable actions;
	private Hashtable stateVariables;
	
	public UPnPServiceImpl(Service service) {
		this.service = service;
		actions = new Hashtable();
		stateVariables=new Hashtable();
		/*
		 * action
		 */
		ActionList actionlist = service.getActionList();
		for (int i = 0; i < actionlist.size(); i++) {
			Action act = actionlist.getAction(i);
			actions.put(act.getName(), new UPnPActionImpl(act,this));
		}
		/*StateVariable*/
		ServiceStateTable stateTable=service.getServiceStateTable();
		for(int i=0;i<stateTable.size();i++){
			StateVariable var= stateTable.getStateVariable(i);
			stateVariables.put(var.getName(),new UPnPStateVariableImpl(var));
		}
	
	
	} /*
	   * (non-Javadoc)
	   * 
	   * @see org.osgi.service.upnp.UPnPService#getId()
	   */
	public String getId() {
		// TODO Auto-generated method stub
		return service.getServiceID();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.upnp.UPnPService#getType()
	 */
	public String getType() {
		// TODO Auto-generated method stub
		return service.getServiceType();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.upnp.UPnPService#getVersion()
	 */
	public String getVersion() {
		String serviceType = service.getServiceType();
		int start = serviceType.lastIndexOf(':');
		String version = serviceType.substring(start+1);
		return version;
	} 
	/*
	   * (non-Javadoc)
	   * 
	   * @see org.osgi.service.upnp.UPnPService#getAction(java.lang.String)
	   */
	public UPnPAction getAction(String name) {
		//TODO to check
		return (UPnPAction) actions.get(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.upnp.UPnPService#getActions()
	 */
	public UPnPAction[] getActions() {
		//TODO check again
		Enumeration e=actions.elements();
		if(e==null){
			return null;
		}
		UPnPAction [] uPnPacts=new UPnPAction[actions.size()];
		int i=0;
		while(e.hasMoreElements()){
			uPnPacts[i]=(UPnPActionImpl)e.nextElement();
			i++;
		}
		return uPnPacts;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.upnp.UPnPService#getStateVariables()
	 */
	public UPnPStateVariable[] getStateVariables() {
		//TODO check again
		UPnPStateVariableImpl [] vars =new UPnPStateVariableImpl[stateVariables.size()];
		Enumeration e=stateVariables.elements();
		if(e==null){
			return null;
		}
		int i=0;
		while(e.hasMoreElements()){
			vars[i]=(UPnPStateVariableImpl)e.nextElement();
			i++;
		}
		return vars;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.upnp.UPnPService#getStateVariable(java.lang.String)
	 */
	public UPnPStateVariable getStateVariable(String name) {
		//TODO chack again
		return (UPnPStateVariableImpl) stateVariables.get(name);
	}

	public Service getCyberService(){
		return service;
	}

}
