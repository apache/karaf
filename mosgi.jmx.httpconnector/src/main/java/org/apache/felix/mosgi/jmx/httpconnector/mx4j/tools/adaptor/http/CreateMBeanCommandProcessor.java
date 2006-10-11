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
package org.apache.felix.mosgi.jmx.httpconnector.mx4j.tools.adaptor.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.management.JMException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * CreateMBeanCommandProcessor, processes a request for creating and registering
 * an MBean
 *
 * @author    <a href="mailto:tibu@users.sourceforge.net">Carlos Quiroz</a>
 * @version   $Revision: 1.1.1.1 $
 */
public class CreateMBeanCommandProcessor extends HttpCommandProcessorAdaptor
{

	/**
	 * Constructs a new CreateMBeanCommandProcessor
	 *
	 */
	public CreateMBeanCommandProcessor()
	{
	}


	public Document executeRequest(HttpInputStream in)
		throws IOException, JMException
	{
		Document document = builder.newDocument();

		Element root = document.createElement("MBeanOperation");
		document.appendChild(root);
		Element operationElement = document.createElement("Operation");
		operationElement.setAttribute("name", "create");
		root.appendChild(operationElement);

		String objectVariable = in.getVariable("objectname");
		String classVariable = in.getVariable("class");
		if (objectVariable == null || objectVariable.equals("")
				 || classVariable == null || classVariable.equals(""))
		{
			operationElement.setAttribute("result", "error");
			operationElement.setAttribute("errorMsg", "Incorrect parameters in the request");
			return document;
		}
		operationElement.setAttribute("objectname", objectVariable);
		List types = new ArrayList();
		List values = new ArrayList();
		int i = 0;
		boolean unmatchedParameters = false;
		boolean valid = false;
		do
		{
			String parameterType = in.getVariable("type" + i);
			String parameterValue = in.getVariable("value" + i);
			valid = (parameterType != null && parameterValue != null);
			if (valid)
			{
				types.add(parameterType);
				Object value = null;
				try
				{
					value = CommandProcessorUtil.createParameterValue(parameterType, parameterValue);
				}
				catch (Exception e)
				{
					operationElement.setAttribute("result", "error");
					operationElement.setAttribute("errorMsg", "Parameter " + i + ": " + parameterValue + " cannot be converted to type " + parameterType);
					return document;
				}
				if (value != null)
				{
					values.add(value);
				}
			}
			if (parameterType == null ^ parameterValue == null)
			{
				unmatchedParameters = true;
				break;
			}
			i++;
		} while (valid);
		if (objectVariable == null || objectVariable.equals(""))
		{
			operationElement.setAttribute("result", "error");
			operationElement.setAttribute("errorMsg", "Incorrect parameters in the request");
			return document;
		}
		if (unmatchedParameters)
		{
			operationElement.setAttribute("result", "error");
			operationElement.setAttribute("errorMsg", "count of parameter types doesn't match count of parameter values");
			return document;
		}
		ObjectName name = null;
		try
		{
			name = new ObjectName(objectVariable);
		}
		catch (MalformedObjectNameException e)
		{
			operationElement.setAttribute("result", "error");
			operationElement.setAttribute("errorMsg", "Malformed object name");
			return document;
		}

		if (server.isRegistered(name))
		{
			operationElement.setAttribute("result", "error");
			operationElement.setAttribute("errorMsg", "A MBean with name " + name + " is already registered");
			return document;
		}
		else
		{
			try
			{
				if (types.size() > 0)
				{
					Object[] params = values.toArray();
					String[] signature = new String[types.size()];
					types.toArray(signature);
					server.createMBean(classVariable, name, null, params, signature);
				}
				else
				{
					server.createMBean(classVariable, name, null);
				}
				operationElement.setAttribute("result", "success");
			}
			catch (Exception e)
			{
				operationElement.setAttribute("result", "error");
				operationElement.setAttribute("errorMsg", e.getMessage());
			}
		}
		return document;
	}

}

