/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.felix.mosgi.jmx.agent.mx4j.server;


import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;
import javax.management.MBeanServerDelegate;

/**
 * <p>This class is responsible for creating new instances of {@link MBeanServerDelegate}
 * and {@link MBeanServer}. It creates instances from the implementation in the
 * <code>mx4j.server</code> package.</p>
 *
 * <p>The {@link javax.management.MBeanServerFactory} first creates the delegate, then it
 * creates the MBeanServer and provides a reference to the created delegate to it.
 * Note that the delegate passed to the MBeanServer might not be the instance returned
 * by this builder; for example, it could be a wrapper around it.</p>
 *
 * @see MBeanServer
 * @see javax.management.MBeanServerFactory
 *
 * @author <a href="mailto:oreinert@users.sourceforge.net">Olav Reinert</a>
 * @version $Revision: 1.1.1.1 $
 **/

public class MX4JMBeanServerBuilder extends MBeanServerBuilder
{
	/**
	 * Returns a new {@link MX4JMBeanServerDelegate} instance for a new MBeanServer.
	 * @return a new {@link MX4JMBeanServerDelegate} instance for a new MBeanServer.
	 **/
	public MBeanServerDelegate newMBeanServerDelegate()
	{
		return new MX4JMBeanServerDelegate();
	}

	/**
	 * Returns a new {@link MX4JMBeanServer} instance.
	 * @param defaultDomain the default domain name for the new server.
	 * @param outer the {@link MBeanServer} that is passed in calls to
	 * 	{@link javax.management.MBeanRegistration#preRegister(javax.management.MBeanServer, javax.management.ObjectName)}.
	 * @param delegate the {@link MBeanServerDelegate} instance for the new server.
	 * @return a new {@link MX4JMBeanServer} instance.
	 **/
	public MBeanServer newMBeanServer(String defaultDomain, MBeanServer outer, MBeanServerDelegate delegate)
	{
		return new MX4JMBeanServer(defaultDomain, outer, delegate);
	}
}
