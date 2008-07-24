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

import java.util.Properties;

import org.apache.felix.ipojo.test.scenarios.service.dependency.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.service.dependency.service.FooService;
import org.osgi.framework.ServiceReference;

public class DynCheckServiceProvider extends CheckProviderParentClass implements CheckService {
    
	FooService fs;
	
	int simpleB = 0;
	int objectB = 0;
	int refB = 0;
	int bothB = 0;

	public boolean check() {
		return fs.foo();
	}

	public Properties getProps() {
		Properties props = new Properties();
		props.put("voidB", new Integer(simpleB));
		props.put("objectB", new Integer(objectB));
		props.put("refB", new Integer(refB));
		props.put("bothB", new Integer(bothB));
		props.put("voidU", new Integer(simpleU));
		props.put("objectU", new Integer(objectU));
		props.put("refU", new Integer(refU));
		props.put("bothU", new Integer(bothU));
		if (fs != null) {
		    props.put("int", new Integer(fs.getInt()));
		    if(fs.getObject() != null) { props.put("object", fs.getObject()); }
		}
        props.put("static", CheckService.foo);
        props.put("class", CheckService.class.getName());
		return props;
	}
	
	private void voidBind() {
		simpleB++;
	}
	
	protected void objectBind(FooService o) {
	    if (o == null) {
	        System.err.println("Bind receive null !!! ");
	        return;
	    }
		if(o != null && o instanceof FooService) { objectB++; }
	}
	
	public void refBind(ServiceReference sr) {
		if(sr != null) { refB++; }
	}
	
    public void bothBind(FooService o, ServiceReference sr) {
	    if(sr != null && o != null && o instanceof FooService) { bothB++; }
	}

}
