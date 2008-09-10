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

package org.apache.felix.upnp.sample.binaryLight.services;

import java.util.HashMap;

import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPService;
import org.osgi.service.upnp.UPnPStateVariable;

import org.apache.felix.upnp.sample.binaryLight.LightModel;
import org.apache.felix.upnp.sample.binaryLight.actions.GetStatusAction;
import org.apache.felix.upnp.sample.binaryLight.actions.GetTargetAction;
import org.apache.felix.upnp.sample.binaryLight.actions.SetTargetAction;
import org.apache.felix.upnp.sample.binaryLight.statevariables.StatusStateVariable;
import org.apache.felix.upnp.sample.binaryLight.statevariables.TargetStateVariable;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/

public class PowerSwitchService implements UPnPService{
	
	final static private String SERVICE_ID = "urn:upnp-org:serviceId:SwitchPower:1";
	final static private String SERVICE_TYPE = "urn:schemas-upnp-org:service:SwitchPower:"
		+ PowerSwitchService.VERSION;
	final static private String VERSION = "1";

	private LightModel model;
	private UPnPStateVariable status,target;
	private UPnPStateVariable[] states;
	private HashMap actions = new HashMap();
	
	
	public PowerSwitchService(LightModel model){
		this.model = model;
		status = new StatusStateVariable(model);
		target = new TargetStateVariable();
		this.states = new UPnPStateVariable[]{status,target};
		
		UPnPAction setTarget = new SetTargetAction(model,target);
		UPnPAction getTarget = new GetTargetAction(model,target);
		UPnPAction getStatus = new GetStatusAction(model,status);
		actions.put(setTarget.getName(),setTarget);
		actions.put(getTarget.getName(),getTarget);
		actions.put(getStatus.getName(),getStatus);
		
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPService#getId()
	 */
	public String getId() {
		return SERVICE_ID;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPService#getType()
	 */
	public String getType() {
		return SERVICE_TYPE;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPService#getVersion()
	 */
	public String getVersion() {
		return VERSION;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPService#getAction(java.lang.String)
	 */
	public UPnPAction getAction(String name) {
		return (UPnPAction)actions.get(name);
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPService#getActions()
	 */
	public UPnPAction[] getActions() {
		return (UPnPAction[])(actions.values()).toArray(new UPnPAction[]{});
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPService#getStateVariables()
	 */
	public UPnPStateVariable[] getStateVariables() {
		return states;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPService#getStateVariable(java.lang.String)
	 */
	public UPnPStateVariable getStateVariable(String name) {
		if (name.equals("Status"))
			return status;
		else if (name.equals("Target"))
			return target;
		else return null;
	}
}
