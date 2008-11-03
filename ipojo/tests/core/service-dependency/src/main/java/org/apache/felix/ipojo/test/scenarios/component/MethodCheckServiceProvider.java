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
import java.util.Map;
import java.util.Properties;

import org.apache.felix.ipojo.test.scenarios.service.dependency.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.service.dependency.service.FooService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class MethodCheckServiceProvider implements CheckService {
	
	FooService fs;
    
    BundleContext context;
	
	int simpleB = 0;
	int objectB = 0;
	int refB = 0;
	int bothB = 0;
	int mapB = 0;
	int dictB = 0;
	int simpleU = 0;
	int objectU = 0;
	int refU = 0;
	int bothU = 0;
	int mapU = 0;
	int dictU = 0;
	
    
    public MethodCheckServiceProvider(BundleContext bc) {
        context = bc;
    }

	public boolean check() {
		return fs.foo();
	}

	public Properties getProps() {
		Properties props = new Properties();
        if(fs != null) {
            props.put("result", new Boolean(fs.foo()));
            props.put("boolean", new Boolean(fs.getBoolean()));
            props.put("int", new Integer(fs.getInt()));
            props.put("long", new Long(fs.getLong()));
            props.put("double", new Double(fs.getDouble()));
        } else {
            props.put("result", new Boolean(false));
        }
		props.put("voidB", new Integer(simpleB));
		props.put("objectB", new Integer(objectB));
		props.put("refB", new Integer(refB));
		props.put("bothB", new Integer(bothB));
	    props.put("mapB", new Integer(mapB));
	    props.put("dictB", new Integer(dictB));
		props.put("voidU", new Integer(simpleU));
		props.put("objectU", new Integer(objectU));
		props.put("refU", new Integer(refU));
		props.put("bothU", new Integer(bothU));
		props.put("mapU", new Integer(mapU));
		props.put("dictU", new Integer(dictU));
		
		if(fs != null) {
		    if(fs.getObject() != null) { props.put("object", fs.getObject()); }
        }
		
		return props;
	}
	
	protected void objectBind(FooService o) {
		if(o != null && o instanceof FooService) { objectB++; }
        fs = o;
	}
	protected void objectUnbind(FooService o) {
		if(o != null && o instanceof FooService) { objectU++; }
        fs = null;
	}
	
	public void refBind(ServiceReference sr) {
		if(sr != null) { refB++; }
        fs = (FooService) context.getService(sr);
	}
	public void refUnbind(ServiceReference sr) {
		if(sr != null) { refU++; }
        context.ungetService(sr);
        fs = null;
	}
	
    protected void bothBind(FooService o, ServiceReference ref) {
	    if(ref != null && o != null && o instanceof FooService) { bothB++; }
	    fs = o;
	}	
    protected void bothUnbind(FooService o, ServiceReference ref) {
	     if(ref != null && o != null && o instanceof FooService) { bothU++; }
	     fs = null;
	}
    
    protected void propertiesMapBind(FooService o, Map props) {
        if(props != null && o != null && o instanceof FooService && props.size() > 0) { mapB++; }
        fs = o;
    }   
    protected void propertiesMapUnbind(FooService o, Map props) {
         if(props != null && o != null && o instanceof FooService && props.size() > 0) { mapU++; }
         fs = null;
    }
    
    protected void propertiesDictionaryBind(FooService o, Dictionary props) {
        if(props != null && o != null && o instanceof FooService && props.size() > 0) { dictB++; }
        fs = o;
    }   
    protected void propertiesDictionaryUnbind(FooService o, Dictionary props) {
         if(props != null && o != null && o instanceof FooService && props.size() > 0) { dictU++; }
         fs = null;
    }
}
