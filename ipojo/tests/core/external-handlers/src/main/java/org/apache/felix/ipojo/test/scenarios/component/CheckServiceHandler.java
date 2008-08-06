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
package org.apache.felix.ipojo.test.scenarios.component;

import java.util.Dictionary;
import java.util.Properties;

import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.test.scenarios.eh.service.CheckService;
import org.osgi.framework.ServiceRegistration;

public class CheckServiceHandler extends PrimitiveHandler implements CheckService {
	
	ServiceRegistration sr;
	boolean isValid;
	int changes = 0;
	static final String NAMESPACE = "org.apache.felix.ipojo.test.handler.checkservice";
	
	Properties props = new Properties();

	public void configure(Element metadata, Dictionary configuration) {
		Element[] meta = metadata.getElements("check", NAMESPACE);
		if(meta == null) { return;	}		
		// Get handler props 
		props.put("instance.name", configuration.get("name"));
		if(configuration.get("csh.simple") != null) { props.put("Simple", configuration.get("csh.simple")); }
		if(configuration.get("csh.map") != null) { 
			Dictionary m = (Dictionary) configuration.get("csh.map");
            if (m.size() > 0) {
                props.put("Map1", m.get("a"));
                props.put("Map2", m.get("b"));
                props.put("Map3", m.get("c"));
            }
		}
		props.put("changes", new Integer(changes));
		
	}
	
	public void initializeComponentFactory(ComponentTypeDescription cd, Element metadata) {
	    cd.addProperty(new PropertyDescription("csh.simple", "java.lang.String", null));
        cd.addProperty(new PropertyDescription("csh.map", "java.util.Dictionary", null));
	}
	
	public void start() {
		if(sr == null) {
			sr = getInstanceManager().getContext().registerService(CheckService.class.getName(), this, props);
		}
		isValid = true;
	}
	
	public void stop() {
		isValid = false;
		synchronized(this) {
			if(sr != null) { sr.unregister(); }
		}
	}
	
	public boolean check() {
		if(isValid) { isValid = false;}
		else { isValid = true; }
		return isValid;
	}

	public Properties getProps() {
		return props;
	}
	
	public void stateChanged(int state) {
		if (sr != null) {
		    changes++;
		    props.put("changes", new Integer(changes));
		    sr.setProperties(props);
		}
	}

	public String getName() {
		return NAMESPACE;
	}
	
	public HandlerDescription getDescription() {
		return new CheckServiceHandlerDescription(this);
	}

}
