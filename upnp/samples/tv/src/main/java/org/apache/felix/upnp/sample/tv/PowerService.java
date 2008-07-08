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

package org.apache.felix.upnp.sample.tv;

import java.util.HashMap;

import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPService;
import org.osgi.service.upnp.UPnPStateVariable;

public class PowerService implements UPnPService {
	final private String SERVICE_ID = "urn:schemas-upnp-org:serviceId:power:1";
	final private String SERVICE_TYPE = "urn:schemas-upnp-org:service:power:1";
	final private String VERSION ="1";

	private PowerStateVariable power;
	private ResultStateVariable result;
	private UPnPStateVariable[] states;
	private HashMap actions = new HashMap();
	
	
	public PowerService(){
		power = new PowerStateVariable();
		result = new ResultStateVariable();
		this.states = new UPnPStateVariable[]{power,result};
		
		UPnPAction setPower= new SetPowerAction(power,result);
		UPnPAction getPower = new GetPowerAction(power);
		actions.put(setPower.getName(),setPower);
		actions.put(getPower.getName(),getPower);
		
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
		if (name.equals("Power"))
			return power;
		else if (name.equals("Result"))
			return result;
		else return null;
	}
}
