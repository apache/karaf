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
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import junit.framework.TestCase;

public class OutOfProcessTestHarness extends TestCase {
	Logger logger=Logger.getLogger(this.getClass().getName());
	MBeanServerConnection mbs;
	MBeanProxyManager proxyManager;
	ObjectName testName;
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		JMXServiceURL u=new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:1199/server");
		mbs=JMXConnectorFactory.connect(u).getMBeanServerConnection();
		proxyManager=new MBeanProxyManager();
		proxyManager.addRemoteServer(u.toString());
		
	}
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}
}
