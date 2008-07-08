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

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPStateVariable;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/

public class GetTimeAction implements UPnPAction {

	final private String NAME = "GetTime";
	final private String RESULT_STATUS = "CurrentTime";
	final private String[] OUT_ARG_NAMES = new String[]{RESULT_STATUS};
	private TimeStateVariable time;
	
	
	public GetTimeAction(TimeStateVariable time){
		this.time = time;
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
		return null;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPAction#getInputArgumentNames()
	 */
	public String[] getInputArgumentNames() {
		
		return null;
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
		return time;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPAction#invoke(java.util.Dictionary)
	 */
	public Dictionary invoke(Dictionary args) throws Exception {
		String value = time.getCurrentTime();
		Hashtable result = new Hashtable();
		result.put(RESULT_STATUS,value);
		return result;
	}
}
