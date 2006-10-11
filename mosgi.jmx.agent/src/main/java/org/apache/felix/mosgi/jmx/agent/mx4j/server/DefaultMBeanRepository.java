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

import java.util.HashMap;
import java.util.Iterator;

import javax.management.ObjectName;

/**
 * Default implementation of the MBeanRepository interface.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
class DefaultMBeanRepository implements MBeanRepository
{
	private HashMap m_map = new HashMap();

	public MBeanMetaData get(ObjectName name)
	{
		return (MBeanMetaData)m_map.get(name);
	}

	public void put(ObjectName name, MBeanMetaData metadata)
	{
		m_map.put(name, metadata);
	}

	public void remove(ObjectName name)
	{
		m_map.remove(name);
	}

	public int size()
	{
		return m_map.size();
	}

	public Iterator iterator()
	{
		return m_map.values().iterator();
	}

	public Object clone()
	{
		try
		{
			DefaultMBeanRepository repository = (DefaultMBeanRepository)super.clone();
			repository.m_map = (HashMap)m_map.clone();
			return repository;
		}
		catch (CloneNotSupportedException ignored)
		{
			return null;
		}
	}
}
