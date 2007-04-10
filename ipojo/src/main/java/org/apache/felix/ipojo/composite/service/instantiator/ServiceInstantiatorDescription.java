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
package org.apache.felix.ipojo.composite.service.instantiator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.osgi.framework.ServiceReference;

/**
 * Description of the Service Instantiator Handler.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ServiceInstantiatorDescription extends HandlerDescription {
	
	/**
	 * List of managed service instances.
	 */
	private List m_instances;

	/**
	 * Constructor.
	 * @param arg0 : name of the handler
	 * @param arg1 : validity of the handler
	 * @param insts : list of service instance
	 */
	public ServiceInstantiatorDescription(String arg0, boolean arg1, List insts) {
		super(arg0, arg1);
		m_instances = insts;
	}
	
	/**
	 * @see org.apache.felix.ipojo.architecture.HandlerDescription#getHandlerInfo()
	 */
	public String getHandlerInfo() {
		String r = "";
		for (int i = 0; i < m_instances.size(); i++) {
			SvcInstance inst = (SvcInstance) m_instances.get(i);
			HashMap map = inst.getUsedReferences();
			Set keys = map.keySet();
			Iterator it = keys.iterator();
			while (it.hasNext()) {
				ServiceReference ref = (ServiceReference) it.next();
				Object o = map.get(ref);
				if (o != null  && o instanceof ComponentInstance) {
					r += "\t Specification " + inst.getSpecification() + " instantiated from " + ((ComponentInstance) o).getComponentDescription().getName() + " \n";
				}
			}
		}
		return r;
	}

}
