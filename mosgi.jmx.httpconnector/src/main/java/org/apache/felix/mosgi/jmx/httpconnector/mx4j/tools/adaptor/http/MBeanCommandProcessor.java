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
import java.util.Iterator;
import java.util.Set;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ObjectName;
//import javax.management.modelmbean.ModelMBeanInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * MBeanCommandProcessor, processes a request for getting data about a MBean
 *
 * @author <a href="mailto:tibu@users.sourceforge.net">Carlos Quiroz</a>
 * @author <a href="mailto:l.dobrev@lsnsoft.com">Lachezar Dobrev</a>
 * @version $Revision: 1.1.1.1 $
 */
public class MBeanCommandProcessor extends HttpCommandProcessorAdaptor
{
	public MBeanCommandProcessor()
	{
	}

	public Document executeRequest(HttpInputStream in) throws IOException, JMException
	{
		Document document = builder.newDocument();

		String name = in.getVariable("objectname");
		ObjectName objectName = null;

		if (name != null)
		{
			objectName = new ObjectName(name);
			if (!objectName.isPattern())
			{
				// not a pattern - assume a single MBean
				if (server.isRegistered(objectName))
				{
					Element mb = createMBeanElement(document, objectName, in);
					document.appendChild(mb);
				}
			}
			else
			{
				// A pattern - return all matching MBeans
				Set names = new TreeSet(CommandProcessorUtil.createObjectNameComparator());
				names.addAll(server.queryNames(objectName, null));
				Element root = document.createElement("Server");
				root.setAttribute("pattern", objectName.toString());
				for (Iterator it = names.iterator(); it.hasNext();)
				{
					Element mb = createMBeanElement(document, (ObjectName)it.next(), in);
					root.appendChild(mb);
				}
				document.appendChild(root);
			}
		}
		return document;
	}

	private Element createMBeanElement(Document document, ObjectName objectName, HttpInputStream in)
		throws JMException
	{
		Element root = document.createElement("MBean");

		MBeanInfo info = server.getMBeanInfo(objectName);
		root.setAttribute("description", info.getDescription());
		root.setAttribute("classname", info.getClassName());
		root.setAttribute("objectname", objectName.toString());
/*
		if (info instanceof ModelMBeanInfo)
		{
			root.setAttribute("model", "true");
		}
*/
		if (HttpUtil.booleanVariableValue(in, "attributes", true)) {
			MBeanAttributeInfo[] attributes = info.getAttributes();
			if (attributes != null)
			{
				SortedMap sortedAttributes = new TreeMap();
				for (int i=0;i<attributes.length;i++)
				{
					Element attribute = document.createElement("Attribute");
					attribute.setAttribute("name", attributes[i].getName());
					attribute.setAttribute("type", attributes[i].getType());
					attribute.setAttribute("description", attributes[i].getDescription());
					attribute.setAttribute("strinit", String.valueOf(CommandProcessorUtil.canCreateParameterValue(attributes[i].getType())));
					if (attributes[i].isReadable() && attributes[i].isWritable())
					{
						attribute.setAttribute("availability", "RW");
					}
					if (attributes[i].isReadable() && !attributes[i].isWritable())
					{
						attribute.setAttribute("availability", "RO");
					}
					if (!attributes[i].isReadable() && attributes[i].isWritable())
					{
						attribute.setAttribute("availability", "WO");
					}
					try
					{
						Object attributeValue = server.getAttribute(objectName, attributes[i].getName());
						attribute.setAttribute("isnull", (attributeValue==null)?"true":"false");
						if (attributeValue != null)
						{
							attribute.setAttribute("value", attributeValue.toString());
							if (attributeValue.getClass().isArray())
							{
								attribute.setAttribute("aggregation", "array");
							}
							if (attributeValue instanceof java.util.Collection)
							{
								attribute.setAttribute("aggregation", "collection");
							}
							if (attributeValue instanceof java.util.Map)
							{
								attribute.setAttribute("aggregation", "map");
							}
						}
						else
						{
							attribute.setAttribute("value", "null");
						}

					}
					catch (JMException e)
					{
						attribute.setAttribute("value", e.getMessage());
					}
					sortedAttributes.put(attributes[i].getName(), attribute);
				}
				Iterator keys = sortedAttributes.keySet().iterator();
				while (keys.hasNext())
				{
					root.appendChild((Element)sortedAttributes.get(keys.next()));
				}
			}
		}
		if (HttpUtil.booleanVariableValue(in, "constructors", true))
		{
			MBeanConstructorInfo[] constructors = info.getConstructors();
			if (constructors != null)
			{
				// How to order contructors?
				for (int i=0;i<constructors.length;i++)
				{
					Element constructor = document.createElement("Constructor");
					constructor.setAttribute("name", constructors[i].getName());
					constructor.setAttribute("description", constructors[i].getDescription());
					addParameters(constructor, document, constructors[i].getSignature());
					root.appendChild(constructor);
				}
			}
		}
		if (HttpUtil.booleanVariableValue(in, "operations", true))
		{
			MBeanOperationInfo[] operations = info.getOperations();
			if (operations != null)
			{
				for (int i=0;i<operations.length;i++)
				{
					Element operation = document.createElement("Operation");
					operation.setAttribute("name", operations[i].getName());
					operation.setAttribute("description", operations[i].getDescription());
					operation.setAttribute("return", operations[i].getReturnType());
					switch (operations[i].getImpact())
					{
						case MBeanOperationInfo.UNKNOWN:
							operation.setAttribute("impact", "unknown");
							break;
						case MBeanOperationInfo.ACTION:
							operation.setAttribute("impact", "action");
							break;
						case MBeanOperationInfo.INFO:
							operation.setAttribute("impact", "info");
							break;
						case MBeanOperationInfo.ACTION_INFO:
							operation.setAttribute("impact", "action_info");
							break;
					}
					addParameters(operation, document, operations[i].getSignature());
					root.appendChild(operation);
				}
			}
		}
		if (HttpUtil.booleanVariableValue(in, "notifications", true))
		{
			MBeanNotificationInfo[] notifications = info.getNotifications();
			if (notifications != null)
			{
				for (int i=0;i<notifications.length;i++)
				{
					Element notification = document.createElement("Notification");
					notification.setAttribute("name", notifications[i].getName());
					notification.setAttribute("description", notifications[i].getDescription());
					String[] types = notifications[i].getNotifTypes();
					for (int j=0;j<types.length;j++)
					{
						Element type =  document.createElement("Type");
						type.setAttribute("name", types[j]);
						notification.appendChild(type);
					}
					root.appendChild(notification);
				}
			}
		}
		return root;
	}

	protected void addParameters(Element node, Document document, MBeanParameterInfo[] parameters)
	{
		for (int j=0;j<parameters.length;j++)
		{
			Element parameter = document.createElement("Parameter");
			parameter.setAttribute("name", parameters[j].getName());
			parameter.setAttribute("description", parameters[j].getDescription());
			parameter.setAttribute("type", parameters[j].getType());
			parameter.setAttribute("strinit", String.valueOf(CommandProcessorUtil.canCreateParameterValue(parameters[j].getType())));
			// add id since order is relevant
			parameter.setAttribute("id", ""+j);
			node.appendChild(parameter);
		}
	}

}
