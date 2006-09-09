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
import java.util.Set;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import junit.framework.TestCase;

public class MBeanProxyFactoryTestCase extends IntrospectorTestHarness {

	MBeanProxyFactory factory;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		factory = new MBeanProxyFactory(mbs);
	}

	public void testNewProxyInstance() throws Exception{
		Set<ObjectName> onames = mbs.queryNames(ObjectName.WILDCARD, null);
		for (ObjectName name : onames) {
				Object mbean=factory.newProxyInstance(name.toString());
				assertTrue(mbean instanceof MBean);
				logger.info(((MBean)mbean).getObjectName());
		}
	}
}
