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

package org.apache.felix.upnp.sample.clock;

import java.beans.PropertyChangeEvent;
import java.util.Dictionary;

import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPStateVariable;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/

public class SetTimeAction implements UPnPAction {

	final private String NAME = "SetTime";
	final private String NEW_TIME_VALUE = "NewTime";
	final private String NEW_RESULT_VALUE = "Result";
	final private String[] IN_ARG_NAMES = new String[]{NEW_TIME_VALUE};
	final private String[] OUT_ARG_NAMES = new String[]{NEW_RESULT_VALUE};
	private UPnPStateVariable time,result;
	
	
	public SetTimeAction(UPnPStateVariable time,UPnPStateVariable result){
		this.time = time;
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
		if (argumentName.equals("NewTime")) return time;
		else if (argumentName.equals("Result")) return result;
		else return null;
	}

	/* (non-Javadoc)
	 * 
	 * @see org.osgi.service.upnp.UPnPAction#invoke(java.util.Dictionary)
	 */
	public Dictionary invoke(Dictionary args) throws Exception {
		Long newValue = (Long) args.get(NEW_TIME_VALUE);
        Long oldValue = (Long) ((TimeStateVariable) time).getCurrentValue();
		((TimeStateVariable) time).setCurrentTime(newValue.longValue());
        ClockDevice.notifier.propertyChange(new PropertyChangeEvent(time,"Time",oldValue,newValue));        
		args.remove(NEW_TIME_VALUE);
		args.put(NEW_RESULT_VALUE,((TimeStateVariable) time).getCurrentTime());
		return args;
	}
}
