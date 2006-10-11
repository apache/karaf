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
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.management.Attribute;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * SetAttributesCommandProcessor, processes a request for setting one or more attributes
 * in one MBean. it uses th facility of havin multiple submit buttons in a web page
 * if the set_all=Set variable is passed all attributes will be set, if a set_XXX varialbe
 * is passed only the specific attribute will be set
 *
 * @author <a href="mailto:tibu@users.sourceforge.net">Carlos Quiroz</a>
 * @version $Revision: 1.1.1.1 $
 */
public class SetAttributesCommandProcessor extends HttpCommandProcessorAdaptor
{

  public SetAttributesCommandProcessor()
  {
  }

  public Document executeRequest(HttpInputStream in) throws IOException, JMException
  {
    Document document = builder.newDocument();

    Element root = document.createElement("MBeanOperation");
    document.appendChild(root);
    Element operationElement = document.createElement("Operation");
    operationElement.setAttribute("operation", "setattributes");
    root.appendChild(operationElement);

    String objectVariable = in.getVariable("objectname");
    if (objectVariable == null || objectVariable.equals(""))
    {
      operationElement.setAttribute("result", "error");
      operationElement.setAttribute("errorMsg", "Missing objectname in the request");
      return document;
    }
    operationElement.setAttribute("objectname", objectVariable);
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
      Map variables = in.getVariables();
      if (variables.containsKey("setall"))
      {
        Iterator keys = variables.keySet().iterator();
        SortedMap allAttributes = new TreeMap();
        while (keys.hasNext()) {
          String key = (String)keys.next();
          if (key.startsWith("value_"))
          {
            String attributeVariable = key.substring(6, key.length());
            String valueVariable = in.getVariable(key);
            Element attributeElement = setAttribute(document, attributeVariable, valueVariable, name);
            allAttributes.put(attributeVariable, attributeElement);
            operationElement.appendChild(attributeElement);
          }
        }
        keys = allAttributes.keySet().iterator();
        while (keys.hasNext()) {
          Element attributeElement = (Element)allAttributes.get(keys.next());
          operationElement.appendChild(attributeElement);
        }
      }
      else
      {
        Iterator keys = variables.keySet().iterator();
        SortedMap allAttributes = new TreeMap();
        while (keys.hasNext())
        {
          String key = (String)keys.next();
          if (key.startsWith("set_"))
          {
            String attributeVariable = key.substring(4, key.length());
            String valueVariable = in.getVariable("value_" + attributeVariable);
            Element attributeElement = setAttribute(document, attributeVariable, valueVariable, name);
            allAttributes.put(attributeVariable, attributeElement);
          }
        }
        keys = allAttributes.keySet().iterator();
        while (keys.hasNext()) {
          Element attributeElement = (Element)allAttributes.get(keys.next());
          operationElement.appendChild(attributeElement);
        }
      }
      //operationElement.setAttribute("result", "success");
    }
    else
    {
      if (name != null)
      {
        operationElement.setAttribute("result", "error");
        operationElement.setAttribute("errorMsg", "MBean " + name + " not registered");
      }
    }
    return document;
  }

  private Element setAttribute(Document document, String attributeVariable, String valueVariable, ObjectName name) throws JMException
  {
    Element attributeElement = document.createElement("Attribute");
    attributeElement.setAttribute("attribute", attributeVariable);
    MBeanInfo info = server.getMBeanInfo(name);
    MBeanAttributeInfo[] attributes = info.getAttributes();
    MBeanAttributeInfo targetAttribute = null;
    if (attributes != null)
    {
      for (int i=0;i<attributes.length;i++)
      {
        if (attributes[i].getName().equals(attributeVariable))
        {
          targetAttribute = attributes[i];
          break;
        }
      }
    }
    if (targetAttribute != null)
    {
      String type = targetAttribute.getType();
      Object value = null;
      if (valueVariable != null)
      {
        try
        {
          value = CommandProcessorUtil.createParameterValue(type, valueVariable);
        }
        catch (Exception e)
        {
          attributeElement.setAttribute("result", "error");
          attributeElement.setAttribute("errorMsg", "Value: " + valueVariable + " could not be converted to " + type);
        }
        if (value != null)
        {
          try
          {
            server.setAttribute(name, new Attribute(attributeVariable, value));
            attributeElement.setAttribute("result", "success");
            attributeElement.setAttribute("value", valueVariable);
          }
          catch (Exception e)
          {
            attributeElement.setAttribute("result", "error");
            attributeElement.setAttribute("errorMsg", e.getMessage());
          }
        }
      }
    }
    else
    {
      attributeElement.setAttribute("result", "error");
      attributeElement.setAttribute("errorMsg", "Attribute " + attributeVariable + " not found");
    }
    return attributeElement;
  }

}
