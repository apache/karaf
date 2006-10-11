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
package org.apache.felix.mosgi.jmx.agent.mx4j;


import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Default implementation for the MBeanDescription interface.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public class MBeanDescriptionAdapter implements MBeanDescription
{
	public String getMBeanDescription()
	{
		return "Manageable Bean";
	}

	public String getConstructorDescription(Constructor ctor)
	{
		return "Constructor exposed for management";
	}

	public String getConstructorParameterName(Constructor ctor, int index)
	{
		return "param" + (index + 1);
	}

	public String getConstructorParameterDescription(Constructor ctor, int index)
	{
		return "Constructor's parameter n. " + (index + 1);
	}

	public String getAttributeDescription(String attribute)
	{
		return "Attribute exposed for management";
	}

	public String getOperationDescription(Method operation)
	{
		return "Operation exposed for management";
	}

	public String getOperationParameterName(Method method, int index)
	{
		return "param" + (index + 1);
	}

	public String getOperationParameterDescription(Method method, int index)
	{
		return "Operation's parameter n. " + (index + 1);
	}
}
