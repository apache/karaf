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

import java.util.Iterator;
import javax.management.ObjectName;

/**
 * The MBeanServer implementation delegates to implementations of this interface the storage of registered MBeans. <p>
 * All necessary synchronization code is taken care by the MBeanServer, so implementations can be coded without caring
 * of synchronization issues.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public interface MBeanRepository extends Cloneable
{
	/**
	 * Returns the metadata information associated with the given object name.
	 * @see #put
	 */
	public MBeanMetaData get(ObjectName name);

	/**
	 * Inserts the given metadata associated with the given object name into this repository.
	 * @see #get
	 */
	public void put(ObjectName name, MBeanMetaData metadata);

	/**
	 * Removes the metadata associated with the given object name from this repository.
	 */
	public void remove(ObjectName name);

	/**
	 * Returns the size of this repository.
	 */
	public int size();

	/**
	 * Returns an iterator on the metadata stored in this repository.
	 */
	public Iterator iterator();

	/**
	 * Clones this MBean repository
	 */
	public Object clone();
}
