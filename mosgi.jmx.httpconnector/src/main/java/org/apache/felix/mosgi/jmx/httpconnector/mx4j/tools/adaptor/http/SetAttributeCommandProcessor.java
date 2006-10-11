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

import javax.management.ObjectName;
import javax.management.JMException;
import javax.management.MalformedObjectNameException;
import javax.management.MBeanInfo;
import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import java.io.IOException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * SetAttributeCommandProcessor, processes a request for setting one attribute
 * in one MBean
 *
 * @author <a href="mailto:tibu@users.sourceforge.net">Carlos Quiroz</a>
 * @version $Revision: 1.1.1.1 $
 */
public class SetAttributeCommandProcessor extends HttpCommandProcessorAdaptor
{

  public SetAttributeCommandProcessor()
  {
  }

  public Document executeRequest(HttpInputStream in) throws IOException, JMException
  {
    Document document = builder.newDocument();

    Element root = document.createElement("MBeanOperation");
    document.appendChild(root);
    Element operationElement = document.createElement("Operation");
    operationElement.setAttribute("operation", "setattribute");
    root.appendChild(operationElement);

    String objectVariable = in.getVariable("objectname");
    String attributeVariable = in.getVariable("attribute");
    String valueVariable = in.getVariable("value");
    if (objectVariable == null || objectVariable.equals("") ||
      attributeVariable == null || attributeVariable.equals("") ||
      valueVariable == null)
    {
      operationElement.setAttribute("result", "error");
      operationElement.setAttribute("errorMsg", "Incorrect parameters in the request");
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
            operationElement.setAttribute("result", "error");
            operationElement.setAttribute("errorMsg", "Value: " + valueVariable + " could not be converted to " + type);
          }
          if (value != null)
          {
            try
            {
              server.setAttribute(name, new Attribute(attributeVariable, value));
              operationElement.setAttribute("result", "success");
            }
            catch (Exception e)
            {
              operationElement.setAttribute("result", "error");
              operationElement.setAttribute("errorMsg", e.getMessage());
            }
          }
        }
      }
      else
      {
        operationElement.setAttribute("result", "error");
        operationElement.setAttribute("errorMsg", "Attribute " + attributeVariable + " not found");
      }
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

}
