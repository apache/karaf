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

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPStateVariable;

public class SetPowerAction implements UPnPAction {

	final private String NAME = "SetPower";
	final private String NEW_TIME_VALUE = "Power";
	final private String NEW_RESULT_VALUE = "Result";
	final private String[] IN_ARG_NAMES = new String[]{NEW_TIME_VALUE};
	final private String[] OUT_ARG_NAMES = new String[]{NEW_RESULT_VALUE};
	private PowerStateVariable power;
	private ResultStateVariable result;
	
	
	public SetPowerAction(PowerStateVariable power,ResultStateVariable result){
		this.power = power;
		this.result=result;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPAction#getName()
	 */
	public String getName() {
		return NAME;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPAction#getReturnArgumentName()
	 */
	public String getReturnArgumentName() {
		return "Result";
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPAction#getInputArgumentNames()
	 */
	public String[] getInputArgumentNames() {
		return IN_ARG_NAMES;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPAction#getOutputArgumentNames()
	 */
	public String[] getOutputArgumentNames() {
		return OUT_ARG_NAMES;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPAction#getStateVariable(java.lang.String)
	 */
	public UPnPStateVariable getStateVariable(String argumentName) {
		if (argumentName.equals("Power")) return power;
		else if (argumentName.equals("Result")) return result;
		else return null;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPAction#invoke(java.util.Dictionary)
	 */
	public Dictionary invoke(Dictionary args) throws Exception {
		Boolean value = (Boolean) args.get(NEW_TIME_VALUE);
		power.setPower(value);
		Hashtable result = new Hashtable();
		result.put(NEW_RESULT_VALUE,Boolean.TRUE);
		return result;
	}
}
