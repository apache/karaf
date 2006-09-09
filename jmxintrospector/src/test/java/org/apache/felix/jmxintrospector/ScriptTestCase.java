/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.felix.jmxintrospector;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import junit.framework.TestCase;

public class ScriptTestCase extends IntrospectorTestHarness {
	Logger logger=Logger.getLogger(this.getClass().getName());
	ScriptEngineManager engineManager;
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		engineManager=new ScriptEngineManager();
		engineManager.put("manager", proxyManager);
	}
	public void testRuby() throws Exception{
		fail("Not implemented");
	}
	public void testJavascript() throws Exception{
		ScriptEngine js=engineManager.getEngineByName("javascript");
		//results are the same, but may have different hash
		List<Object> objectsInJava=proxyManager.getObjects();
		List<Object> objectsInJS=(List<Object>) js.eval("manager.getObjects()");
		for (Object objectJS : objectsInJS) {
			Object javaPeer=null;
			for (Object objectJava : objectsInJava) {
				if (((MBean)objectJava).getObjectName().equals(((MBean)objectJS).getObjectName()))
				javaPeer=objectJava;
			}
			assertNotNull(javaPeer);
			System.out.println(javaPeer+";"+ objectJS);
			assertEquals(javaPeer.getClass(), objectJS.getClass());
			assertEquals(javaPeer, objectJS);//BUG in MBeanProxyFactory$JMXInvocationHandler

		}
		
		
	}
	
}
