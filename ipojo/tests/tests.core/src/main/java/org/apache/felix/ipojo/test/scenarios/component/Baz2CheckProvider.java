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

import org.apache.felix.ipojo.test.scenarios.service.BazService;
import org.apache.felix.ipojo.test.scenarios.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.service.FooService;
import org.osgi.framework.ServiceReference;

public class Baz2CheckProvider implements CheckService {
	
	BazService fs;
	
	int simpleB = 0;
	int objectB = 0;
	int refB = 0;
	int simpleU = 0;
	int objectU = 0;
	int refU = 0;

	public boolean check() {
		return fs.foo();
	}

	public Properties getProps() {
		Properties props = new Properties();
		props.put("result", new Boolean(fs.foo()));
		props.put("voidB", new Integer(simpleB));
		props.put("objectB", new Integer(objectB));
		props.put("refB", new Integer(refB));
		props.put("voidU", new Integer(simpleU));
		props.put("objectU", new Integer(objectU));
		props.put("refU", new Integer(refU));
		props.put("boolean", new Boolean(fs.getBoolean()));
		props.put("int", new Integer(fs.getInt()));
		props.put("long", new Long(fs.getLong()));
		props.put("double", new Double(fs.getDouble()));
		if(fs.getObject() != null) { props.put("object", fs.getObject()); }
		
		return props;
	}
	
	private void voidBind() {
		simpleB++;
	}
	private void voidUnbind() {
		simpleU++;
	}
	
	protected void objectBind(Object o) {
		if(o != null && o instanceof FooService) { objectB++; }
	}
	protected void objectUnbind(Object o) {
		if(o != null && o instanceof FooService) { objectU++; }
	}
	
	public void refBind(ServiceReference sr) {
		if(sr != null) { refB++; }
	}
	public void refUnbind(ServiceReference sr) {
		if(sr != null) { refU++; }
	}
}
